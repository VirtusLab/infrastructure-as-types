package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.Port.NamedPort
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.materializer.skuber.{ Exporter, Metadata }

class InterpretersIntegrationSpec extends InterpreterSpec[SkuberContext] with JsonMatchers {
  import com.virtuslab.interpreter.skuber.Skuber._

  it should "create a simple system" in {
    val namespaceName = generateNamespaceName()
    val system = DistributedSystem(generateSystemName()).inSystem { implicit ds: SystemBuilder[SkuberContext] =>
      import ds._

      namespaces(
        Namespace(namespaceName).inNamespace { implicit ns: NamespaceBuilder[SkuberContext] =>
          import ns._

          applications(
            Application(Labels(Name("app-one")), "image-app-one", ports = Port(9090) :: Nil),
            Application(Labels(Name("app-two")), "image-app-two", ports = NamedPort("http-port", 9090) :: Nil)
          )
        }
      )
    }

    val resources = system.interpret().map(Exporter.metaAndJsValue)

    Ensure(resources)
      .contain(
        Metadata("v1", "Service", namespaceName, "app-one") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: v1
          |kind: Service
          |metadata:
          |  name: app-one
          |  namespace: $namespaceName
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
        Metadata("apps/v1", "Deployment", namespaceName, "app-one") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: apps/v1
          |kind: Deployment
          |metadata:
          |  name: app-one
          |  namespace: $namespaceName
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
        Metadata("v1", "Service", namespaceName, "app-two") -> matchJsonString(yamlToJson(s"""
          |---
          |apiVersion: v1
          |kind: Service
          |metadata:
          |  name: app-two
          |  namespace: $namespaceName
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
        Metadata("apps/v1", "Deployment", namespaceName, "app-two") -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: app-two
          |  namespace: $namespaceName
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
        Metadata("v1", "Namespace", "default", namespaceName) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: $namespaceName
          |  labels:
          |    name: $namespaceName
          |""".stripMargin))
      )
  }
}
