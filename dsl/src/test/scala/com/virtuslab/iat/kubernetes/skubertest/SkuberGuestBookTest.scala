package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.test.EnsureMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.virtuslab.iat.dsl.Label.{ App, Name, Role, Tier }
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Container, Namespace }
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import org.json4s.Formats
import play.api.libs.json.JsValue
import skuber.Resource.Quantity
import skuber.{ Resource, Service, Container => SContainer }
import skuber.apps.v1.Deployment

class SkuberGuestBookTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "create Guestbook Example resources" in {

    val guestbook = Namespace(Name("guestbook") :: Nil)

    val redisMaster = Application(
      Name("redis-master") :: App("redis") :: Role("master") :: Tier("backend") :: Nil,
      Container(
        Name("master") :: Nil,
        image = "k8s.gcr.io/redis:e2e",
        ports = Port(6379) :: Nil
      ) :: Nil
    )

    def redisMasterDetails(s: Service, dpl: Deployment): (Service, Deployment) = {
      // format: off
      import com.softwaremill.quicklens._
      (s, dpl.modify(_.spec.each.template.spec.each.containers.each.resources).setTo(Some(Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      ))))
    }

    import kubernetes.skuber._
    import kubernetes.skuber.metadata._
    import _root_.skuber.json.format._

    val resources: List[(Metadata, JsValue)] =
      guestbook.interpret.map(_.asMetaJsValue) ++
        redisMaster
          .interpret(guestbook, t => redisMasterDetails(t._1, t._2))
          .map((r: SResource[_ <: Base]) => r.asMetaJsValue)

    implicit val formats: Formats = JsonMethods.defaultFormats

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", guestbook.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Namespace
            |metadata:
            |  name: ${guestbook.name}
            |  labels:
            |    name: ${guestbook.name}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", guestbook.name, redisMaster.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: redis-master
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: redis-master
            |    app: redis
            |    role: master
            |    tier: backend
            |spec:
            |  selector:
            |    matchLabels:
            |      name: redis-master
            |      app: redis
            |      role: master
            |      tier: backend
            |  replicas: 1
            |  template:
            |    metadata:
            |      labels:
            |        name: redis-master
            |        app: redis
            |        role: master
            |        tier: backend
            |    spec:
            |      containers:
            |      - name: master
            |        image: k8s.gcr.io/redis:e2e  # or just image: redis
            |        imagePullPolicy: IfNotPresent
            |        resources:
            |          requests:
            |            cpu: 100m
            |            memory: 100Mi
            |        ports:
            |        - containerPort: 6379
            |          protocol: TCP
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", guestbook.name, redisMaster.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: ${redisMaster.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${redisMaster.name}
            |    app: redis
            |    role: master
            |    tier: backend
            |spec:
            |  type: ClusterIP
            |  sessionAffinity: None
            |  ports:
            |  - port: 6379
            |    targetPort: 6379
            |    protocol: TCP
            |  selector:
            |    name: ${redisMaster.name}
            |    app: redis
            |    role: master
            |    tier: backend
            |""".stripMargin))
      )
  }
}
