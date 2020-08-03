package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.TCP
import com.virtuslab.iat.kubernetes.dsl.{Application, Container, Namespace}
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import skuber.{HTTPGetAction, Probe}

class SkuberDetailsTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "modify resources with the details" in {
    val ns = Namespace(Name("test") :: Nil)

    val app1Port = 60000
    val app1 = Application(
      Name("hello-world") :: Nil,
      Container(
        Name("hello") :: Nil,
        image = "gcr.io/google-samples/hello-app:2.0",
        envs = "PORT" -> s"$app1Port" :: Nil,
        ports = TCP(app1Port) :: Nil
      ) :: Nil
    )

    import iat.skuber.details._

    val app1Details = (
      nodePortService,
      replicas(3).andThen(readinessProbe(
        Probe(HTTPGetAction(skuber.portNumToNameablePort(app1Port), path="/healthz"))
      ))
    )

    import iat.kubernetes.dsl.experimental._
    import iat.skuber.experimental._
    import iat.skuber.playjson._
    import skuber.json.format._

    val resources: Seq[(Metadata, JsValue)] =
      ns.interpret.asMetaJsValues ++
      app1.inNamespace(ns).interpret.map(app1Details).asMetaJsValues

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", ns.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Namespace
            |metadata:
            |  name: ${ns.name}
            |  labels:
            |    name: ${ns.name}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, "hello-world") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: hello-world
            |  namespace: ${ns.name}
            |  labels:
            |    name: hello-world
            |spec:
            |  selector:
            |    matchLabels:
            |      name: hello-world
            |  replicas: 3
            |  template:
            |    metadata:
            |      labels:
            |        name: hello-world
            |    spec:
            |      containers:
            |      - name: hello
            |        image: "gcr.io/google-samples/hello-app:2.0"
            |        imagePullPolicy: IfNotPresent
            |        env:
            |        - name: "PORT"
            |          value: "60000"
            |        ports:
            |        - protocol: TCP
            |          containerPort: 60000
            |        readinessProbe:
            |          httpGet:
            |            port: 60000
            |            path: "/healthz"
            |            scheme: "HTTP"
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, "hello-world") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: hello-world
            |  namespace: ${ns.name}
            |  labels:
            |    name: hello-world
            |spec:
            |  type: NodePort
            |  selector:
            |    name: hello-world
            |  ports:
            |  - protocol: TCP
            |    port: 60000
            |    targetPort: 60000
            |  sessionAffinity: None
            |""".stripMargin)),
      )
  }
}
