package com.virtuslab.dsl.interpreter

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.{ Configuration, DistributedSystem, Labels, Name, Namespace, NamespaceBuilder, SystemBuilder, UntypedLabel }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import skuber.json.format._

class ConfigurationInterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  it should "produce empty config map without labels" in {
    implicit val systemBuilder = DistributedSystem.ref("foo").builder
    implicit val namespaceBuilder = Namespace.ref("bar").builder

    val configuration = Configuration(labels = Labels(Name("foo"), UntypedLabel("name", "bazz")), data = Map.empty)

    val skuberConfig = ConfigurationInterpreter(configuration)

    Json.toJson(skuberConfig) should matchJsonString("""
        |{
        |  "kind" : "ConfigMap",
        |  "apiVersion" : "v1",
        |  "metadata" : {
        |    "name" : "foo",
        |    "namespace" : "bar",
        |    "labels" : {
        |      "name" : "foo",
        |      "name" : "bazz"
        |    }
        |  }
        |}
        |""".stripMargin)
  }
}
