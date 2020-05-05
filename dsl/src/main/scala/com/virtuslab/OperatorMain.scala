package com.virtuslab

import java.nio.file.Path

import _root_.skuber.Resource
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Container, Namespace }
import com.virtuslab.iat.kubernetes

object OperatorMain extends AbstractMain with App {

  def deploy(): Unit = {
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
      containers = Container(
        Name("app") :: Nil,
        image = "quay.io/virtuslab/cloud-file-server:v0.0.6",
        command = List("cloud-file-server"),
        args = List("--config", "/opt/config.yaml"),
        ports = Port(8080) :: Nil
      ) :: Nil,
      configurations = conf :: Nil,
      mounts = conf.mount("config", "config.yaml", Path.of("/opt/")) :: Nil
    )

    import kubernetes.skuber.details._
    val appDetails = resourceRequirements(
      _.name == "app",
      Resource.Requirements(
        requests = Map(
          "cpu" -> "100m",
          "memory" -> "10Mi"
        ),
        limits = Map(
          "cpu" -> "200m",
          "memory" -> "200Mi"
        )
      )
    )

    import kubernetes.skuber.deployment._
    import skuber.json.format._

    val n: Either[Throwable, Namespace] = ns.interpret.upsert.deinterpret
    val a: Either[Throwable, Application] =
      app
        .interpret(ns)
        .map(appDetails)
        .map(_.upsert)
        .reduce(_.deinterpret)

    println(n)
    println(a)
  }

  // Run
  deploy()

  // Cleanup
  client.close
}
