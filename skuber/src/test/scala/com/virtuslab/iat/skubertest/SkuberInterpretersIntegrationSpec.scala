package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.TCP
import com.virtuslab.iat.kubernetes.dsl.{ Application, Container, Namespace }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberInterpretersIntegrationSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "create a simple system" in {
    val ns = Namespace(Name("foo") :: Nil)
    val app1 = Application(
      Name("app-one") :: Nil,
      Container(
        Name("app") :: Nil,
        image = "image-app-one",
        ports = TCP(9090) :: Nil
      ) :: Nil
    )

    val app2 = Application(
      Name("app-two") :: Nil,
      Container(
        Name("app") :: Nil,
        image = "image-app-two",
        ports = TCP("http-port", 9090) :: Nil
      ) :: Nil
    )

    import iat.skuber.playjson._
    import skuber.json.format._

    val resources =
      ns.interpret.asMetaJsValues ++
        app1
          .interpretWith(ns)
          .reduce(_.asMetaJsValues) ++
        app2
          .interpretWith(ns)
          .reduce(_.asMetaJsValues)

    Ensure(resources)
      .contain(
        Metadata("v1", "Service", ns.name, "app-one") -> matchJson(yamlToJson(s"""
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
        Metadata("apps/v1", "Deployment", ns.name, "app-one") -> matchJson(yamlToJson(s"""
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
          |      - name: app
          |        image: image-app-one
          |        imagePullPolicy: IfNotPresent
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)),
        Metadata("v1", "Service", ns.name, "app-two") -> matchJson(yamlToJson(s"""
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
        Metadata("apps/v1", "Deployment", ns.name, "app-two") -> matchJson(yamlToJson(s"""
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
          |      - name: app
          |        image: image-app-two
          |        imagePullPolicy: IfNotPresent
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |          name: http-port
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)),
        Metadata("v1", "Namespace", "default", ns.name) -> matchJson(yamlToJson(s"""
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
