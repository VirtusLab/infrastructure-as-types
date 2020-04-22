package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.materializer.skuber.{ Exporter, Metadata }

class ConnectionTest extends InterpreterSpec[SkuberContext] with JsonMatchers {

  it should "allow to express connections between two namespaces" in {

    case class RoleLabel(value: String) extends Label {
      override val key: String = "role"
    }

    implicit val ds: SystemBuilder[SkuberContext] = DistributedSystem(generateSystemName()).builder

    val frontendRoleLabel = RoleLabel("frontend")
    val frontend = Namespace(Labels(Name("frontend"), frontendRoleLabel))

    val backendRoleLabel = RoleLabel("backend")
    val backend = Namespace(Labels(Name("backend"), backendRoleLabel))

    info(s"system: ${ds.name}, namespaces: ${frontend.name}, ${backend.name}")

    val app3 = Application(Labels(Name("app-three"), backendRoleLabel), "image-app-three")

    backend
      .inNamespace { implicit ns: NamespaceBuilder[SkuberContext] =>
        import ns._

        applications(
          app3
        )

        connections(
          app3 communicatesWith frontend
        )
      }

    val app1 = Application(Labels(Name("app-one"), frontendRoleLabel), "image-app-one", ports = Port(9090) :: Nil)
    val app2 = Application(Labels(Name("app-two"), frontendRoleLabel), "image-app-two", ports = Port(9090) :: Nil)

    frontend
      .inNamespace { implicit ns: NamespaceBuilder[SkuberContext] =>
        import ns._

        applications(
          app1,
          app2
        )

        connections(
          app1.communicatesWith(app2),
          app1.communicatesWith(backend)
        )
      }

    val resources = ds.build().interpret().map(Exporter.metaAndJsValue)

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", "frontend") -> matchJsonString(yamlToJson("""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: frontend
          |  labels:
          |    name: frontend
          |    role: frontend
          |""".stripMargin)),
        Metadata("v1", "Namespace", "default", "backend") -> matchJsonString(yamlToJson("""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: backend
          |  labels:
          |    name: backend
          |    role: backend
          |""".stripMargin)),
        Metadata("v1", "Service", "frontend", "app-one") -> matchJsonString(yamlToJson("""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: app-one
          |  namespace: frontend
          |  labels:
          |    name: app-one
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
        Metadata("v1", "Service", "frontend", "app-two") -> matchJsonString(yamlToJson("""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: app-two
          |  namespace: frontend
          |  labels:
          |    name: app-two
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
        Metadata("v1", "Service", "backend", "app-three") -> matchJsonString(yamlToJson("""
          |---
          |kind: Service
          |apiVersion: v1
          |metadata:
          |  name: app-three
          |  namespace: backend
          |  labels:
          |    name: app-three
          |    role: backend
          |spec:
          |  selector:
          |    name: app-three
          |    role: backend
          |  type: ClusterIP
          |  sessionAffinity: None
          |""".stripMargin)),
        Metadata("apps/v1", "Deployment", "backend", "app-three") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: app-three
          |  namespace: backend
          |  labels:
          |    name: app-three
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
          |      - name: app-three
          |        image: image-app-three
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("apps/v1", "Deployment", "frontend", "app-two") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: app-two
          |  namespace: frontend
          |  labels:
          |    name: app-two
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
          |      - name: app-two
          |        image: image-app-two
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("apps/v1", "Deployment", "frontend", "app-one") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: Deployment
          |apiVersion: apps/v1
          |metadata:
          |  name: app-one
          |  namespace: frontend
          |  labels:
          |    name: app-one
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
          |      - name: app-one
          |        image: image-app-one
          |        ports:
          |        - containerPort: 9090
          |          protocol: TCP
          |        imagePullPolicy: IfNotPresent
          |      restartPolicy: Always
          |      dnsPolicy: ClusterFirst
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "frontend", "app-one-backend-app-one") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: app-one-backend-app-one
          |  namespace: frontend
          |  labels:
          |    name: app-one-backend-app-one
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
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "backend", "app-three-frontend-app-three") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: app-three-frontend-app-three
          |  namespace: backend
          |  labels:
          |    name: app-three-frontend-app-three
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
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "frontend", "app-one-app-two-app-one") -> matchJsonString(
          yamlToJson("""
          |---
          |kind: NetworkPolicy
          |apiVersion: networking.k8s.io/v1
          |metadata:
          |  name: app-one-app-two-app-one
          |  namespace: frontend
          |  labels:
          |    name: app-one-app-two-app-one
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
    implicit val (ds, ns) = builders()

    import Expressions._
    import ds._
    import ns._

    val app1 = Application(Labels(Name("app-one")), image = "test")

    val conn1 = app1
      .communicatesWith(namespaceLabeled("role".is("backend")))
      .transform { c =>
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
      .transform { c =>
        Connection(
          Labels(
            Name("custom-name"),
            UntypedLabel("tier", "top") +: app1.labels.tail.toSeq: _*
          ),
          c.resourceSelector,
          c.ingress,
          c.egress
        )
      }

    applications(app1)
    connections(conn1)
    namespaces(ns)

    val resources = ds.build().interpret().map(Exporter.metaAndJsValue)

    Ensure(resources)
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
    implicit val (ds, ns) = builders()

    import Expressions._
    import ds._
    import ns._

    connections(
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
          expressions = Labels(Name("kube-system")),
          protocols = Protocols(Protocol.Layers(l4 = UDP(53)))
        )
      ),
      Connection(
        name = "allow-kubernetes-access",
        resourceSelector = AllApplications,
        ingress = NoSelector,
        egress = SelectedNamespaces(
          expressions = Labels(Name("default")),
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

    namespaces(ns)

    val resources = ds.build().interpret().map(Exporter.metaAndJsValue)

    Ensure(resources)
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
