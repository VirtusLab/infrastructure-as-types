package com.virtuslab.interpreter.skuber

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl._
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters.yamlToJson

class ConfigurationInterpreterSpec extends InterpreterSpec with JsonMatchers {

  ignore should "ignore second label name when it's user defined" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val configuration = Configuration(
      Labels(Name("foo"), UntypedLabel("name", "bazz")),
      data = Map.empty
    )

    val config = Skuber.configurationInterpreter.apply(Definition(configuration)).head.asJsValue

    config.should(matchJsonString(yamlToJson(s"""
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
