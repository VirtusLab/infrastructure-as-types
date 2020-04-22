package com.virtuslab.interpreter.skuber

import java.nio.file.Path

import com.virtuslab.dsl.Mountable._
import com.virtuslab.dsl.{ Configuration, Labels, Name, Secret }
import com.virtuslab.iat.json.converters.playJsonToString
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers

class MountInterpreterSpec extends InterpreterSpec[SkuberContext] with JsonMatchers {

  import com.virtuslab.interpreter.skuber.Skuber._
  import skuber.json.format._

  it should "generate volume mount based on config map entry" in {
    implicit val (ds, ns) = builders()

    val config = Configuration(
      labels = Labels(Name("test-configmap")),
      data = Map("test.txt" -> """
        |I'm testy tester, being tested ;-)
        |""".stripMargin)
    )
    val mount = config.mount("test-mount", "test.txt", Path.of("/opt/foo.txt"))
    val (volume, volumeMount) = Skuber.mount(mount)

    playJsonToString(volume).should(matchJsonString("""
        |{
        |  "name" : "test-mount",
        |  "configMap" : {
        |    "name" : "test-configmap"
        |  }
        |}
        |""".stripMargin))

    playJsonToString(volumeMount).should(matchJsonString("""
        |{
        |  "name" : "test-mount",
        |  "mountPath" : "/opt/foo.txt",
        |  "subPath" : "test.txt"
        |}
        |""".stripMargin))
  }

  it should "generate volume mount based on secret entry" in {
    implicit val (ds, ns) = builders()

    val secret = Secret(
      labels = Labels(Name("top-secret")),
      data = Map("test.txt" -> """
        |I'm testy tester, being tested ;-)
        |""".stripMargin)
    )
    val mount = secret.mount(name = "test-secret-mount", key = "test.txt", as = Path.of("/opt/test-secret.txt"))
    val (volume, volumeMount) = Skuber.mount(mount)

    playJsonToString(volume).should(matchJsonString("""
        |{
        |  "name" : "test-secret-mount",
        |  "secret" : {
        |    "secretName" : "top-secret"
        |  }
        |}
        |""".stripMargin))
    playJsonToString(volumeMount).should(matchJsonString("""
        |{
        |  "name" : "test-secret-mount",
        |  "mountPath" : "/opt/test-secret.txt",
        |  "subPath" : "test.txt"
        |}
        |""".stripMargin))
  }

  //TODO: PV/PVC
  ignore should "generate volume mount based on external source" in {
    ???
  }
}
