package com.virtuslab.dsl

import com.virtuslab.dsl.interpreter.{ InterpreterSpec, SystemInterpreter }
import com.virtuslab.internal.{ ShortMeta, SkuberConverter }
import com.virtuslab.scalatest.yaml.Converters.yamlToJson

class InterpretersIntegrationSpec extends InterpreterSpec {

  it should "create a simple system" in {
    val namespaceName = generateNamespaceName()
    val system = DistributedSystem.ref(generateSystemName()).inSystem { implicit ds =>
      import ds._

      namespaces(
        Namespace.ref(namespaceName).inNamespace { implicit ns =>
          import ns._

          applications(
            Application(Labels(Name("app-one")), "image-app-one", ports = Networked.Port(9090) :: Nil),
            Application(Labels(Name("app-two")), "image-app-two", ports = Networked.Port(9090, Some("http-port")) :: Nil)
          )
        }
      )
    }

    val resources = SkuberConverter(SystemInterpreter.of(system)).toMetaAndJsValue

    Ensure(resources)
      .contain(
        ShortMeta("v1", "Service", namespaceName, "app-one") -> yamlToJson(s"""
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
          |""".stripMargin),
        ShortMeta("apps/v1", "Deployment", namespaceName, "app-one") -> yamlToJson(s"""
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
          |""".stripMargin),
        ShortMeta("v1", "Service", namespaceName, "app-two") -> yamlToJson(s"""
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
          |""".stripMargin),
        ShortMeta("apps/v1", "Deployment", namespaceName, "app-two") -> yamlToJson(s"""
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
          |""".stripMargin),
        ShortMeta("v1", "Namespace", "default", namespaceName) -> yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: $namespaceName
          |  labels:
          |    name: $namespaceName
          |""".stripMargin)
      )
  }
}
