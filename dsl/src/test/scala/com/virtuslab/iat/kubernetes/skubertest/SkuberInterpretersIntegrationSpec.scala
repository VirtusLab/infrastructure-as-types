package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.Port.NamedPort
import com.virtuslab.iat.dsl.kubernetes.{ Application, Namespace }
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.test.EnsureMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberInterpretersIntegrationSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  import skuber.json.format._

  it should "create a simple system" in {
    import kubernetes.skuber._
    import kubernetes.skuber.metadata._

    val ns = Namespace(Name("foo") :: Nil)
    val app1 = Application(Name("app-one") :: Nil, image = "image-app-one", ports = Port(9090) :: Nil)

    val app2 = Application(
      Name("app-two") :: Nil,
      image = "image-app-two",
      ports = NamedPort("http-port", 9090) :: Nil
    )

    val resources = interpret(ns) ++ interpret(app1, ns) ++ interpret(app2, ns)

    Ensure(resources)
      .contain(
        Metadata("v1", "Service", ns.name, "app-one") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: v1
          |kind: Service
          |metadata:
          |  name: app-one
          |  namespace: ${ns.name}
          |  labels:
          |    name: app-one
          |spec:
          |  type: ClusterIP
          |  selector:
          |    name: app-one
          |  ports:
          |  - protocol: TCP
          |    port: 9090
          |    targetPort: 9090
          |  sessionAffinity: None
          |""".stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, "app-one") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: apps/v1
          |kind: Deployment
          |metadata:
          |  name: app-one
          |  namespace: ${ns.name}
          |  labels:
          |    name: app-one
          |spec:
          |  selector:
          |    matchLabels:
          |      name: app-one
          |  replicas: 1
          |  template:
          |    metadata:
          |      labels:
          |        name: app-one
          |    spec:
          |      containers:
          |      - name: app-one
          |        image: image-app-one
          |        imagePullPolicy: IfNotPresent
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)),
        Metadata("v1", "Service", ns.name, "app-two") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: v1
          |kind: Service
          |metadata:
          |  name: app-two
          |  namespace: ${ns.name}
          |  labels:
          |    name: app-two
          |spec:
          |  type: ClusterIP
          |  sessionAffinity: None
          |  ports:
          |  - name: http-port
          |    protocol: TCP
          |    port: 9090
          |    targetPort: 9090
          |  selector:
          |    name: app-two
          |""".stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, "app-two") -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: app-two
          |  namespace: ${ns.name}
          |  labels:
          |    name: app-two
          |spec:
          |  selector:
          |    matchLabels:
          |      name: app-two
          |  replicas: 1
          |  template:
          |    metadata:
          |      labels:
          |        name: app-two
          |    spec:
          |      containers:
          |      - name: app-two
          |        image: image-app-two
          |        imagePullPolicy: IfNotPresent
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |          name: http-port
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)),
        Metadata("v1", "Namespace", "default", ns.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: ${ns.name}
          |  labels:
          |    name: ${ns.name}
          |""".stripMargin))
      )
  }
}
