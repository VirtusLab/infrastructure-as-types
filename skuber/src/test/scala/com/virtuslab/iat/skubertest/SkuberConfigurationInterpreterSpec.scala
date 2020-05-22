package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ Name, UntypedLabel }
import com.virtuslab.iat.kubernetes.dsl.{ Configuration, Namespace }
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberConfigurationInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  ignore should "ignore second label name when it's user defined" in {
    val ns = Namespace(Name("foo") :: Nil)
    val conf = Configuration(
      Name("foo") :: UntypedLabel("name", "bazz") :: Nil,
      data = Map.empty
    )

    import iat.skuber.playjson._
    import skuber.json.format._

    val config = conf.interpretWith(ns).asJsValues.head

    config.should(matchJson(yamlToJson(s"""
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
