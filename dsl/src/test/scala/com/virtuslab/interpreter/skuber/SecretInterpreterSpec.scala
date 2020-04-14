package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl.{ Definition, Labels, Name, Secret }
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers

class SecretInterpreterSpec extends InterpreterSpec[SkuberContext] with JsonMatchers {

  it should "create empty secret" in {
    implicit val (ds, ns) = builders()

    val secret = Secret(Labels(Name("test")), data = Map.empty)

    val resource = Skuber.secretInterpreter(Definition(secret)).head.asJValue
    resource.should(matchJson(s"""
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
        |""".stripMargin))
  }

  it should "create secret with key and value" in {
    implicit val (ds, ns) = builders()

    val secret = Secret(Labels(Name("test")), data = Map("foo" -> "bar"))

    val resource = Skuber.secretInterpreter(Definition(secret)).head.asJValue
    resource.should(matchJson(s"""
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
       |""".stripMargin))
  }

}
