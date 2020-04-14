package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl._
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.json.json4s.jackson.JsonMethods.pretty
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers

class ConfigurationInterpreterSpec extends InterpreterSpec[SkuberContext] with JsonMatchers {

  ignore should "ignore second label name when it's user defined" in {
    implicit val (ds, ns) = builders()

    val configuration = Configuration(
      Labels(Name("foo"), UntypedLabel("name", "bazz")),
      data = Map.empty
    )

    val config = Skuber.configurationInterpreter.apply(Definition(configuration)).head.asJValue
    pretty(config).should(matchJsonString(yamlToJson(s"""
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
