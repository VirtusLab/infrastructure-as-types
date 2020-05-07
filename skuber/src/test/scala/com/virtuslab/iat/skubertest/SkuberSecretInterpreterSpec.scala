package com.virtuslab.iat.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.kubernetes.dsl.{ Namespace, Secret }
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberSecretInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "create empty secret" in {
    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map.empty)

    import iat.skuber.playjson._
    import skuber.json.format._

    val secret = sec.interpret(ns).asJsValues.head

    secret.should(matchJsonString(yamlToJson(s"""
        |---
        |kind: Secret
        |apiVersion: v1
        |metadata:
        |  name: ${sec.name}
        |  namespace: ${ns.name}
        |  labels:
        |    name: ${sec.name}
        |""".stripMargin)))
  }

  it should "create secret with key and value" in {
    val ns = Namespace(Name("foo") :: Nil)
    val sec = Secret(Name("test") :: Nil, data = Map("foo" -> "admin"))

    import iat.skuber.playjson._
    import skuber.json.format._

    val secret = sec.interpret(ns).asJsValues.head

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
       |  foo: YWRtaW4=
       |""".stripMargin)))
  }
}
