package com.virtuslab

import com.virtuslab.dsl.{ HttpApplication, System, SystemInterpreter }
import play.api.libs.json.Format
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

    val appConfig = ConfigMap(
      metadata = ObjectMeta(name = "app"),
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
    val createConfig = createOrUpdate(nsClient, appConfig)
    val config = Await.result(createConfig, 1.minute)
    println(s"Successfully created '$config' on Kubernetes cluster")

    // Populate the namespace
    val app = HttpApplication("app", "quay.io/virtuslab/cloud-file-server:v0.0.6")
      .listensOn(8080)

    val system = System("test")
      .addApplication(app)

    val systemInterpreter = SystemInterpreter.of(system)

    systemInterpreter(system) foreach {
      case (service, deployment) => {
        val createService = createOrUpdate(nsClient, service)
        val createDeployment = createOrUpdate(nsClient, deployment)

        val svc = Await.result(createService, 1.minute)
        println(s"Successfully created '$svc' on Kubernetes cluster")
        val dpl = Await.result(createDeployment, 1.minute)
        println(s"Successfully created '$dpl' on Kubernetes cluster")
      }
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
