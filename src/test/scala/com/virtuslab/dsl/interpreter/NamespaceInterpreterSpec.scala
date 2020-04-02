package com.virtuslab.dsl.interpreter

import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json

class NamespaceInterpreterSpec extends InterpreterSpec {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders()

    val resource = NamespaceInterpreter(ns.build())

    val json = Json.toJson(resource)
    json should matchJsonString(yamlToJson(s"""
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
