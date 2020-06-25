package com.virtuslab.iat.examples

import java.nio.file.Path

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ Name, Role }
import com.virtuslab.iat.dsl.TCP
import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Container, Namespace }

object BlueGreen extends SkuberApp with scala.App {
  import iat.kubernetes.dsl.ops._
  import iat.skuber.dsl._

  val ns = Namespace(Name("foo") :: Nil)

  val conf = Configuration(
    Name("app") :: Nil,
    data = Map(
      "config.yaml" -> """
              |listen: :8080
              |logRequests: true
              |connectors:
              |- type: file
              |  uri: file:///opt/test.txt
              |  pathPrefix: /health
              |""".stripMargin,
      "test.txt" -> """
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
      ports = TCP(8080) :: Nil
    ) :: Nil,
    configurations = conf :: Nil,
    mounts = conf.mount("config", path = Path.of("/opt")) :: Nil
  )

  import iat.skuber.details._
  val appDetails = resourceRequirements(
    filter = _.name == "app",
    skuber.Resource.Requirements(
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

  import iat.kubernetes.dsl.experimental._
  import iat.skuber.experimental._
  import iat.skuber.interpreter._

  val v1Conf =
    conf
      .inNamespace(ns)
      .interpretedImplicitly

  // (Application, Namespace)
  // (Application, Namespace) => (Service, Deployment)
  // (Service, Deployment) => (Service, Deployment)
  val v1App =
    app
      .inNamespace(ns)
      .interpretedImplicitly
      .withDetails(
        serviceUnchanged.merge(appDetails)
      )

  val v2App =
    app
      .patch(
        a =>
          a.copy(
            labels = Role("bar") :: a.labels
          )
      )
      .inNamespace(ns)
      .interpretedWith(applicationInterpreter)
      .withDetails(
        serviceUnchanged.merge(appDetails)
      )

  val v1Ns = ns.interpretedImplicitly
//  val v1Ns = ns.interpretedWith(namespaceInterpreter)

  import skuber.json.format._
  upsertNamespace.execute(v1Ns).toTry.get
  upsertConfiguration.execute(v1Conf).toTry.get
  upsertApplication.execute(v1App).toTry.get
  println("...v1 done...")

  //    val v1ToV2 = v1App.evolutionTo(v2App).withStrategy(blueGreenApplication) FIXME
  val v1ToV2 = v1App.evolutionTo(v2App)
  blueGreenApplication.execute(v1ToV2).toTry.get

  println("...ending.")
  // Cleanup
  close()
}
