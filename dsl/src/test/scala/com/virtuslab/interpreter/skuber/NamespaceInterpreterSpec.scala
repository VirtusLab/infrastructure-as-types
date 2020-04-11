package com.virtuslab.interpreter.skuber

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.Definition
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json
import skuber.Namespace

class NamespaceInterpreterSpec extends InterpreterSpec with JsonMatchers {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders[SkuberContext]()

    val namespace: Namespace = Skuber.namespaceInterpreter(Definition(ns.namespace)).head.obj.asInstanceOf[Namespace]

    Json.toJson(namespace) should matchJsonString(yamlToJson(s"""
        |---
        |kind: Namespace
        |apiVersion: v1
        |metadata:
        |  name: ${ns.name}
        |  labels:
        |    name: ${ns.name}
        """.stripMargin))
  }
}
