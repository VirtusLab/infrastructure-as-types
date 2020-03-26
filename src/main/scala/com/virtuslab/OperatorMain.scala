package com.virtuslab

import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.dsl._
import com.virtuslab.internal.SkuberConverter.Resource
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object OperatorMain extends AbstractMain with App {
  import com.virtuslab.dsl.{ Application, Configuration, DistributedSystem, Namespace }

  def deploy(): Unit = {
    implicit val systemBuilder: SystemBuilder = DistributedSystem("test").builder

    val namespace = Namespace.ref("test")
    implicit val namespaceBuilder: NamespaceBuilder = namespace.builder

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

    Application(
      labels = Labels(Name("app")),
      image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
      command = List("cloud-file-server"),
      args = List("--config", "/opt/config.yaml"),
      configurations = List(configuration),
      ports = Networked.Port(8080) :: Nil
    )

    Application(
      labels = Labels(Name("app")),
      image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
      command = List("cloud-file-server"),
      args = List("--config", "/opt/config.yaml"),
      configurations = List(configuration),
      ports = Networked.Port(8080) :: Nil
    )

    val resources: Seq[Resource[ObjectResource]] = SystemInterpreter.of(systemBuilder).resources
    resources.foreach { resource: Resource[ObjectResource] =>
      val createNamespace = createOrUpdate(client, resource)
      val ns = Await.result(createNamespace, 1.minute)
      println(s"Successfully created '$ns' on Kubernetes cluster")
    }
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
