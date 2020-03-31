package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Namespace
import play.api.libs.json.Json

class NamespaceInterpreterSpec extends InterpreterSpec {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in new Builders {
    val namespace = Namespace.ref("test").inNamespace(identity)
    val resource = NamespaceInterpreter(namespace)

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
