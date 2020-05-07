package com.virtuslab.iat.openapitest

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.dsl.{Application, Configuration, Namespace, Secret}
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.openapi
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpreterDerivationTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "derive a one level nested interpreter" in {
    import openapi.interpreter._
    import openapi.json4s.InterpreterDerivation._
    import openapi.json4s._

    case class Group1(
        superApp: Application = Application(Name("bar") :: Nil),
        myConfiguration: Configuration = Configuration(Name("conf-bar") :: Nil, data = Map.empty),
        mySecret: Secret = Secret(Name("sec-bar") :: Nil, data = Map.empty))

    val g1 = Group1()
    val ns: Namespace = Namespace(Name("foo") :: Nil)

    val rs = interpret(ns) ++ interpret(g1, ns)

    Ensure(asMetaJsonString(rs.map(_.result)))
      .contain(
        Metadata("v1", "Namespace", "", ns.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Namespace
            |apiVersion: v1
            |metadata:
            |  name: ${ns.name}
            |  labels:
            |    name: ${ns.name}
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, g1.superApp.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Service
            |apiVersion: v1
            |metadata:
            |  name: ${g1.superApp.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${g1.superApp.name}
            |spec: {}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, g1.superApp.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Deployment
            |apiVersion: apps/v1
            |metadata:
            |  name: ${g1.superApp.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${g1.superApp.name}
            |spec:
            |  template:
            |    spec:
            |      containers:
            |        - name: bar
            |""".stripMargin)),
        Metadata("v1", "ConfigMap", ns.name, g1.myConfiguration.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: ConfigMap
            |apiVersion: v1
            |metadata:
            |  name: ${g1.myConfiguration.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${g1.myConfiguration.name}
            |data: {}
            |""".stripMargin)),
        Metadata("v1", "Secret", ns.name, g1.mySecret.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Secret
            |apiVersion: v1
            |metadata:
            |  name: ${g1.mySecret.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${g1.mySecret.name}
            |data: {}
            |""".stripMargin))
      )
  }
}
