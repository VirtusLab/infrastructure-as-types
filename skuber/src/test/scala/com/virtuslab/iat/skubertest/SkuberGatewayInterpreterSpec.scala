package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.HTTP.Host
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.kubernetes.dsl.{ Gateway, Namespace }
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.skuber.yaml.Yaml.yamlToJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberGatewayInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "allow to define a simple Ingress definition" in {
    val ns = Namespace(Name("foo") :: Nil)
    val gateway = Gateway(
      Name("external") :: Nil,
      inputs = Protocols(
        Protocol.Layers(l7 = HTTP(host = Host("test.dsl.virtuslab.com")), l4 = TCP())
      ),
      outputs = Protocols(
        Protocol.Layers(l7 = HTTP(host = Host("app1.ns1")), l4 = TCP(Port(80)))
      )
    )

    import iat.skuber.playjson._
    import skuber.json.ext.format._

    val ingress = gateway.interpretWith(ns).asJsValues.head

    ingress.should(matchJson(yamlToJson(s"""
        |apiVersion: extensions/v1beta1
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
        |          serviceName: app1.ns1
        |          servicePort: 80
        |""".stripMargin)))
  }
}
