package com.virtuslab.iat.openapitest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Namespace, Secret }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.json4s.jackson.JsonMatchers
import org.json4s.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpreterTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "derive a one level nested interpreter" in {

    val myApp = Application(Name("bar") :: Nil)
    val myConfiguration = Configuration(Name("conf-bar") :: Nil, data = Map.empty)
    val mySecret = Secret(Name("sec-bar") :: Nil, data = Map.empty)

    val ns: Namespace = Namespace(Name("foo") :: Nil)

    import iat.openapi.json4s._

    val rs: Seq[(Metadata, JValue)] = ns.interpret.asMetaJValues ++
      myApp.interpretWith(ns).asMetaJValues ++
      myConfiguration.interpretWith(ns).asMetaJValues ++
      mySecret.interpretWith(ns).asMetaJValues

    Ensure(rs)
      .contain(
        Metadata("v1", "Namespace", "", ns.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: Namespace
            |apiVersion: v1
            |metadata:
            |  name: ${ns.name}
            |  labels:
            |    name: ${ns.name}
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, myApp.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: Service
            |apiVersion: v1
            |metadata:
            |  name: ${myApp.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${myApp.name}
            |spec: {}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, myApp.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: Deployment
            |apiVersion: apps/v1
            |metadata:
            |  name: ${myApp.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${myApp.name}
            |spec:
            |  template:
            |    spec:
            |      containers:
            |        - name: bar
            |""".stripMargin)),
        Metadata("v1", "ConfigMap", ns.name, myConfiguration.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: ConfigMap
            |apiVersion: v1
            |metadata:
            |  name: ${myConfiguration.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${myConfiguration.name}
            |data: {}
            |""".stripMargin)),
        Metadata("v1", "Secret", ns.name, mySecret.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: Secret
            |apiVersion: v1
            |metadata:
            |  name: ${mySecret.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${mySecret.name}
            |data: {}
            |""".stripMargin))
      )
  }
}
