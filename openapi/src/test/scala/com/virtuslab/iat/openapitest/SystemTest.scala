package com.virtuslab.iat.openapitest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Protocols
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Gateway, Namespace }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.json4s.jackson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SystemTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "serialize Namespace to JSON" in {
    val app1: Application = Application(Name("anApp") :: Nil)
    val conf1: Configuration = Configuration(Name("aConf") :: Nil, Map())
    val gw1: Gateway = Gateway(Name("aGate") :: Nil, Protocols.Any)

    val ns = Namespace(Name("theNamespace") :: Nil)

    import iat.openapi.json4s._
    val rs = ns.interpret.asMetaJValues ++
      app1.interpretWith(ns).asMetaJValues ++
      conf1.interpretWith(ns).asMetaJValues ++
      gw1.interpretWith(ns).asMetaJValues

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
        """.stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, app1.name) -> matchJson(yamlToJson(s"""
            |---
            |kind: Deployment
            |apiVersion: apps/v1
            |metadata:
            |  name: ${app1.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${app1.name}
            |spec:
            |  template:
            |    spec:
            |      containers:
            |        - name: anApp
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, app1.name) -> matchJson(yamlToJson(s"""
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: ${app1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${app1.name}
             |spec: {}
             |""".stripMargin)),
        Metadata("v1", "ConfigMap", ns.name, conf1.name) -> matchJson(yamlToJson(s"""
             |---
             |kind: ConfigMap
             |apiVersion: v1
             |metadata:
             |  name: ${conf1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${conf1.name}
             |data: {}
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1beta1", "Ingress", ns.name, gw1.name) -> matchJson(yamlToJson(s"""
             |---
             |kind: Ingress
             |apiVersion: networking.k8s.io/v1beta1
             |metadata:
             |  name: ${gw1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${gw1.name}
             |""".stripMargin))
      )
  }
}
