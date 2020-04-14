package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl.Definition
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers

class NamespaceInterpreterSpec extends InterpreterSpec[SkuberContext] with JsonMatchers {

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders()

    val namespace = Skuber.namespaceInterpreter(Definition(ds.system, ns.namespace)).head.asJValue

    namespace.should(matchJson(yamlToJson(s"""
        |---
        |kind: Namespace
        |apiVersion: v1
        |metadata:
        |  name: ${ns.name}
        |  labels:
        |    name: ${ns.name}
        |""".stripMargin)))
  }
}
