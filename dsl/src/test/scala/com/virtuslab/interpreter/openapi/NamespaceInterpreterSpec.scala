package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl.Definition
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.kubernetes.client.openapi.model.Namespace
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import com.virtuslab.json.Converters.yamlToJson
import org.json4s.jackson.Serialization
import org.json4s.{ Formats, NoTypeHints }

class NamespaceInterpreterSpec extends InterpreterSpec with JsonMatchers {

  it should "serialize Namespace to JSON" in {
    implicit val (ds, ns) = builders[OpenAPIContext]()

    val namespace: Namespace = OpenAPI.namespaceInterpreter(Definition(ns.namespace)).head.asInstanceOf[Namespace] // FIXME

    import org.json4s.jackson.Serialization._
    implicit val formats: Formats = Serialization.formats(NoTypeHints)

    write(namespace) should matchJson(yamlToJson(s"""
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
