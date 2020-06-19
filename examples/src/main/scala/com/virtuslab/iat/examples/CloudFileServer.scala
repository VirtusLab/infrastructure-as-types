package com.virtuslab.iat.examples

import java.nio.file.Paths

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.TCP
import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Container, Namespace }
import skuber.Resource

object CloudFileServer extends SkuberApp with App {
  import iat.kubernetes.dsl.ops._
  import iat.skuber.dsl._

  val ns = Namespace(Name("test") :: Nil)
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
    mounts = conf.mount("config", Paths.get("/opt/")) :: Nil
  )

  import iat.skuber.details._
  val appDetails = resourceRequirements(
    filter = _.name == "app",
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

  import iat.skuber.deployment._
  import skuber.json.format._

  val results =
    ns.interpret.upsert.deinterpret.summary ::
      conf
        .interpretWith(ns)
        .upsert
        .deinterpret
        .summary ::
      app
        .interpretWith(ns)
        .map(appDetails)
        .upsert
        .deinterpret
        .summary :: Nil

  results.foreach(s => println(s.asString))

  // Cleanup
  close()
}
