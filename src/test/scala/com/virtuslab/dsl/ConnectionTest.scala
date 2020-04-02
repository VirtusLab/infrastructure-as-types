package com.virtuslab.dsl

import com.virtuslab.dsl.Connection.ConnectionDefinition
import com.virtuslab.dsl.interpreter.{ InterpreterSpec, SystemInterpreter }
import com.virtuslab.internal.{ ShortMeta, SkuberConverter }
import com.virtuslab.scalatest.yaml.Converters.yamlToJson

class ConnectionTest extends InterpreterSpec {
  it should "allow to express connections between two namespaces" in {

    case class RoleLabel(value: String) extends Label {
      override val key: String = "role"
    }

    implicit val ds: SystemBuilder = DistributedSystem.ref(generateSystemName()).builder

    val frontendRoleLabel = RoleLabel("frontend")
    val frontendNsRef = Namespace.ref(Labels(Name("frontend"), frontendRoleLabel))

    val backendRoleLabel = RoleLabel("backend")
    val backendNsRef = Namespace.ref(Labels(Name("backend"), backendRoleLabel))

    info(s"system: ${ds.name}, namespaces: ${frontendNsRef.name}, ${backendNsRef.name}")

    val app3 = Application.ref(Labels(Name("app-three"), backendRoleLabel), "image-app-three")

    backendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app3
        )

        connections(
          app3 communicatesWith frontendNsRef
        )
      }

    val app1 = Application.ref(Labels(Name("app-one"), frontendRoleLabel), "image-app-one", ports = Networked.Port(9090) :: Nil)
    val app2 = Application.ref(Labels(Name("app-two"), frontendRoleLabel), "image-app-two", ports = Networked.Port(9090) :: Nil)

    frontendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app1,
          app2
        )

        connections(
          app1.communicatesWith(app2),
          app1.communicatesWith(backendNsRef)
        )
      }

    val resources = SkuberConverter(SystemInterpreter.of(ds)).toMetaAndJsValue

    Ensure(resources)
      .contain(
        ShortMeta("v1", "Namespace", "default", "frontend") -> yamlToJson("""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: frontend
          |  labels:
          |    name: frontend
          |    role: frontend
          |""".stripMargin),
        ShortMeta("v1", "Namespace", "default", "backend") -> yamlToJson("""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: backend
          |  labels:
          |    name: backend
          |    role: backend
          |""".stripMargin),
        ShortMeta("v1", "Service", "frontend", "app-one") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("v1", "Service", "frontend", "app-two") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("v1", "Service", "backend", "app-three") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("apps/v1", "Deployment", "backend", "app-three") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("apps/v1", "Deployment", "frontend", "app-two") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("apps/v1", "Deployment", "frontend", "app-one") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", "frontend", "app-one-backend-app-one") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", "backend", "app-three-frontend-app-three") -> yamlToJson("""
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
          |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", "frontend", "app-one-app-two-app-one") -> yamlToJson("""
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
  }

  it should "allow to express complex customized connections" in {
    implicit val (ds, ns) = builders()

    import ds._
    import ns._
    import Expressions._

    val app1 = Application.ref(Labels(Name("app-one")), image = "test").define

    val conn1 = app1
      .communicatesWith(namespaceLabeled("role".is("backend")))
      .transform({ c =>
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
      })
      .define({ c =>
        ConnectionDefinition(
          implicitly[NamespaceBuilder].namespace,
          Labels(
            Name("custom-name"),
            UntypedLabel("tier", "top") +: app1.labels.tail.toSeq: _*
          ),
          c.resourceSelector,
          c.ingress,
          c.egress
        )
      })

    applications(app1)
    connections(conn1)
    namespaces(ns)

    val resources = SkuberConverter(SystemInterpreter.of(systemBuilder)).toMetaAndJsValue

    Ensure(resources)
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "custom-name") ->
          yamlToJson(s"""
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
            |""".stripMargin)
      )
  }

  it should "allow for external connections" in {
    implicit val (ds, ns) = builders()

    import ds._
    import ns._
    import Expressions._

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
          protocols = AllProtocols
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
          protocols = AllProtocols
        )
      ),
      Connection(
        name = "allow-dns-access",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = SelectedNamespaces(
          expressions = Labels(Name("kube-system")),
          protocols = Protocols(UDP(53))
        )
      ),
      Connection(
        name = "allow-kubernetes-access",
        resourceSelector = AllApplications,
        ingress = NoSelector,
        egress = SelectedNamespaces(
          expressions = Labels(Name("default")),
          protocols = Protocols(TCP(443))
        )
      ),
      Connection(
        name = "complex-ip-exclude",
        resourceSelector = SelectedApplications(
          expressions = Expressions("app" is "akka-cluster-demo"),
          protocols = AllProtocols
        ),
        ingress = SelectedIPs(IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11"))),
        egress = SelectedIPs(IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11")))
      )
    )

    namespaces(ns)

    val resources = SkuberConverter(SystemInterpreter.of(systemBuilder)).toMetaAndJsValue

    Ensure(resources)
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-all-ingress") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-ingress") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-all-egress") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-egress") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-all") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-ingress-to-nginx") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-egress-to-nginx") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-dns-access") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-kubernetes-access") ->
          yamlToJson(s"""
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
            |""".stripMargin),
        ShortMeta("networking.k8s.io/v1", "NetworkPolicy", ns.name, "complex-ip-exclude") ->
          yamlToJson(s"""
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
            |""".stripMargin)
      )
  }
}
