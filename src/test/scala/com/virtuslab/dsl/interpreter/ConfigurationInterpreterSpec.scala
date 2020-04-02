package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.{ Configuration, Labels, Name, UntypedLabel }
import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json
import skuber.json.format._

class ConfigurationInterpreterSpec extends InterpreterSpec {

  ignore should "ignore second label name when it's user defined" in {
    implicit val (ds, ns) = builders()

    val configuration = Configuration(
      Labels(Name("foo"), UntypedLabel("name", "bazz")),
      data = Map.empty
    )

    val skuberConfig = ConfigurationInterpreter(configuration)

    Json.toJson(skuberConfig).should(matchJsonString(yamlToJson(s"""
      |---
      |kind: ConfigMap
      |apiVersion: v1
      |metadata:
      |  name: foo
      |  namespace: "${ns.name}"
      |  labels:
      |    name: foo
      |""".stripMargin)))
  }
}
