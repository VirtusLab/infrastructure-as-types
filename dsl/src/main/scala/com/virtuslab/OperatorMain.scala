package com.virtuslab

import java.nio.file.Path

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Container, Namespace }
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.kubernetes.skuber
import _root_.skuber.apps.v1.Deployment
import _root_.skuber.Service
import _root_.skuber.{ Container => SContainer }

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

    def appDetails(s: Service, dpl: Deployment): (Service, Deployment) = {
      // format: off
      (s, dpl.copy(spec = dpl.spec.map(dspec =>
        dspec.copy(
          template = dspec.template.copy(
            spec = dspec.template.spec.map(pspec =>
              pspec.copy(
                containers = pspec.containers.map {
                  case c: SContainer if c.name == "app" => c.copy(
                    resources = None
                  )
                }
              )
            )
          )
      ))))
    }

    import kubernetes.skuber._
    import kubernetes.skuber.deployment._
    import kubernetes.skuber.deployment.Upsert
    import _root_.skuber.json.format._

    implicit def deploy[P <: Base]: Processor[P] = Upsert.deployer

    val n: Either[skuber.deployment.Error, Namespace] = ns.process()
    val a: Either[skuber.deployment.Error, Application] = app.process(ns, t => appDetails(t._1, t._2))

    println(n)
    println(a)
  }

  // Run
  deploy()

  // Cleanup
  client.close
}
