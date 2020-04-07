package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.HTTP.Host
import com.virtuslab.dsl.{ Gateway, HTTP, Labels, Name, Port, Protocol, Protocols, TCP }
import com.virtuslab.scalatest.yaml.Converters.yamlToJson
import play.api.libs.json.Json
import skuber.json.ext.format._

class GatewayInterpreterSpec extends InterpreterSpec {
  it should "allow to define a simple Ingress definition" in {
    implicit val (ds, ns) = builders()
    val gateway = Gateway(
      Labels(Name("external")),
      Protocols(
        Protocol.Layers(l7 = HTTP(host = Host("test.dsl.virtuslab.com")), l4 = TCP(Port(80)))
      )
    )

    val ingress = new GatewayInterpreter()(gateway)

    Json.toJson(ingress) should matchJsonString(yamlToJson(s"""
        |apiVersion: networking.k8s.io/v1beta1
        |kind: Ingress
        |metadata:
        |  name: external
        |  namespace: ${ns.name}
        |  labels:
        |    name: external
        |spec:
        |  rules:
        |  - host: test.dsl.virtuslab.com
        |    http:
        |      paths:
        |      - path: /
        |        backend:
        |          serviceName: ???
        |          servicePort: 80
        |""".stripMargin))
  }
}
