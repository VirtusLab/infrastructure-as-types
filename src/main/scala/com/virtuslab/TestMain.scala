package com.virtuslab

import cats.data.NonEmptyList
import play.api.libs.json.Format
import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object TestMain extends DSLMain with App {
  import com.virtuslab.dsl.{ Configuration, HttpApplication, Namespace, System, SystemInterpreter }

  def deploy(): Unit = {
    val namespace = Namespace("test").inNamespace { implicit ns =>
      val configuration = Configuration(
        name = "app",
        data = Map(
          "config.yaml" ->
            """
              |listen: :8080
              |logRequests: true
              |connectors:
              |- type: file
              |  uri: file:///opt/test.txt
              |  pathPrefix: /health
              |""".stripMargin,
          "test.txt" ->
            """
              |I'm testy tester, being tested ;-)
              |""".stripMargin
        )
      )

      val app = HttpApplication(
        name = "app",
        image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
        command = List("cloud-file-server"),
        args = List("--config", "/opt/config.yaml"),
        configurations = List(configuration)
      ).listensOn(8080)

      NonEmptyList.of(configuration) :+ app
    }

    // Populate the namespace

    val system = System("test")
      .addNamespace(namespace)

    import skuber.{ ConfigMap, Namespace, Service }
    import skuber.apps.v1.Deployment
    import skuber.json.format._

    val systemInterpreter = SystemInterpreter.of(system)
    systemInterpreter(system).foreach {

      case namespace: Namespace =>
        val createNamespace = createOrUpdate(client, namespace)
        val ns = Await.result(createNamespace, 1.minute)
        println(s"Successfully created '$ns' on Kubernetes cluster")

      case config: ConfigMap =>
        val createConfig = createOrUpdate(client, config)
        val cfg = Await.result(createConfig, 1.minute)
        println(s"Successfully created '$cfg' on Kubernetes cluster")

      case service: Service =>
        val createService = createOrUpdate(client, service)
        val svc = Await.result(createService, 1.minute)
        println(s"Successfully created '$svc' on Kubernetes cluster")

      case deployment: Deployment =>
        val createDeployment = createOrUpdate(client, deployment)
        val dpl = Await.result(createDeployment, 1.minute)
        println(s"Successfully created '$dpl' on Kubernetes cluster")

      case o => throw new UnsupportedOperationException(s"unexpected resource $o")
    }
  }

  def createOrUpdate[O <: ObjectResource](client: K8SRequestContext, o: O)(implicit fmt: Format[O], rd: ResourceDefinition[O]): Future[O] = {
    import skuber._

    client.create(o) recoverWith {
      case ex: K8SException if ex.status.code.contains(409) => client.update(o) // this need an abstraction
    }
  }

  // Run
  deploy()

  // Cleanup
  client.close
  //  super.close() // FIXME
}
