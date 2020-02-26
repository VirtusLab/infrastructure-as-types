package com.virtuslab

import com.stephenn.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import skuber.Namespace

class TestMainTest extends AnyFlatSpec with Matchers with JsonMatchers {

  import skuber.json.format._

  it should "serialize Namespace to JSON" in {
    val namespace = Namespace.forName("test")
    val json = Json.toJson(namespace)
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
