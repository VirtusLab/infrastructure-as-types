package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.HTTP.Host
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.kubernetes.{ Gateway, Namespace }
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.json.json4s.jackson.JsonMethods
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes
import com.virtuslab.iat.test.EnsureMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberGatewayInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  it should "allow to define a simple Ingress definition" in {
    val ns = Namespace(Name("foo") :: Nil)
    val gateway = Gateway(
      Name("external") :: Nil,
      Protocols(
        Protocol.Layers(l7 = HTTP(host = Host("test.dsl.virtuslab.com")), l4 = TCP(Port(80)))
      )
    )

    import kubernetes.skuber.playjson._
    import skuber.json.ext.format._

    val ingress = gateway.interpret(ns).asJsValues.head

    ingress.should(matchJsonString(yamlToJson(s"""
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
        |""".stripMargin)))
  }
}
