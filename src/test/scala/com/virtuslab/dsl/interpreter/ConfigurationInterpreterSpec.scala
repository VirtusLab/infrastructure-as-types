package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.{ Configuration, Labels, Name, UntypedLabel }
import play.api.libs.json.Json
import skuber.json.format._

class ConfigurationInterpreterSpec extends InterpreterSpec {

  ignore should "ignore second label name when it's user defined" in new Builders {
    val configuration = Configuration(Labels(Name("foo"), UntypedLabel("name", "bazz")), data = Map.empty)

    val skuberConfig = ConfigurationInterpreter(configuration)

    Json.toJson(skuberConfig) should matchJsonString(s"""
        |{
        |  "kind" : "ConfigMap",
        |  "apiVersion" : "v1",
        |  "metadata" : {
        |    "name" : "foo",
        |    "namespace" : "$namespaceName",
        |    "labels" : {
        |      "name" : "foo"
        |    }
        |  }
        |}
        |""".stripMargin)
  }
}
