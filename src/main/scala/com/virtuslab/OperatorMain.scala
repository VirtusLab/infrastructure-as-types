package com.virtuslab

import java.nio.file.Path

import com.virtuslab.dsl._
import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.internal.SkuberConverter.Resource
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object OperatorMain extends AbstractMain with App {
  import com.virtuslab.dsl.{ Application, Configuration, DistributedSystem, Namespace }

  def deploy(): Unit = {
    val system = DistributedSystem.ref("test").inSystem { implicit ds =>
      import ds._
      namespaces(
        Namespace.ref("test").inNamespace { implicit ns =>
          import ns._

          val configuration = Configuration(
            labels = Labels(Name("app")),
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

          import com.virtuslab.dsl.Mountable._
          applications(
            Application(
              labels = Labels(Name("app")),
              image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
              command = List("cloud-file-server"),
              args = List("--config", "/opt/config.yaml"),
              configurations = List(configuration),
              ports = Networked.Port(8080) :: Nil,
              mounts = configuration.mount("config", "config.yaml", Path.of("/opt/")) :: Nil
            )
          )
        }
      )
    }

    val resources: Seq[Resource[ObjectResource]] = SystemInterpreter.of(system).resources
    resources.map(createOrUpdate(client))
  }

  def createOrUpdate(client: K8SRequestContext): Resource[ObjectResource] => ObjectResource =
    (resource: Resource[ObjectResource]) => {
      val future = createOrUpdate(client, resource)
      val result = Await.result(future, 1.minute)
      println(s"Successfully created '$result' on Kubernetes cluster")
      result
    }

  def createOrUpdate(client: K8SRequestContext, resource: Resource[ObjectResource]): Future[ObjectResource] = {
    import skuber._

    client.create(resource.obj)(resource.format, resource.definition, LoggingContext.lc) recoverWith {
      case ex: K8SException if ex.status.code.contains(409) => client.update(resource.obj)(resource.format, resource.definition, LoggingContext.lc)
    }
  }

  // Run
  deploy()

  // Cleanup
  client.close
  //  super.close() // FIXME
}
