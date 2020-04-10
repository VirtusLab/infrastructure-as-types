package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl._
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json
import skuber.ConfigMap

class ConfigurationInterpreterSpec extends InterpreterSpec {
  import skuber.json.format._

  ignore should "ignore second label name when it's user defined" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val configuration = Configuration(
      Labels(Name("foo"), UntypedLabel("name", "bazz")),
      data = Map.empty
    )

    val skuberConfig = Skuber.configurationInterpreter.apply(Definition(configuration)).head.obj.asInstanceOf[ConfigMap] // FIXME

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
