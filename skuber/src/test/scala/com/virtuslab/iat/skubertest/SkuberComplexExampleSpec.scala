package com.virtuslab.iat.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ App, Name }
import com.virtuslab.iat.kubernetes.dsl.{ Application, Container, Namespace, SelectedIPs }
import com.virtuslab.iat.dsl.{ IP, Port }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberComplexExampleSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  it should "scaffold infrastructure" in {
    val namespace = Namespace(Name("reactive-system") :: Nil)

    val api = Application(
      Name("api") :: Nil,
      Container(
        Name("api") :: Nil,
        image = "some.ecr.io/api:1.0",
        ports = Port(80) :: Nil
      ) :: Nil
    )

    val processor = Application(
      Name("processor") :: Nil,
      Container(
        Name("api") :: Nil,
        image = "some.ecr.io/api:1.0"
      ) :: Nil
    )

    val view = Application(
      Name("view") :: Nil,
      Container(
        Name("api") :: Nil,
        image = "some.ecr.io/api:1.0",
        ports = Port(8080) :: Nil
      ) :: Nil
    )

    val cassandra = Application(
      Name("cassandra-node") :: App("cassanrda") :: Nil,
      Container(
        Name("cassandra") :: Nil,
        image = "some.ecr.io/cassandra:3.0",
        ports = Port(6379) :: Nil //FIXME
      ) :: Nil
    )

    val postgres = Application(
      Name("postgres") :: App("postgres") :: Nil,
      Container(
        Name("postgres") :: Nil,
        image = "some.ecr.io/postgres:10.12",
        ports = Port(5432) :: Nil
      ) :: Nil
    )

    val kafka = Application(
      Name("kafka-node") :: App("kafka") :: Nil,
      Container(
        Name("master") :: Nil,
        image = "some.ecr.io/kafka:2.5.0",
        ports = Port(9092) :: Nil
      ) :: Nil
    )

    import iat.kubernetes.dsl.ops._

    val connExtApi = api
      .communicatesWith(
        SelectedIPs(IP.Range("0.0.0.0/0")).ports(api.allPorts: _*)
      )
      .ingressOnly
      .named("external-api")

    val conApiKafka = api.communicatesWith(kafka).named("api-kafka")
    val conApiView = api.communicatesWith(view).egressOnly.named("api-kafka")
    val conViewPostgres = view.communicatesWith(postgres).egressOnly.named("view-postgres")
    val conKafkaProcessor = kafka.communicatesWith(processor).ingressOnly.named("kafka-processor")
    val conProcessorCassandra = processor.communicatesWith(cassandra).egressOnly.named("processor-cassandra")

    import iat.skuber.playjson._
    import skuber.json.format._

    val resource = namespace.interpret.asMetaJsValues ++
      (api :: processor :: view :: cassandra :: postgres :: kafka :: Nil)
        .flatMap(_.interpret(namespace).asMetaJsValues) ++
      (connExtApi :: conApiKafka :: conApiView :: conViewPostgres :: conKafkaProcessor :: conProcessorCassandra :: Nil)
        .flatMap(_.interpret(namespace).asMetaJsValues)

    Ensure(resource)
      .contain(
        Metadata("v1", "Namespace", "default", "reactive-system") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Namespace
             |apiVersion: v1
             |metadata:
             |  name: reactive-system
             |  labels:
             |    name: reactive-system
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "api") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: api
             |  namespace: ${namespace.name}
             |  labels:
             |    name: api
             |spec:
             |  ports:
             |  - protocol: TCP
             |    port: 80
             |    targetPort: 80
             |  selector:
             |    name: api
             |  type: ClusterIP
             |  sessionAffinity: None
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "api") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: api
             |  namespace: ${namespace.name}
             |  labels:
             |    name: api
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: api
             |  template:
             |    metadata:
             |      labels:
             |        name: api
             |    spec:
             |      containers:
             |      - name: api
             |        image: some.ecr.io/api:1.0
             |        ports:
             |        - containerPort: 80
             |          protocol: TCP
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "processor") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: processor
             |  namespace: ${namespace.name}
             |  labels:
             |    name: processor
             |spec:
             |  selector:
             |    name: processor
             |  type: ClusterIP
             |  sessionAffinity: None
             |
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "processor") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: processor
             |  namespace: ${namespace.name}
             |  labels:
             |    name: processor
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: processor
             |  template:
             |    metadata:
             |      labels:
             |        name: processor
             |    spec:
             |      containers:
             |      - name: api
             |        image: some.ecr.io/api:1.0
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "view") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: view
             |  namespace: ${namespace.name}
             |  labels:
             |    name: view
             |spec:
             |  ports:
             |  - protocol: TCP
             |    port: 8080
             |    targetPort: 8080
             |  selector:
             |    name: view
             |  type: ClusterIP
             |  sessionAffinity: None
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "view") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: view
             |  namespace: ${namespace.name}
             |  labels:
             |    name: view
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: view
             |  template:
             |    metadata:
             |      labels:
             |        name: view
             |    spec:
             |      containers:
             |      - name: api
             |        image: some.ecr.io/api:1.0
             |        ports:
             |        - containerPort: 8080
             |          protocol: TCP
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "cassandra-node") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: cassandra-node
             |  namespace: ${namespace.name}
             |  labels:
             |    name: cassandra-node
             |    app: cassanrda
             |spec:
             |  ports:
             |  - protocol: TCP
             |    port: 6379
             |    targetPort: 6379
             |  selector:
             |    name: cassandra-node
             |    app: cassanrda
             |  type: ClusterIP
             |  sessionAffinity: None
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "cassandra-node") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: cassandra-node
             |  namespace: ${namespace.name}
             |  labels:
             |    name: cassandra-node
             |    app: cassanrda
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: cassandra-node
             |      app: cassanrda
             |  template:
             |    metadata:
             |      labels:
             |        name: cassandra-node
             |        app: cassanrda
             |    spec:
             |      containers:
             |      - name: cassandra
             |        image: some.ecr.io/cassandra:3.0
             |        ports:
             |        - containerPort: 6379
             |          protocol: TCP
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "postgres") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: postgres
             |  namespace: ${namespace.name}
             |  labels:
             |    name: postgres
             |    app: postgres
             |spec:
             |  ports:
             |  - protocol: TCP
             |    port: 5432
             |    targetPort: 5432
             |  selector:
             |    name: postgres
             |    app: postgres
             |  type: ClusterIP
             |  sessionAffinity: None
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "postgres") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: postgres
             |  namespace: ${namespace.name}
             |  labels:
             |    name: postgres
             |    app: postgres
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: postgres
             |      app: postgres
             |  template:
             |    metadata:
             |      labels:
             |        name: postgres
             |        app: postgres
             |    spec:
             |      containers:
             |      - name: postgres
             |        image: some.ecr.io/postgres:10.12
             |        ports:
             |        - containerPort: 5432
             |          protocol: TCP
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("v1", "Service", "reactive-system", "kafka-node") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: kafka-node
             |  namespace: ${namespace.name}
             |  labels:
             |    name: kafka-node
             |    app: kafka
             |spec:
             |  ports:
             |  - protocol: TCP
             |    port: 9092
             |    targetPort: 9092
             |  selector:
             |    name: kafka-node
             |    app: kafka
             |  type: ClusterIP
             |  sessionAffinity: None
             |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "reactive-system", "kafka-node") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: Deployment
             |apiVersion: apps/v1
             |metadata:
             |  name: kafka-node
             |  namespace: ${namespace.name}
             |  labels:
             |    name: kafka-node
             |    app: kafka
             |spec:
             |  replicas: 1
             |  selector:
             |    matchLabels:
             |      name: kafka-node
             |      app: kafka
             |  template:
             |    metadata:
             |      labels:
             |        name: kafka-node
             |        app: kafka
             |    spec:
             |      containers:
             |      - name: master
             |        image: some.ecr.io/kafka:2.5.0
             |        ports:
             |        - containerPort: 9092
             |          protocol: TCP
             |        imagePullPolicy: IfNotPresent
             |      restartPolicy: Always
             |      dnsPolicy: ClusterFirst
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "external-api") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: external-api
             |  namespace: ${namespace.name}
             |  labels:
             |    name: external-api
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: api
             |  ingress:
             |  - ports:
             |    - port: 80
             |      protocol: TCP
             |    from:
             |    - ipBlock:
             |        cidr: 0.0.0.0/0
             |  policyTypes:
             |  - Ingress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "api-kafka") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: api-kafka
             |  namespace: ${namespace.name}
             |  labels:
             |    name: api-kafka
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: api
             |  ingress:
             |  - ports:
             |    - port: 9092
             |      protocol: TCP
             |    from:
             |    - podSelector:
             |        matchLabels:
             |          name: kafka-node
             |          app: kafka
             |  egress:
             |  - ports:
             |    - port: 9092
             |      protocol: TCP
             |    to:
             |    - podSelector:
             |        matchLabels:
             |          name: kafka-node
             |          app: kafka
             |  policyTypes:
             |  - Ingress
             |  - Egress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "api-kafka") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: api-kafka
             |  namespace: ${namespace.name}
             |  labels:
             |    name: api-kafka
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: api
             |  egress:
             |  - ports:
             |    - port: 8080
             |      protocol: TCP
             |    to:
             |    - podSelector:
             |        matchLabels:
             |          name: view
             |  policyTypes:
             |  - Egress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "view-postgres") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: view-postgres
             |  namespace: ${namespace.name}
             |  labels:
             |    name: view-postgres
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: view
             |  egress:
             |  - ports:
             |    - port: 5432
             |      protocol: TCP
             |    to:
             |    - podSelector:
             |        matchLabels:
             |          name: postgres
             |          app: postgres
             |  policyTypes:
             |  - Egress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "kafka-processor") -> matchJsonString(yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: kafka-processor
             |  namespace: ${namespace.name}
             |  labels:
             |    name: kafka-processor
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: kafka-node
             |      app: kafka
             |  ingress:
             |  - from:
             |    - podSelector:
             |        matchLabels:
             |          name: processor
             |  policyTypes:
             |  - Ingress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "reactive-system", "processor-cassandra") -> matchJsonString(
          yamlToJson(s"""
             |---
             |kind: NetworkPolicy
             |apiVersion: networking.k8s.io/v1
             |metadata:
             |  name: processor-cassandra
             |  namespace: ${namespace.name}
             |  labels:
             |    name: processor-cassandra
             |spec:
             |  podSelector:
             |    matchLabels:
             |      name: processor
             |  egress:
             |  - ports:
             |    - port: 6379
             |      protocol: TCP
             |    to:
             |    - podSelector:
             |        matchLabels:
             |          name: cassandra-node
             |          app: cassanrda
             |  policyTypes:
             |  - Egress
             |""".stripMargin)
        )
      )
  }
}
