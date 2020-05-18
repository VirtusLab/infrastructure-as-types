package com.virtuslab.iat.skubertest

import _root_.skuber.Resource.Quantity
import _root_.skuber.{ Resource, Service }
import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ App, Name, Role, Tier }
import com.virtuslab.iat.dsl.{ IP, TCP }
import com.virtuslab.iat.kubernetes.dsl._
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue

class SkuberGuestBookTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "create Guestbook Example resources" in {

    val guestbook = Namespace(Name("guestbook") :: Nil)

    val redisMaster = Application(
      Name("redis-master") :: App("redis") :: Role("master") :: Tier("backend") :: Nil,
      Container(
        Name("master") :: Nil,
        image = "k8s.gcr.io/redis:e2e",
        ports = TCP(6379) :: Nil
      ) :: Nil
    )

    val redisSlave = Application(
      Name("redis-slave") :: App("redis") :: Role("slave") :: Tier("backend") :: Nil,
      Container(
        Name("slave") :: Nil,
        image = "gcr.io/google_samples/gb-redisslave:v3",
        ports = TCP(6379) :: Nil,
        envs = "GET_HOSTS_FROM" -> "dns" :: Nil
      ) :: Nil
    )

    val frontend = Application(
      Name("frontend") :: App("guestbook") :: Tier("frontend") :: Nil,
      Container(
        Name("php-redis") :: Nil,
        image = "gcr.io/google-samples/gb-frontend:v4",
        ports = TCP(80) :: Nil,
        envs = "GET_HOSTS_FROM" -> "dns" :: Nil
      ) :: Nil
    )

    import iat.kubernetes.dsl.NetworkPolicy.ops._

    // external traffic - from external sources
    val connExtFront = frontend
      .communicatesWith(
        Select.any.withIPs(IP.Range("0.0.0.0/0")).withPorts(frontend.allPorts: _*)
      )
      .ingressOnly
      .named("external-frontend")

    // internal traffic - between components
    val connFrontRedis = frontend
      .communicatesWith(redisMaster)
      .egressOnly
      .labeled(Name("front-redis") :: App("guestbook") :: Nil)
    val connRedisMS = redisMaster
      .communicatesWith(redisSlave)
      .labeled(Name("redis-master-slave") :: App("guestbook") :: Nil)
    val connRedisSM = redisSlave
      .communicatesWith(redisMaster)
      .labeled(Name("redis-slave-master") :: App("guestbook") :: Nil)

    // cluster traffic - to in-cluster services
    val connFrontDns = frontend
      .communicatesWith(NetworkPolicy.peer.kubernetesDns)
      .egressOnly
      .named("front-k8s-dns")
    val connRedisSlaveDns = redisSlave
      .communicatesWith(NetworkPolicy.peer.kubernetesDns)
      .egressOnly
      .named("redis-slave-k8s-dns")

    import iat.skuber.details._

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

    // format: off
    val frontendDetails = resourceRequirements(
      Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      )
    ).andThen(
      replicas(3)
    ).andThen(
      serviceType(Service.Type.NodePort)
    )

    import iat.skuber.playjson._
    import skuber.json.format._

    val ns: Seq[(Metadata, JsValue)] =
      guestbook.interpret.asMetaJsValues
    val apps: Seq[(Metadata, JsValue)] = List(
      redisMaster
        .interpret(guestbook)
        .map(redisMasterDetails),
      redisSlave
        .interpret(guestbook)
        .map(redisSlaveDetails),
      frontend
        .interpret(guestbook)
        .map(frontendDetails)
    ).flatMap(_.asMetaJsValues)

    val conns: Seq[(Metadata, JsValue)] = List(
      NetworkPolicy.default.denyAll.interpret(guestbook),
      connExtFront.interpret(guestbook),
      connFrontRedis.interpret(guestbook),
      connRedisMS.interpret(guestbook),
      connRedisSM.interpret(guestbook),
      connFrontDns.interpret(guestbook),
      connRedisSlaveDns.interpret(guestbook)
    ).flatMap(_.asMetaJsValues)

    val resources = ns ++ apps ++ conns

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", guestbook.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: v1
            |kind: Namespace
            |metadata:
            |  name: ${guestbook.name}
            |  labels:
            |    name: ${guestbook.name}
            |""".stripMargin)),
        Metadata("apps/v1", "Deployment", guestbook.name, redisMaster.name) -> matchJson(yamlToJson(s"""
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
        Metadata("v1", "Service", guestbook.name, redisMaster.name) -> matchJson(yamlToJson(s"""
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
        Metadata("apps/v1", "Deployment", guestbook.name, redisSlave.name) -> matchJson(yamlToJson(s"""
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
        Metadata("v1", "Service", guestbook.name, redisSlave.name) -> matchJson(yamlToJson(s"""
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
        Metadata("apps/v1", "Deployment", guestbook.name, frontend.name) -> matchJson(yamlToJson(s"""
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
        Metadata("v1", "Service", guestbook.name, frontend.name) -> matchJson(yamlToJson(s"""
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
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, "default-deny-all") ->
          matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-all
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: default-deny-all
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Ingress
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connExtFront.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connExtFront.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connExtFront.name}
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: frontend
            |      app: guestbook
            |      tier: frontend
            |  ingress:
            |  - ports:
            |    - port: 80
            |      protocol: TCP
            |    from:
            |    - ipBlock:
            |        cidr: "0.0.0.0/0"
            |  policyTypes:
            |    - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connFrontRedis.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connFrontRedis.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connFrontRedis.name}
            |    app: guestbook
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: frontend
            |      app: guestbook
            |      tier: frontend
            |  egress:
            |  - ports:
            |    - port: 6379
            |      protocol: TCP
            |    to:
            |    - podSelector:
            |        matchLabels:
            |          name: redis-master
            |          app: redis
            |          role: master
            |          tier: backend
            |  policyTypes:
            |    - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connRedisMS.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connRedisMS.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connRedisMS.name}
            |    app: guestbook
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: redis-master
            |      app: redis
            |      role: master
            |      tier: backend
            |  ingress:
            |  - ports:
            |    - port: 6379
            |      protocol: TCP
            |    from:
            |    - podSelector:
            |        matchLabels:
            |          name: redis-slave
            |          app: redis
            |          role: slave
            |          tier: backend
            |  egress:
            |  - ports:
            |    - port: 6379
            |      protocol: TCP
            |    to:
            |    - podSelector:
            |        matchLabels:
            |          name: redis-slave
            |          app: redis
            |          role: slave
            |          tier: backend
            |  policyTypes:
            |    - Ingress
            |    - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connRedisSM.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connRedisSM.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connRedisSM.name}
            |    app: guestbook
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: redis-slave
            |      app: redis
            |      role: slave
            |      tier: backend
            |  ingress:
            |  - ports:
            |    - port: 6379
            |      protocol: TCP
            |    from:
            |    - podSelector:
            |        matchLabels:
            |          name: redis-master
            |          app: redis
            |          role: master
            |          tier: backend
            |  egress:
            |  - ports:
            |    - port: 6379
            |      protocol: TCP
            |    to:
            |    - podSelector:
            |        matchLabels:
            |          name: redis-master
            |          app: redis
            |          role: master
            |          tier: backend
            |  policyTypes:
            |    - Ingress
            |    - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connFrontDns.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connFrontDns.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connFrontDns.name}
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: frontend
            |      app: guestbook
            |      tier: frontend
            |  egress:
            |  - ports:
            |    - port: 53
            |      protocol: UDP
            |    - port: 53
            |      protocol: TCP
            |    to:
            |    - namespaceSelector:
            |        matchLabels:
            |          name: kube-system
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", guestbook.name, connRedisSlaveDns.name) -> matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: ${connRedisSlaveDns.name}
            |  namespace: ${guestbook.name}
            |  labels:
            |    name: ${connRedisSlaveDns.name}
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: redis-slave
            |      app: redis
            |      role: slave
            |      tier: backend
            |  egress:
            |  - ports:
            |    - port: 53
            |      protocol: UDP
            |    - port: 53
            |      protocol: TCP
            |    to:
            |    - namespaceSelector:
            |        matchLabels:
            |          name: kube-system
            |  policyTypes:
            |  - Egress
            |""".stripMargin))
      )
  }
}
