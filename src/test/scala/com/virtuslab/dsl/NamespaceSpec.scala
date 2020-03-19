package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.interpreter.NamespaceInterpreter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class NamespaceSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    implicit val system: SystemBuilder = DistributedSystem("test").builder
    val namespace = Namespace.ref("test").inNamespace(identity)
    val resource = new NamespaceInterpreter().apply(namespace)

    val json = Json.toJson(resource)
    json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"test",
    "labels": {
      "name":"test"
    }
  }
}
""")
  }

}
