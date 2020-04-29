package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.Label.{ Name, UntypedLabel }
import com.virtuslab.iat.dsl.kubernetes.{ Configuration, Namespace }
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.test.EnsureMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberConfigurationInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  ignore should "ignore second label name when it's user defined" in {
    val ns = Namespace(Name("foo") :: Nil)
    val conf = Configuration(
      Name("foo") :: UntypedLabel("name", "bazz") :: Nil,
      data = Map.empty
    )

    import kubernetes.skuber.playjson._
    import skuber.json.format._

    val config = conf.interpret(ns).head.result

    config.should(matchJsonString(yamlToJson(s"""
      |---
      |kind: ConfigMap
      |apiVersion: v1
      |metadata:
      |  name: foo
      |  namespace: "${ns.name}"
      |  labels:
      |    name: foo
      |""".stripMargin)))
  }
}
