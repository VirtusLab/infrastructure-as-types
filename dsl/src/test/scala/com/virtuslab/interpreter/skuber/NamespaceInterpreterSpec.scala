package com.virtuslab.interpreter.skuber

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.Definition
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters.yamlToJson

class NamespaceInterpreterSpec extends InterpreterSpec with JsonMatchers {

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val namespace = Skuber.namespaceInterpreter(Definition(ns.namespace)).head.asJsValue

    namespace.should(matchJsonString(yamlToJson(s"""
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
