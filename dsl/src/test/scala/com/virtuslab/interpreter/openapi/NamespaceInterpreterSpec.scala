package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl.Definition
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers

class NamespaceInterpreterSpec extends InterpreterSpec with JsonMatchers {

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders[OpenAPIContext]()

    val namespace = OpenAPI.namespaceInterpreter(Definition(ds.system, ns.namespace)).head.asJValue

    namespace.should(matchJson(yamlToJson(s"""
        |---
        |kind: Namespace
        |apiVersion: v1
        |metadata:
        |  name: ${ns.name}
        |  labels:
        |    name: ${ns.name}
        """.stripMargin)))
  }
}
