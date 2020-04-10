package com.virtuslab

import java.nio.file.Path

import com.virtuslab.deployer.skuber.SimpleDeployer
import com.virtuslab.dsl.{ Application, Configuration, DistributedSystem, Namespace, _ }
import com.virtuslab.interpreter.SystemInterpreter

object OperatorMain extends AbstractMain with App {

  def deploy(): Unit = {
    import com.virtuslab.interpreter.skuber.Skuber._

    val system = DistributedSystem("test").inSystem { implicit ds: SystemBuilder[SkuberContext] => // FIXME
      import ds._
      namespaces(
        Namespace("test").inNamespace { implicit ns: NamespaceBuilder[SkuberContext] => // FIXME
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

    SimpleDeployer.createOrUpdate(client, SystemInterpreter.of(system))
  }

  // Run
  deploy()

  // Cleanup
  client.close
  //  super.close() // FIXME
}
