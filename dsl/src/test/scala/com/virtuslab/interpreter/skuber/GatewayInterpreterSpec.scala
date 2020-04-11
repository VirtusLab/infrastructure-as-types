package com.virtuslab.interpreter.skuber

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.HTTP.Host
import com.virtuslab.dsl._
import com.virtuslab.interpreter.InterpreterSpec
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters.yamlToJson

class GatewayInterpreterSpec extends InterpreterSpec with JsonMatchers {

  it should "allow to define a simple Ingress definition" in {
    implicit val (ds, ns) = builders[SkuberContext]()
    val gateway = Gateway(
      Labels(Name("external")),
      Protocols(
        Protocol.Layers(l7 = HTTP(host = Host("test.dsl.virtuslab.com")), l4 = TCP(Port(80)))
      )
    )

    val ingress = Skuber.gatewayInterpreter(Definition(gateway)).head.asJsValue

    ingress should matchJsonString(yamlToJson(s"""
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
