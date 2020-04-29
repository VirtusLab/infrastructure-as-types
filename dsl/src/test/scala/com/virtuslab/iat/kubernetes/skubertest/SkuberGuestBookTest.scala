package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.Label.{ App, Name, Role, Tier }
import com.virtuslab.iat.dsl.Port
import com.virtuslab.iat.dsl.kubernetes.{ Application, Container, Namespace }
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.test.EnsureMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import skuber.Resource.Quantity
import skuber.{ Resource, Service }

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

    val redisSlave = Application(
      Name("redis-slave") :: App("redis") :: Role("slave") :: Tier("backend") :: Nil,
      Container(
        Name("slave") :: Nil,
        image = "gcr.io/google_samples/gb-redisslave:v3",
        ports = Port(6379) :: Nil,
        envs = "GET_HOSTS_FROM" -> "dns" :: Nil
      ) :: Nil
    )

    val frontend = Application(
      Name("frontend") :: App("guestbook") :: Tier("frontend") :: Nil,
      Container(
        Name("php-redis") :: Nil,
        image = "gcr.io/google-samples/gb-frontend:v4",
        ports = Port(80) :: Nil,
        envs = "GET_HOSTS_FROM" -> "dns" :: Nil
      ) :: Nil
    )

    import kubernetes.skuber.details._

    val redisMasterDetails = resourceRequirements(
      Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      )
    )

    val redisSlaveDetails = resourceRequirements(
      Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      )
    ).andThen(
      replicas(2)
    )

    val frontendDetails = resourceRequirements(
      Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      )
    ).andThen(
      replicas(3)
    )
    .andThen(
      serviceType(Service.Type.NodePort)
    )

    import _root_.skuber.json.format._
    import kubernetes.skuber._
    import kubernetes.skuber.metadata._

    val resources: List[(Metadata, JsValue)] =
      guestbook.interpret.map(_.asMetaJsValue) ++
        redisMaster
          .interpret(guestbook, redisMasterDetails)
          .map((r: SResource[_ <: Base]) => r.asMetaJsValue) ++
        redisSlave
          .interpret(guestbook, redisSlaveDetails)
          .map((r: SResource[_ <: Base]) => r.asMetaJsValue) ++
        frontend
          .interpret(guestbook, frontendDetails)
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
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", guestbook.name, redisSlave.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: redis-slave
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: redis-slave
            |    app: redis
            |    role: slave
            |    tier: backend
            |spec:
            |  selector:
            |    matchLabels:
            |      name: redis-slave
            |      app: redis
            |      role: slave
            |      tier: backend
            |  replicas: 2
            |  template:
            |    metadata:
            |      labels:
            |        name: redis-slave
            |        app: redis
            |        role: slave
            |        tier: backend
            |    spec:
            |      containers:
            |      - name: slave
            |        image: gcr.io/google_samples/gb-redisslave:v3
            |        imagePullPolicy: IfNotPresent
            |        resources:
            |          requests:
            |            cpu: 100m
            |            memory: 100Mi
            |        env:
            |        - name: GET_HOSTS_FROM
            |          value: dns
            |          # Using `GET_HOSTS_FROM=dns` requires your cluster to provide a dns service.
            |        ports:
            |        - containerPort: 6379
            |          protocol: TCP
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", guestbook.name, redisSlave.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: redis-slave
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: redis-slave
            |    app: redis
            |    role: slave
            |    tier: backend
            |spec:
            |  type: ClusterIP
            |  sessionAffinity: None
            |  ports:
            |  - port: 6379
            |    targetPort: 6379
            |    protocol: TCP
            |  selector:
            |    name: redis-slave
            |    app: redis
            |    role: slave
            |    tier: backend
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", guestbook.name, frontend.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: apps/v1
            |kind: Deployment
            |metadata:
            |  name: frontend
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: frontend
            |    app: guestbook
            |    tier: frontend
            |spec:
            |  selector:
            |    matchLabels:
            |      name: frontend
            |      app: guestbook
            |      tier: frontend
            |  replicas: 3
            |  template:
            |    metadata:
            |      labels:
            |        name: frontend
            |        app: guestbook
            |        tier: frontend
            |    spec:
            |      containers:
            |      - name: php-redis
            |        image: gcr.io/google-samples/gb-frontend:v4
            |        imagePullPolicy: IfNotPresent
            |        resources:
            |          requests:
            |            cpu: 100m
            |            memory: 100Mi
            |        env:
            |        - name: GET_HOSTS_FROM
            |          value: dns # Using `GET_HOSTS_FROM=dns` requires your cluster to provide a dns service.
            |        ports:
            |        - containerPort: 80
            |          protocol: TCP
            |      restartPolicy: Always
            |      dnsPolicy: ClusterFirst
            |""".stripMargin)),
        Metadata("v1", "Service", guestbook.name, frontend.name) -> matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Service
            |metadata:
            |  name: frontend
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: frontend
            |    app: guestbook
            |    tier: frontend
            |spec:
            |  # comment or delete the following line if you want to use a LoadBalancer
            |  type: NodePort
            |  # if your cluster supports it, uncomment the following to automatically create
            |  # an external load-balanced IP for the frontend service.
            |  # type: LoadBalancer
            |  sessionAffinity: None
            |  ports:
            |  - port: 80
            |    targetPort: 80
            |    protocol: TCP
            |  selector:
            |    name: frontend
            |    app: guestbook
            |    tier: frontend
            |""".stripMargin))
      )
  }
}
