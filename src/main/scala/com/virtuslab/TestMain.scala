package com.virtuslab

import akka.actor.Deploy
import com.virtuslab.dsl.{ Configuration, HttpApplication, System, SystemInterpreter }
import play.api.libs.json.Format
import skuber.apps.v1.Deployment
import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object TestMain extends DSLMain with App {

  def deploy(): Unit = {
    import skuber._
    import skuber.json.format._

    val namespace = Namespace.forName("test")

    // Ensure a namespace
    val createNamespace = createOrUpdate(client, namespace)
    val ns = Await.result(createNamespace, 1.minute)
    println(s"Successfully created '$ns' on Kubernetes cluster")

    val nsClient = client.usingNamespace(namespace.name)

    val configuration = Configuration(
      "app",
      Map(
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

    // Populate the namespace
    val app = new HttpApplication("app", "quay.io/virtuslab/cloud-file-server:v0.0.6") {
      override val command: List[String] = List("cloud-file-server")
      override val args: List[String] = List("--config", "/opt/config.yaml")
    }.listensOn(8080)

    val system = System("test")
      .addApplication(app)
      .addConfiguration(configuration)

    val systemInterpreter = SystemInterpreter.of(system)

    systemInterpreter(system).foreach {
      case config: ConfigMap =>
        val createConfig = createOrUpdate(nsClient, config)
        val cfg = Await.result(createConfig, 1.minute)
        println(s"Successfully created '$cfg' on Kubernetes cluster")

      case service: Service =>
        val createService = createOrUpdate(nsClient, service)
        val svc = Await.result(createService, 1.minute)
        println(s"Successfully created '$svc' on Kubernetes cluster")

      case deployment: Deployment =>
        val createDeployment = createOrUpdate(nsClient, deployment)
        val dpl = Await.result(createDeployment, 1.minute)
        println(s"Successfully created '$dpl' on Kubernetes cluster")
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
  super.close()
}
