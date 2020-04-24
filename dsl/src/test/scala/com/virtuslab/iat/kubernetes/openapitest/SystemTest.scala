package com.virtuslab.iat.kubernetes.openapitest

import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Protocols
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Gateway, Namespace }
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.kubernetes.{ openapi, Metadata }
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson

class SystemTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "serialize Namespace to JSON" in {
    import openapi._
    import openapi.json4s._
    import openapi.json4s.InterpreterDerivation._

    case class Group1(
        app1: Application = Application(Name("anApp") :: Nil, image = ""),
        conf1: Configuration = Configuration(Name("aConf") :: Nil, Map()),
        gw1: Gateway = Gateway(Name("aGate") :: Nil, Protocols.Any))

    val g1 = Group1()
    val ns = Namespace(Name("theNamespace") :: Nil)

    val js = interpret(ns) ++ interpret(g1, ns)

    Ensure(asMetaJsonString(js.map(_.result)))
      .contain(
        Metadata("v1", "Namespace", "", ns.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Namespace
            |apiVersion: v1
            |metadata:
            |  name: ${ns.name}
            |  labels:
            |    name: ${ns.name}
        """.stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, g1.app1.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Deployment
            |apiVersion: apps/v1
            |metadata:
            |  name: ${g1.app1.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${g1.app1.name}
            |spec:
            |  template:
            |    spec:
            |      containers:
            |        - name: anApp
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, g1.app1.name) -> matchJsonString(yamlToJson(s"""
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: ${g1.app1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${g1.app1.name}
             |spec: {}
             |""".stripMargin)),
        Metadata("v1", "ConfigMap", ns.name, g1.conf1.name) -> matchJsonString(yamlToJson(s"""
             |---
             |kind: ConfigMap
             |apiVersion: v1
             |metadata:
             |  name: ${g1.conf1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${g1.conf1.name}
             |data: {}
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1beta1", "Ingress", ns.name, g1.gw1.name) -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Ingress
             |apiVersion: networking.k8s.io/v1beta1
             |metadata:
             |  name: ${g1.gw1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${g1.gw1.name}
             |""".stripMargin))
      )
  }
}
