package com.virtuslab.dsl

import cats.data.NonEmptyList
import com.stephenn.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class NamespaceSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    val namespace = Namespace("test").inNamespace(
      ns =>
        NonEmptyList.one(new Namespaced {
          override def namespace: Namespace = ns
        })
    )
    val ns = new NamespaceInterpreter().apply(namespace)

    val json = Json.toJson(ns)
    json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"test"
  }
}
""")
  }

}
