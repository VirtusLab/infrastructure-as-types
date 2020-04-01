package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Namespace
import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json

class NamespaceInterpreterSpec extends InterpreterSpec {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    implicit val (sb, nb) = builders()

    val namespace = Namespace.ref("test").inNamespace(identity)
    val resource = NamespaceInterpreter(namespace)

    val json = Json.toJson(resource)
    json should matchJsonString(yamlToJson(s"""
        |---
        |kind: Namespace
        |apiVersion: v1
        |metadata:
        |  name: test
        |  labels:
        |    name: test
        """.stripMargin))
  }
}
