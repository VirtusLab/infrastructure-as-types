package com.virtuslab.iat.skubertest

import java.nio.file.Paths

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.kubernetes.dsl.Secret
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberMountInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "generate volume mount based on config map entry" in {
    import iat.kubernetes.dsl._
    import iat.kubernetes.dsl.ops._
    import iat.skuber.dsl._

    val config = Configuration(
      Name("test-configmap") :: Nil,
      data = Map(
        "test.txt" -> "I'm testy tester, being tested ;-)"
      )
    )
    val mount = config.mount(
      name = "test-mount",
      key = "test.txt",
      path = Paths.get("/opt/foo.txt")
    )

    import iat.skuber._
    val (volume, volumeMount) = subinterpreter.mountInterpreter(mount)

    import iat.skuber.playjson._
    import skuber.json.format._
    asJsValue(volume).should(matchJson("""
       |{
       |  "name" : "test-mount",
       |  "configMap" : {
       |    "name" : "test-configmap"
       |  }
       |}
       |""".stripMargin))

    asJsValue(volumeMount).should(matchJson("""
       |{
       |  "name" : "test-mount",
       |  "mountPath" : "/opt/foo.txt",
       |  "subPath" : "test.txt",
       |  "readOnly": true
       |}
       |""".stripMargin))
  }

  it should "mount multiple keys to a directory" in {
    import iat.kubernetes.dsl._
    import iat.kubernetes.dsl.ops._
    import iat.skuber.dsl._

    val config = Configuration(
      Name("config-two") :: Nil,
      data = Map(
        "test.txt" -> "I'm testy tester, being tested ;-)"
      )
    )
    val mount = config.mount(
      name = "config",
      path = Paths.get("/opt/")
    )

    import iat.skuber._
    val (volume, volumeMount) = subinterpreter.mountInterpreter(mount)

    import iat.skuber.playjson._
    import skuber.json.format._
    asJsValue(volume).should(matchJson("""
       |{
       |  "name" : "config",
       |  "configMap" : {
       |    "name" : "config-two"
       |  }
       |}
       |""".stripMargin))

    asJsValue(volumeMount).should(matchJson("""
      |{
      |  "name": "config",
      |  "mountPath": "/opt",
      |  "readOnly": true
      |}
      |""".stripMargin))
  }

  it should "generate volume mount based on secret entry" in {
    import iat.kubernetes.dsl.ops._
    import iat.skuber.dsl._

    val secret = Secret(
      Name("top-secret") :: Nil,
      data = Map("test.txt" -> """
        |I'm testy tester, being tested ;-)
        |""".stripMargin)
    )
    val mount = secret.mount(
      name = "test-secret-mount",
      key = "test.txt",
      path = Paths.get("/opt/test-secret.txt")
    )

    import iat.skuber._
    import iat.skuber.playjson._

    val (volume, volumeMount) = subinterpreter.mountInterpreter(mount)

    import skuber.json.format._
    asJsValue(volume).should(matchJson("""
        |{
        |  "name" : "test-secret-mount",
        |  "secret" : {
        |    "secretName" : "top-secret"
        |  }
        |}
        |""".stripMargin))
    asJsValue(volumeMount).should(matchJson("""
        |{
        |  "name" : "test-secret-mount",
        |  "mountPath" : "/opt/test-secret.txt",
        |  "subPath" : "test.txt",
        |  "readOnly": true
        |}
        |""".stripMargin))
  }

  //TODO: PV/PVC
  ignore should "generate volume mount based on external source" in {
    ???
  }
}
