package com.virtuslab.iat.kubernetes.openapitest

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Namespace
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.openapi
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NamespaceInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "serialize Namespace to JSON" in {
    import openapi._
    import openapi.json4s._

    val ns = Namespace(Name("foo") :: Nil)

    val namespace =
      namespaceInterpreter.interpret(ns).map(_.transform).head

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
