package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ Name, Role, UntypedLabel }
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.kubernetes.dsl.{ NetworkPolicy, _ }
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberConnectionTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "allow to express connections between two namespaces" in {
    import iat.kubernetes.dsl.NetworkPolicy.ops._

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
        ports = TCP(9090) :: Nil
      ) :: Nil
    )
    val app2 = Application(
      Name("app-two") :: frontendRole :: Nil,
      Container(
        Name("app") :: Nil,
        image = "image-app-two",
        ports = TCP(9090) :: Nil
      ) :: Nil
    )
    val connApp1 = app1.communicatesWith(backend).named("app1-backend-app1")
    val connApp1app2 = app1.communicatesWith(app2).named("app1-app2-app1")

    import iat.skuber.playjson._
    import skuber.json.format._

    val resources =
      List(backend, frontend).flatMap(_.interpret.asMetaJsValues) ++
        app1.interpretWith(frontend).asMetaJsValues ++
        app2.interpretWith(frontend).asMetaJsValues ++
        app3.interpretWith(backend).asMetaJsValues ++
        connApp1.interpretWith(frontend).asMetaJsValues ++
        connApp3.interpretWith(backend).asMetaJsValues ++
        connApp1app2.interpretWith(frontend).asMetaJsValues

    Ensure(resources)
      .contain(
        Metadata("v1", "Namespace", "default", frontend.name) -> matchJson(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: ${frontend.name}
          |  labels:
          |    name: ${frontend.name}
          |    role: frontend
          |""".stripMargin)),
        Metadata("v1", "Namespace", "default", backend.name) -> matchJson(yamlToJson(s"""
          |---
          |kind: Namespace
          |apiVersion: v1
          |metadata:
          |  name: ${backend.name}
          |  labels:
          |    name: ${backend.name}
          |    role: backend
          |""".stripMargin)),
        Metadata("v1", "Service", frontend.name, app1.name) -> matchJson(yamlToJson(s"""
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
        Metadata("v1", "Service", frontend.name, app2.name) -> matchJson(yamlToJson(s"""
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
        Metadata("v1", "Service", backend.name, app3.name) -> matchJson(yamlToJson(s"""
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
        Metadata("apps/v1", "Deployment", backend.name, app3.name) -> matchJson(
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
        Metadata("apps/v1", "Deployment", frontend.name, app2.name) -> matchJson(
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
        Metadata("apps/v1", "Deployment", frontend.name, app1.name) -> matchJson(
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
        Metadata("networking.k8s.io/v1", "NetworkPolicy", frontend.name, connApp1.name) -> matchJson(
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
          |  - ports:
          |    - port: 9090
          |      protocol: TCP
          |    from:
          |    - namespaceSelector:
          |        matchLabels:
          |          name: backend
          |          role: backend
          |  egress:
          |  - to:
          |    - namespaceSelector:
          |        matchLabels:
          |          name: backend
          |          role: backend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "backend", connApp3.name) -> matchJson(
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
          |    - namespaceSelector:
          |        matchLabels:
          |          name: frontend
          |          role: frontend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        ),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", "frontend", connApp1app2.name) -> matchJson(
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
          |  - ports:
          |    - port: 9090
          |      protocol: TCP
          |    from:
          |    - podSelector:
          |        matchLabels:
          |          name: app-two
          |          role: frontend
          |  egress:
          |  - ports:
          |    - port: 9090
          |      protocol: TCP
          |    to:
          |    - podSelector:
          |        matchLabels:
          |          name: app-two
          |          role: frontend
          |  policyTypes:
          |  - Ingress
          |  - Egress
          |""".stripMargin)
        )
      )
  }

  it should "allow to express complex customized connections" in {
    import iat.dsl.Expressions.ops._
    import iat.kubernetes.dsl.NetworkPolicy.ops._

    val ns = Namespace(Name("foo") :: Nil)
    val app1 = Application(
      Name("app-one") :: Nil,
      Container(Name("app") :: Nil, image = "test") :: Nil
    )

    val conn1 = app1
      .communicatesWith(
        Peer.withType[Namespace].withExpressions("role".is("backend"))
      )
      .named("app1-backend")
      .patch(
        _.copy(
          labels = (Name("custom-name") :: UntypedLabel("tier", "top") :: Nil) ++ app1.labels.tail
        )
      )

    import iat.skuber.playjson._
    import skuber.json.format._

    val resources =
      app1.interpretWith(ns).asMetaJsValues ++
        conn1.interpretWith(ns).asMetaJsValues

    Ensure(resources)
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "custom-name") ->
          matchJson(yamlToJson(s"""
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
            |    - namespaceSelector:
            |        matchLabels:
            |          role: backend
            |  egress:
            |  - to:
            |    - namespaceSelector:
            |        matchLabels:
            |          role: backend
            |  policyTypes:
            |  - Ingress
            |  - Egress
            |""".stripMargin))
      )
  }

  it should "allow for external connections" in {
    import iat.dsl.Expressions.ops._
    import iat.kubernetes.dsl.NetworkPolicy.ops._

    val ns = Namespace(Name("foo") :: Nil)

    val connNginxFromApps = Peer
      .withType[Application]
      .withExpressions("run" is "nginx")
      .communicatesWith(Peer.any)
      .ingressOnly
      .named("allow-ingress-to-nginx")

    val connAppsToNginx = Peer.any
      .communicatesWith(
        Peer.withType[Application].withExpressions("run" is "nginx")
      )
      .egressOnly
      .named("allow-egress-to-nginx")

    val connToCoreDns = Peer.any
      .communicatesWith(
        NetworkPolicy.peer.kubernetesDns
      )
      .egressOnly
      .named("allow-dns-access")

    val connToK8s = Peer.any
      .communicatesWith(
        NetworkPolicy.peer.kubernetesApi
      )
      .egressOnly
      .named("allow-kubernetes-access")

    val connBiWithExclude = Peer.any
      .withExpressions("app" is "akka-cluster-demo")
      .withIPs(
        IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11"))
      )
      .communicatesWith(
        Peer.any
          .withExpressions("app" is "akka-cluster-demo")
          .withIPs(
            IP.Range("10.8.0.0/16").except(IP.Address("10.8.2.11"))
          )
      )
      .named("complex-ip-exclude")

    val connExternal443 = Peer
      .withType[Application]
      .withExpressions("external-egress.monzo.com/443" is "true")
      .communicatesWith(
        Peer.any
          .withIPs(
            IP.Range("0.0.0.0/0")
              .except(
                IP.Range("0.0.0.0/8"),
                IP.Range("10.0.0.0/8"),
                IP.Range("172.16.0.0/12"),
                IP.Range("192.168.0.0/16")
              )
          )
          .withPorts(TCP(443))
      )
      .egressOnly
      .named("egress-external-tcp-443")

    import iat.skuber.playjson._
    import skuber.json.format._

    val resources =
      ns.interpret.asMetaJsValues ++ List(
        NetworkPolicy.default.allowAllIngress.interpretWith(ns),
        NetworkPolicy.default.denyAllIngress.interpretWith(ns),
        NetworkPolicy.default.allowAllEgress.interpretWith(ns),
        NetworkPolicy.default.denyAllEgress.interpretWith(ns),
        NetworkPolicy.default.denyAll.interpretWith(ns),
        NetworkPolicy.default.denyExternalEgress.interpretWith(ns),
        connNginxFromApps.interpretWith(ns),
        connAppsToNginx.interpretWith(ns),
        connToCoreDns.interpretWith(ns),
        connToK8s.interpretWith(ns),
        connBiWithExclude.interpretWith(ns),
        connExternal443.interpretWith(ns)
      ).flatMap(_.asMetaJsValues)

    Ensure(resources)
      .ignore(_.kind != "NetworkPolicy")
      .contain(
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-allow-all-ingress") ->
          matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-allow-all-ingress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-allow-all-ingress
            |spec:
            |  podSelector: {}
            |  ingress:
            |  - {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-all-ingress") ->
          matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-all-ingress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-deny-all-ingress
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-allow-all-egress") ->
          matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-allow-all-egress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-allow-all-egress
            |spec:
            |  podSelector: {}
            |  egress:
            |  - {}
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-all-egress") ->
          matchJson(yamlToJson(s"""
            |---
            |apiVersion: networking.k8s.io/v1
            |kind: NetworkPolicy
            |metadata:
            |  name: default-deny-all-egress
            |  namespace: ${ns.name}
            |  labels:
            |    name: default-deny-all-egress
            |spec:
            |  podSelector: {}
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "default-deny-all") ->
          matchJson(yamlToJson(s"""
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
          matchJson(yamlToJson(s"""
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
            |  - {}
            |  policyTypes:
            |  - Ingress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-egress-to-nginx") ->
          matchJson(yamlToJson(s"""
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
          matchJson(yamlToJson(s"""
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
            |    - protocol: TCP
            |      port: 53
            |  policyTypes:
            |  - Egress
            |""".stripMargin)),
        Metadata("networking.k8s.io/v1", "NetworkPolicy", ns.name, "allow-kubernetes-access") ->
          matchJson(yamlToJson(s"""
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
          matchJson(yamlToJson(s"""
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
          matchJson(yamlToJson(s"""
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
          matchJson(yamlToJson(s"""
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
