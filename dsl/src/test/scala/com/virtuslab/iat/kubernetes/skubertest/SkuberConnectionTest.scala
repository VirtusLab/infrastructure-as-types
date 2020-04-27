package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.Label.{ Name, Role, UntypedLabel }
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.kubernetes._
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.{ dsl, kubernetes }
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.test.EnsureMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberConnectionTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "allow to express connections between two namespaces" in {
    import dsl.kubernetes.Connection.ops._

    val frontendRole = Role("frontend")
    val frontend = Namespace(Name("frontend") :: frontendRole :: Nil)

    val backendRole = Role("backend")
    val backend = Namespace(Name("backend") :: backendRole :: Nil)

    val app3 = Application(
      Name("app-three") :: backendRole :: Nil,
      Container(Name("app") :: Nil, image = "image-app-three") :: Nil
    )
    val connApp3 = app3.communicatesWith(frontend).named("app3-frontend-app3")

    val app1 = Application(
      Name("app-one") :: frontendRole :: Nil,
      Container(
        Name("app") :: Nil,
        image = "image-app-one",
        ports = Port(9090) :: Nil
      ) :: Nil
    )
    val app2 = Application(
      Name("app-two") :: frontendRole :: Nil,
      Container(
        Name("app") :: Nil,
        image = "image-app-two",
        ports = Port(9090) :: Nil
      ) :: Nil
    )
    val connApp1 = app1.communicatesWith(backend).named("app1-backend-app1")
    val connApp1app2 = app1.communicatesWith(app2).named("app1-app2-app1")

    import kubernetes.skuber._
    import kubernetes.skuber.metadata._
    import kubernetes.skuber.metadata.InterpreterDerivation._

    val resources = interpret(backend) ++
      interpret((app3, connApp3), backend) ++
      interpret(frontend) ++
      interpret((app1, app2, connApp1, connApp1app2), frontend)

    Ensure(resources.map(_.result))
      .contain(
        Metadata("v1", "Namespace", "default", frontend.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: ${frontend.name}
          |  labels:
          |    name: ${frontend.name}
          |    role: frontend
          |""".stripMargin)),
        Metadata("v1", "Namespace", "default", backend.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: ${backend.name}
          |  labels:
          |    name: ${backend.name}
          |    role: backend
          |""".stripMargin)),
        Metadata("v1", "Service", frontend.name, app1.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: ${app1.name}
          |  namespace: ${frontend.name}
          |  labels:
          |    name: ${app1.name}
          |    role: frontend
          |spec:
          |  ports:
          |  - protocol: TCP
          |    port: 9090
          |    targetPort: 9090
          |  selector:
          |    name: app-one
          |    role: frontend
          |  type: ClusterIP
          |  sessionAffinity: None
          |""".stripMargin)),
        Metadata("v1", "Service", frontend.name, app2.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: ${app2.name}
          |  namespace: ${frontend.name}
          |  labels:
          |    name: ${app2.name}
          |    role: frontend
          |spec:
          |  ports:
          |  - protocol: TCP
          |    port: 9090
          |    targetPort: 9090
          |  selector:
          |    name: app-two
          |    role: frontend
          |  type: ClusterIP
          |  sessionAffinity: None
          |""".stripMargin)),
        Metadata("v1", "Service", backend.name, app3.name) -> matchJsonString(yamlToJson(s"""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: ${app3.name}
          |  namespace: ${backend.name}
          |  labels:
          |    name: ${app3.name}
          |    role: backend
          |spec:
          |  selector:
          |    name: app-three
          |    role: backend
          |  type: ClusterIP
          |  sessionAffinity: None
          |""".stripMargin)),
        Metadata("apps/v1", "Deployment", backend.name, app3.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: ${app3.name}
          |  namespace: ${backend.name}
          |  labels:
          |    name: ${app3.name}
          |    role: backend
          |spec:
          |  replicas: 1
          |  selector:
          |    matchLabels:
          |      name: app-three
          |      role: backend
          |  template:
          |    metadata:
          |      labels:
          |        name: app-three
          |        role: backend
          |    spec:
          |      containers:
          |      - name: app
          |        image: image-app-three
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("apps/v1", "Deployment", frontend.name, app2.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: ${app2.name}
          |  namespace: ${frontend.name}
          |  labels:
          |    name: ${app2.name}
          |    role: frontend
          |spec:
          |  replicas: 1
          |  selector:
          |    matchLabels:
          |      name: app-two
          |      role: frontend
          |  template:
          |    metadata:
          |      labels:
          |        name: app-two
          |        role: frontend
          |    spec:
          |      containers:
          |      - name: app
          |        image: image-app-two
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("apps/v1", "Deployment", frontend.name, app1.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: ${app1.name}
          |  namespace: ${frontend.name}
          |  labels:
          |    name: ${app1.name}
          |    role: frontend
          |spec:
          |  replicas: 1
          |  selector:
          |    matchLabels:
          |      name: app-one
          |      role: frontend
          |  template:
          |    metadata:
          |      labels:
          |        name: app-one
          |        role: frontend
          |    spec:
          |      containers:
          |      - name: app
          |        image: image-app-one
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", frontend.name, connApp1.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: ${connApp1.name}
          |  namespace: ${frontend.name}
          |  labels:
          |    name: ${connApp1.name}
          |spec:
          |  podSelector:
          |    matchLabels:
          |      name: app-one
          |      role: frontend
          |  ingress:
          |  - from:
          |    - namespaceSelector:
          |        matchLabels:
          |          name: backend
          |          role: backend
          |  egress:
          |  - to:
          |    - podSelector:
          |        matchLabels:
          |          name: app-one
          |          role: frontend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "backend", connApp3.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: ${connApp3.name}
          |  namespace: backend
          |  labels:
          |    name: ${connApp3.name}
          |spec:
          |  podSelector:
          |    matchLabels:
          |      name: app-three
          |      role: backend
          |  ingress:
          |  - from:
          |    - namespaceSelector:
          |        matchLabels:
          |          name: frontend
          |          role: frontend
          |  egress:
          |  - to:
          |    - podSelector:
          |        matchLabels:
          |          name: app-three
          |          role: backend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "frontend", connApp1app2.name) -> matchJsonString(
          yamlToJson(s"""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: ${connApp1app2.name}
          |  namespace: frontend
          |  labels:
          |    name: ${connApp1app2.name}
          |spec:
          |  podSelector:
          |    matchLabels:
          |      name: app-one
          |      role: frontend
          |  ingress:
          |  - from:
          |    - podSelector:
          |        matchLabels:
          |          name: app-two
          |          role: frontend
          |  egress:
          |  - to:
          |    - podSelector:
          |        matchLabels:
          |          name: app-one
          |          role: frontend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        )
      )
  }

  it should "allow to express complex customized connections" in {
    import dsl.Expressions._
    import dsl.kubernetes.Connection.ops._

    val ns = Namespace(Name("foo") :: Nil)
    val app1 = Application(
      Name("app-one") :: Nil,
      Container(Name("app") :: Nil, image = "test") :: Nil
    )

    val conn1 = app1
      .communicatesWith(namespaceLabeled("role".is("backend")))
      .named("app1-backend")
      .patch { c =>
        c.copy(
          resourceSelector = SelectedApplications(
            c.resourceSelector.expressions,
            c.resourceSelector.protocols
          ),
          ingress = SelectedApplications(
            c.ingress.expressions,
            c.ingress.protocols
          ),
          egress = SelectedApplications(
            c.egress.expressions,
            c.egress.protocols
          )
        )
      }
      .patch { c =>
        Connection(
          labels = (Name("custom-name") :: UntypedLabel("tier", "top") :: Nil) ++ app1.labels.tail,
          c.resourceSelector,
          c.ingress,
          c.egress
        )
      }

    import kubernetes.skuber._
    import kubernetes.skuber.metadata._

    val resources = interpret(app1, ns) ++ interpret(conn1, ns)

    Ensure(resources.map(_.result))
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "custom-name") ->
          matchJsonString(yamlToJson(s"""
            |---
            |kind: NetworkPolicy
            |apiVersion: networking.k8s.io/v1
            |metadata:
            |  name: custom-name
            |  namespace: ${ns.name}
            |  labels:
            |    name: custom-name
            |    tier: top
            |spec:
            |  podSelector:
            |    matchLabels:
            |      name: app-one
            |  ingress:
            |  - from:
            |    - podSelector:
            |        matchLabels:
            |          role: backend
            |  egress:
            |  - to:
            |    - podSelector:
            |        matchLabels:
            |          name: app-one
            |  policyTypes:
            |  - Ingress
            |  - Egress
            |""".stripMargin))
      )
  }

  it should "allow for external connections" in {
    import dsl.Expressions._

    val ns = Namespace(Name("foo") :: Nil)
    val g1 = (
      Connection(
        name = "allow-all-ingress",
        resourceSelector = NoSelector,
        ingress = AllowSelector,
        egress = NoSelector
      ),
      Connection(
        name = "default-deny-ingress",
        resourceSelector = NoSelector,
        ingress = DenySelector,
        egress = NoSelector
      ),
      Connection(
        name = "allow-all-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = AllowSelector
      ),
      Connection(
        name = "default-deny-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = DenySelector
      ),
      Connection(
        name = "default-deny-all",
        resourceSelector = NoSelector,
        ingress = DenySelector,
        egress = DenySelector
      ),
      Connection(
        name = "allow-ingress-to-nginx",
        resourceSelector = SelectedApplications(
          expressions = Expressions("run" is "nginx"),
          protocols = Protocols.Any
        ),
        ingress = AllApplications,
        egress = NoSelector
      ),
      Connection(
        name = "allow-egress-to-nginx",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = SelectedApplications(
          expressions = Expressions("run" is "nginx"),
          protocols = Protocols.Any
        )
      ),
      Connection(
        name = "allow-dns-access",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = SelectedNamespaces(
          expressions = Expressions((Name("kube-system") :: Nil).map(n => Expressions.IsEqualExpression(n.key, n.value)): _*),
          protocols = Protocols(Protocol.Layers(l4 = UDP(53)))
        )
      ),
      Connection(
        name = "allow-kubernetes-access",
        resourceSelector = AllApplications,
        ingress = NoSelector,
        egress = SelectedNamespaces(
          expressions = Expressions((Name("default") :: Nil).map(n => Expressions.IsEqualExpression(n.key, n.value)): _*),
          protocols = Protocols(Protocol.Layers(l4 = TCP(443)))
        )
      ),
      Connection(
        name = "complex-ip-exclude",
        resourceSelector = SelectedApplications(
          expressions = Expressions("app" is "akka-cluster-demo"),
          protocols = Protocols.Any
        ),
        ingress = SelectedIPs(IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11"))),
        egress = SelectedIPs(IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11")))
      ),
      Connection(
        name = "egress-external-tcp-443",
        resourceSelector = SelectedApplications(
          expressions = Expressions("external-egress.monzo.com/443" is "true"),
          protocols = Protocols.Any
        ),
        ingress = NoSelector,
        egress = SelectedIPs(
          IP.Range("0.0.0.0/0")
            .except(
              IP.Range("0.0.0.0/8"),
              IP.Range("10.0.0.0/8"),
              IP.Range("172.16.0.0/12"),
              IP.Range("192.168.0.0/16")
            )
        ).ports(TCP(443))
      ),
      Connection(
        name = "default-deny-external-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = SelectedIPs(
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
      )
    )

    import kubernetes.skuber._
    import kubernetes.skuber.metadata._
    import kubernetes.skuber.metadata.InterpreterDerivation._

    val resources = interpret(ns) ++ interpret(g1, ns)

    Ensure(resources.map(_.result))
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-all-ingress") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-all-ingress
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-all-ingress
            |spec:
            |  podSelector: {}
            |  ingress:
            |  - {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-ingress") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-ingress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-deny-ingress
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-all-egress") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-all-egress
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-all-egress
            |spec:
            |  podSelector: {}
            |  egress:
            |  - {}
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-egress") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-egress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-deny-egress
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-all") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-all
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-deny-all
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Ingress
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-ingress-to-nginx") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-ingress-to-nginx
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-ingress-to-nginx
            |spec:
            |  podSelector:
            |    matchLabels:
            |      run: nginx
            |  ingress:
            |  - from:
            |    - podSelector: {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-egress-to-nginx") ->
          matchJsonString(yamlToJson(s"""
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-egress-to-nginx
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-egress-to-nginx
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Egress
            |  egress:
            |  - to:
            |    - podSelector:
            |        matchLabels:
            |          run: nginx
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-dns-access") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-dns-access
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-dns-access
            |spec:
            |  podSelector: {}
            |  egress:
            |  - to:
            |    - namespaceSelector:
            |        matchLabels:
            |          name: kube-system
            |    ports:
            |    - protocol: UDP
            |      port: 53
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-kubernetes-access") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: allow-kubernetes-access
            |  namespace: ${ns.name}
            |  labels:
            |    name: allow-kubernetes-access
            |spec:
            |  podSelector: {}
            |  egress:
            |  - ports:
            |    - port: 443
            |      protocol: TCP
            |    to:
            |    - namespaceSelector:
            |        matchLabels:
            |          name: default
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "complex-ip-exclude") ->
          matchJsonString(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: complex-ip-exclude
            |  namespace: ${ns.name}
            |  labels:
            |    name: complex-ip-exclude
            |spec:
            |  podSelector:
            |    matchLabels:
            |      app: akka-cluster-demo
            |  egress:
            |  - to:
            |    - ipBlock:
            |        cidr: 10.8.0.0/16
            |        except:
            |        - 10.8.2.11/32
            |  ingress:
            |  - from:
            |    - ipBlock:
            |        cidr: 10.8.0.0/16
            |        except:
            |        - 10.8.2.11/32
            |  policyTypes:
            |  - Ingress
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "egress-external-tcp-443") ->
          matchJsonString(yamlToJson(s"""
             |apiVersion: networking.k8s.io/v1
             |kind: NetworkPolicy
             |metadata:
             |  name: egress-external-tcp-443
             |  namespace: ${ns.name}
             |  labels:
             |    name: egress-external-tcp-443
             |spec:
             |  egress:
             |  - to:
             |    - ipBlock:
             |        cidr: 0.0.0.0/0 # allow the whole internet
             |        except: # private IP addresses
             |        - 0.0.0.0/8
             |        - 10.0.0.0/8
             |        - 172.16.0.0/12
             |        - 192.168.0.0/16
             |    ports:
             |      - port: 443
             |        protocol: TCP
             |  podSelector:
             |    matchLabels:
             |      external-egress.monzo.com/443: "true"
             |  policyTypes:
             |  - Egress
             |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-external-egress") ->
          matchJsonString(yamlToJson(s"""
             |apiVersion: networking.k8s.io/v1
             |kind: NetworkPolicy
             |metadata:
             |  name: default-deny-external-egress
             |  namespace: ${ns.name}
             |  labels:
             |    name: default-deny-external-egress
             |spec:
             |  podSelector: {}
             |  # traffic to external IPs will not be allowed from this namespace
             |  # ensure your internal IP range is allowed
             |  egress:
             |  - to:
             |    - ipBlock:
             |        cidr: 10.0.0.0/8
             |    - ipBlock:
             |       cidr: 172.16.0.0/12
             |    - ipBlock:
             |       cidr: 192.168.0.0/16
             |  policyTypes:
             |  - Egress
             |""".stripMargin))
      )
  }
}
