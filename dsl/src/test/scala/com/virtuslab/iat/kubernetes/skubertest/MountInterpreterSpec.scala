package com.virtuslab.iat.kubernetes.skubertest

import java.nio.file.Path

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.kubernetes.{ Configuration, Secret }
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.test.EnsureMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MountInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  import skuber.json.format._

  it should "generate volume mount based on config map entry" in {
    import kubernetes.skuber._
    import kubernetes.skuber.playjson._
    import dsl.kubernetes.Mountable._

    val config = Configuration(
      Name("test-configmap") :: Nil,
      data = Map("test.txt" -> """|I'm testy tester, being tested ;-)
                                  |""".stripMargin)
    )
    val mount = config.mount("test-mount", "test.txt", Path.of("/opt/foo.txt"))
    val (volume, volumeMount) = subinterpreter.mountInterpreter(mount)

    asJsonString(volume).should(matchJson("""
        |{
        |  "name" : "test-mount",
        |  "configMap" : {
        |    "name" : "test-configmap"
        |  }
        |}
        |""".stripMargin))

    asJsonString(volumeMount).should(matchJson("""
        |{
        |  "name" : "test-mount",
        |  "mountPath" : "/opt/foo.txt",
        |  "subPath" : "test.txt"
        |}
        |""".stripMargin))
  }

  it should "generate volume mount based on secret entry" in {
    import kubernetes.skuber._
    import kubernetes.skuber.playjson._
    import dsl.kubernetes.Mountable._

    val secret = Secret(
      Name("top-secret") :: Nil,
      data = Map("test.txt" -> """
        |I'm testy tester, being tested ;-)
        |""".stripMargin)
    )
    val mount = secret.mount(name = "test-secret-mount", key = "test.txt", as = Path.of("/opt/test-secret.txt"))
    val (volume, volumeMount) = subinterpreter.mountInterpreter(mount)

    asJsonString(volume).should(matchJson("""
        |{
        |  "name" : "test-secret-mount",
        |  "secret" : {
        |    "secretName" : "top-secret"
        |  }
        |}
        |""".stripMargin))
    asJsonString(volumeMount).should(matchJson("""
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
