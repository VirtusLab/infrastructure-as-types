package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.{ HTTP, Port, Protocol, Protocols, TCP }
import com.virtuslab.iat.kubernetes.dsl.{ Application, Container, Gateway, Namespace }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import skuber.Service

class SkuberGKEIngressTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "allow for GKE specific ingress configuration" in {

    val gke = Namespace(Name("gke") :: Nil)

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

    val app2Port = 8080
    val app2 = Application(
      Name("hello-kubernetes") :: Nil,
      Container(
        Name("hello-again") :: Nil,
        image = "gcr.io/google-samples/node-hello:1.0",
        envs = "PORT" -> s"$app2Port" :: Nil,
        ports = TCP(app2Port) :: Nil
      ) :: Nil
    )

    val gw = Gateway(
      Name("my-ingress") :: Nil,
      inputs = Protocols(
        Protocol.Layers(l7 = HTTP(), l4 = TCP())
      ),
      outputs = Protocols(
        Protocol.Layers(l7 = HTTP(path = HTTP.Path("/*"), host = HTTP.Host(app1.name)), l4 = TCP(Port(app1Port))),
        Protocol.Layers(l7 = HTTP(path = HTTP.Path("/kube"), host = HTTP.Host(app2.name)), l4 = TCP(Port(app2Port)))
      )
    )

    import iat.skuber.details._
    def nativeService(a: (String, String)): Service => Service = {
      import com.softwaremill.quicklens._
      val annotate = (s: skuber.Service) =>
        s.modify(_.metadata.annotations)
          .using(
            _ ++ Map(a)
          )
      annotate.andThen(serviceType(Service.Type.ClusterIP))
    }

    val defaultService: Service => Service = serviceType(Service.Type.NodePort)

    val app1Details = (
//      nativeService("cloud.google.com/neg" -> """'{"ingress": true}'"""),
      defaultService,
      replicas(3)
    )
    val app2Details = (
//      nativeService("cloud.google.com/neg" -> """'{"ingress": true}'"""),
      defaultService,
      replicas(3)
    )

    import iat.kubernetes.dsl.experimental._
    import iat.skuber.experimental._
    import iat.skuber.playjson._
    import skuber.json.ext.format._
    import skuber.json.format._

    val ns: Seq[(Metadata, JsValue)] =
      gke.interpret.asMetaJsValues
    val apps: Seq[(Metadata, JsValue)] = List(
      app1.inNamespace(gke).interpret.map(app1Details),
      app2.inNamespace(gke).interpret.map(app2Details)
    ).flatMap(_.asMetaJsValues)
    val gws = gw.inNamespace(gke).interpret.asMetaJsValues

    val resources = ns ++ apps ++ gws

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", gke.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Namespace
            |metadata:
            |  name: ${gke.name}
            |  labels:
            |    name: ${gke.name}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", gke.name, "hello-world") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: hello-world
            |  namespace: ${gke.name}
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
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", gke.name, "hello-world") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: hello-world
            |  namespace: ${gke.name}
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
        Metadata("apps/v1", "Deployment", gke.name, "hello-kubernetes") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: hello-kubernetes
            |  namespace: ${gke.name}
            |  labels:
            |    name: hello-kubernetes
            |spec:
            |  selector:
            |    matchLabels:
            |      name: hello-kubernetes
            |  replicas: 3
            |  template:
            |    metadata:
            |      labels:
            |        name: hello-kubernetes
            |    spec:
            |      containers:
            |      - name: hello-again
            |        image: "gcr.io/google-samples/node-hello:1.0"
            |        imagePullPolicy: IfNotPresent
            |        env:
            |        - name: "PORT"
            |          value: "8080"
            |        ports:
            |        - protocol: TCP
            |          containerPort: 8080
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", gke.name, "hello-kubernetes") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: hello-kubernetes
            |  namespace: ${gke.name}
            |  labels:
            |    name: hello-kubernetes
            |spec:
            |  type: NodePort
            |  selector:
            |    name: hello-kubernetes
            |  ports:
            |  - protocol: TCP
            |    port: 8080
            |    targetPort: 8080
            |  sessionAffinity: None
            |""".stripMargin)),
        Metadata("extensions/v1beta1", "Ingress", gke.name, "my-ingress") -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: extensions/v1beta1
            |kind: Ingress
            |metadata:
            |  name: my-ingress
            |  namespace: ${gke.name}
            |  labels:
            |    name: my-ingress
            |spec:
            |  rules:
            |  - http:
            |      paths:
            |      - path: /*
            |        backend:
            |          serviceName: hello-world
            |          servicePort: 60000
            |  - http:
            |      paths:
            |      - path: /kube
            |        backend:
            |          serviceName: hello-kubernetes
            |          servicePort: 8080
            |""".stripMargin))
      )
  }
}
