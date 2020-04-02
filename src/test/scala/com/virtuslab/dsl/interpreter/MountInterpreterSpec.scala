package com.virtuslab.dsl.interpreter

import java.nio.file.Path

import com.virtuslab.dsl.{ Configuration, Labels, Name, Secret }
import com.virtuslab.dsl.Mountable._
import play.api.libs.json.Json
import skuber.json.format._

class MountInterpreterSpec extends InterpreterSpec {

  it should "generate volume mount based on config map entry" in {
    implicit val (ds, ns) = builders()

    val config = Configuration(
      labels = Labels(Name("test-configmap")),
      data = Map("test.txt" -> """
        |I'm testy tester, being tested ;-)
        |""".stripMargin)
    )
    val mount = config.mount("test-mount", "test.txt", Path.of("/opt/foo.txt"))
    val (volume, volumeMount) = MountInterpreter(mount)

    Json.toJson(volume).should(matchJsonString("""
      |{
      |  "name" : "test-mount",
      |  "configMap" : {
      |    "name" : "test-configmap"
      |  }
      |}
      |""".stripMargin))

    Json.toJson(volumeMount).should(matchJsonString("""
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
    val (volume, volumeMount) = MountInterpreter(mount)

    Json.toJson(volume) should matchJsonString("""
        |{
        |  "name" : "test-secret-mount",
        |  "secret" : {
        |    "secretName" : "top-secret"
        |  }
        |}
        |""".stripMargin)
    Json.toJson(volumeMount) should matchJsonString("""
        |{
        |  "name" : "test-secret-mount",
        |  "mountPath" : "/opt/test-secret.txt",
        |  "subPath" : "test.txt"
        |}
        |""".stripMargin)
  }

  //TODO: PV/PVC
  ignore should "generate volume mount based on external source" in {
    ???
  }
}
