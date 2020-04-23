package com.virtuslab

import java.nio.file.Path

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Namespace }
import com.virtuslab.iat.kubernetes

object OperatorMain extends AbstractMain with App {

  def deploy(): Unit = {
    import kubernetes.skuber._
    import kubernetes.skuber.deployment.InterpreterDerivation._
    import com.virtuslab.iat.dsl.kubernetes.Mountable._

    val ns = Namespace(Name("test") :: Nil)
    val conf = Configuration(
      Name("app") :: Nil,
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

    val app = Application(
      Name("app") :: Nil,
      image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
      command = List("cloud-file-server"),
      args = List("--config", "/opt/config.yaml"),
      configurations = List(conf),
      ports = Port(8080) :: Nil,
      mounts = conf.mount("config", "config.yaml", Path.of("/opt/")) :: Nil
    )

    val system = (app, conf)
//    val rs = interpret(ns) ++ interpret(system, ns) // FIXME
  }

  // Run
  deploy()

  // Cleanup
  client.close
}
