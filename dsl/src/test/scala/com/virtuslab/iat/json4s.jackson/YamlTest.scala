package com.virtuslab.iat.json4s.jackson

import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.{ Formats, NoTypeHints }
import org.json4s.jackson.Serialization
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.virtuslab.iat.json.json4s.jackson.YamlMethods

class YamlTest extends AnyFlatSpec with Matchers with JsonMatchers {

  import YamlMethods._
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  it should "parse YAML from a string and back" in {
    val input =
      """apiVersion: networking.k8s.io/v1
        |kind: NetworkPolicy
        |metadata:
        |  name: default-deny-all-ingress
        |  namespace: advanced-policy-demo
        |spec:
        |  podSelector:
        |    matchLabels: {}
        |  policyTypes:
        |  - Ingress
        |""".stripMargin

    val expected = input
    val yaml = parse(input)
    val output = pretty(yaml)

    output should equal(expected)
  }
}
