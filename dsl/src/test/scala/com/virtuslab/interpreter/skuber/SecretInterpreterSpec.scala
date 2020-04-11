package com.virtuslab.interpreter.skuber

import _root_.skuber.{ Secret => SSecret }
import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.{ Definition, Labels, Name, Secret }
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import play.api.libs.json.Json

class SecretInterpreterSpec extends InterpreterSpec with JsonMatchers {
  import skuber.json.format._

  it should "create empty secret" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val secret = Secret(Labels(Name("test")), data = Map.empty)

    val resource: SSecret = Skuber.secretInterpreter(Definition(secret)).head.obj.asInstanceOf[SSecret] // FIXME
    Json.toJson(resource) should matchJsonString(s"""
        |{
        |  "kind" : "Secret",
        |  "apiVersion" : "v1",
        |  "metadata" : {
        |    "name" : "test",
        |    "namespace" : "${ns.name}",
        |    "labels" : {
        |      "name" : "test"
        |    }
        |  }
        |}
        |""".stripMargin)
  }

  it should "create secret with key and value" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val secret = Secret(Labels(Name("test")), data = Map("foo" -> "bar"))

    val resource: SSecret = Skuber.secretInterpreter(Definition(secret)).head.obj.asInstanceOf[SSecret] // FIXME
    Json.toJson(resource) should matchJsonString(s"""
       |{
       |  "kind" : "Secret",
       |  "apiVersion" : "v1",
       |  "metadata" : {
       |    "name" : "test",
       |    "namespace" : "${ns.name}",
       |    "labels" : {
       |      "name" : "test"
       |    }
       |  },
       |  "data" : {
       |    "foo" : "YmFy"
       |  }
       |}
       |""".stripMargin)
  }

}
