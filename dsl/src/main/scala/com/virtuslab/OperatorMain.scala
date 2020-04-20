package com.virtuslab

import java.nio.file.{Path, Paths}

import com.virtuslab.dsl.{Application, Configuration, DistributedSystem, Namespace, _}
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.materializer.skuber.SimpleDeployer

object OperatorMain extends AbstractMain with App {

  def deploy(): Unit = {

    val system = DistributedSystem("test").inSystem { implicit ds: SystemBuilder[SkuberContext] =>
      import ds._
      namespaces(
        Namespace("test").inNamespace { implicit ns: NamespaceBuilder[SkuberContext] =>
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
              ports = Port(8080) :: Nil,
              mounts = configuration.mount("config", "config.yaml", Paths.get("/opt/")) :: Nil
            )
          )
        }
      )
    }

    system.interpret().map(SimpleDeployer(client).createOrUpdate)
  }

  // Run
  deploy()

  // Cleanup
  client.close
}
