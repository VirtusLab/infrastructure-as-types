package com.virtuslab.iat.kubernetes

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.{ Namespace, Secret }
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SecretInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "create empty secret" in {
    import openApi._
    import openApi.json4s._

    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map.empty)

    val secret = secretInterpreter.interpret(sec, ns).map(_.transform).map(JsonMethods.pretty).head
    secret.should(matchJsonString(yamlToJson(s"""
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
    import openApi._
    import openApi.json4s._

    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map("foo" -> "admin"))

    val secret = secretInterpreter.interpret(sec, ns).map(_.transform).map(JsonMethods.pretty).head
    secret.should(matchJsonString(yamlToJson(s"""
       |---
       |kind: Secret
       |apiVersion: v1
       |metadata:
       |  name: ${sec.name}
       |  namespace: ${ns.name}
       |  labels:
       |    name: ${sec.name}
       |data:
       |  foo: [89,87,82,116,97,87,52,61]
       |  # foo: YWRtaW4=
       |""".stripMargin))) // FIXME is this encoding right?
  }
}
