package com.virtuslab.iat.kubernetes.openapitest

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.kubernetes.{ Namespace, Secret }
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.openapi
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SecretInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "create empty secret" in {
    import openapi._
    import openapi.json4s._

    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map.empty)

    val secret = interpret(sec, ns).map(_.result).head
    secret.should(matchJson(yamlToJson(s"""
        |---
        |kind: Secret
        |apiVersion: v1
        |metadata:
        |  name: ${sec.name}
        |  namespace: ${ns.name}
        |  labels:
        |    name: ${sec.name}
        |data: {}
        |""".stripMargin)))
  }

  it should "create secret with key and value" in {
    import openapi._
    import openapi.json4s._

    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map("foo" -> "admin"))

    val secret = interpret(sec, ns).map(_.result).head
    secret.should(matchJson(yamlToJson(s"""
       |---
       |kind: Secret
       |apiVersion: v1
       |metadata:
       |  name: ${sec.name}
       |  namespace: ${ns.name}
       |  labels:
       |    name: ${sec.name}
       |data:
       |  foo: YWRtaW4=
       |""".stripMargin)))
  }
}
