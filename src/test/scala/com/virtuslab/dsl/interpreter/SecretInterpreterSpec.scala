package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.{ Labels, Name, Secret }
import play.api.libs.json.Json
import skuber.json.format._

class SecretInterpreterSpec extends InterpreterSpec {

  it should "create empty secret" in {
    implicit val (ds, ns) = builders()

    val secret = Secret(Labels(Name("test")), data = Map.empty)

    Json.toJson(SecretInterpreter(secret)) should matchJsonString(s"""
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
    implicit val (ds, ns) = builders()

    val secret = Secret(Labels(Name("test")), data = Map("foo" -> "bar"))

    Json.toJson(SecretInterpreter(secret)) should matchJsonString(s"""
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
