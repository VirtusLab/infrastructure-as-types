package com.virtuslab

import java.nio.file.Path

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Container, Namespace }
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.kubernetes.skuber
import _root_.skuber.Resource

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
    val appDetails = resourceRequirements(_.name == "app",
                                          Resource.Requirements(
                                            requests = Map(
                                              "cpu" -> "100m",
                                              "memory" -> "10Mi"
                                            ),
                                            limits = Map(
                                              "cpu" -> "200m",
                                              "memory" -> "200Mi"
                                            )
                                          ))

    import kubernetes.skuber._
    import kubernetes.skuber.deployment._
    import _root_.skuber.json.format._

    implicit def deploy[P <: Base]: Processor[P] = Upsert.apply[P]

    val n: Either[skuber.deployment.Error, Namespace] = ns.process()
    val a: Either[skuber.deployment.Error, Application] = app.process(ns, appDetails)

    println(n)
    println(a)
  }

  // Run
  deploy()

  // Cleanup
  client.close
}
