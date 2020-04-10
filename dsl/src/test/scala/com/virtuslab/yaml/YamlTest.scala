package com.virtuslab.yaml

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class YamlTest extends AnyFlatSpec with Matchers {

  it should "parse YAML from a string and back" in {
    val input =
      """
        |apiVersion: networking.k8s.io/v1
        |kind: NetworkPolicy
        |metadata:
        |  name: default-deny-ingress
        |  namespace: advanced-policy-demo
        |spec:
        |  podSelector:
        |    matchLabels: {}
        |  policyTypes:
        |  - Ingress
        |""".stripMargin

    val expected =
      """---
        |apiVersion: "networking.k8s.io/v1"
        |kind: "NetworkPolicy"
        |metadata:
        |  name: "default-deny-ingress"
        |  namespace: "advanced-policy-demo"
        |spec:
        |  podSelector:
        |    matchLabels: {}
        |  policyTypes:
        |  - "Ingress"""".stripMargin

    val yaml = Yaml.parse(input)
    val output = Yaml.prettyPrint(yaml)

    output should equal(expected)
  }
}
