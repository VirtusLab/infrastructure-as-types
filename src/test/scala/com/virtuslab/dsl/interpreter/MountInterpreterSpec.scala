package com.virtuslab.dsl.interpreter

import java.nio.file.Path

import com.virtuslab.dsl.{ Configuration, Labels, Name }
import play.api.libs.json.Json
import skuber.json.format._

class MountInterpreterSpec extends InterpreterSpec {

  it should "generate volume mount based on config map entry" in new Builders {
    val config = Configuration(labels = Labels(Name("test-configmap")),
                               data = Map(
                                 "test.txt" ->
                                   """
        |I'm testy tester, being tested ;-)
        |""".stripMargin
                               ))
    import com.virtuslab.dsl.Mountable._
    val mount = config.mount("test-mount", "test.txt", Path.of("/opt/foo.txt"))

    val (volume, volumeMount) = MountInterpreter(mount)

    Json.toJson(volume) should matchJsonString("""
        |{
        |  "name" : "test-mount",
        |  "configMap" : {
        |    "name" : "test-configmap"
        |  }
        |}
        |""".stripMargin)

    Json.toJson(volumeMount) should matchJsonString("""
        |{
        |  "name" : "test-mount",
        |  "mountPath" : "/opt/foo.txt",
        |  "subPath" : "test.txt"
        |}
        |""".stripMargin)
  }

  ignore should "generate volume mount based on secret entry" in new Builders {
    ???
  }

  //TODO: PV/PVC
  ignore should "generate volume mount based on external source" in new Builders {
    ???
  }
}
