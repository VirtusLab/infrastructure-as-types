package com.virtuslab.iast

import com.virtuslab.dsl.{ Labels, Name }
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.materializer.openapi.Metadata
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SystemTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {
  object system extends DistributedGraph with KubernetesModel with JValueTarget with Implicits

  import system.implicits._

  it should "serialize Namespace to JSON" in {
    implicit val formats: Formats = JsonMethods.defaultFormats

    val ds = system.DistributedSystem(Labels(Name("theSystem")))
    val ns = system.Namespace(Labels(Name("theNamespace")), ds)
    val app1 = system.Application(Labels(Name("anApp")), ns)
    val conf1 = system.Configuration(Labels(Name("aConf")), app1)
    val gw1 = system.Gateway(Labels(Name("aGate")), ns)

    val namespaces = ds.namespaces
    val n: Iterable[system.Resource[model.Namespace]] = namespaces.map(system.NamespaceInterpreter)
    val (d: Iterable[system.Resource[model.Deployment]], s: Iterable[system.Resource[model.Service]]) =
      namespaces.flatMap(_.applications.toIterable).map(system.ApplicationInterpreter).unzip
    val j: Iterable[(Metadata, String)] =
      n.map(system.MetaStringMaterializer[model.Namespace]()) ++
        d.map(system.MetaStringMaterializer[model.Deployment]()) ++
        s.map(system.MetaStringMaterializer[model.Service]())

    println(namespaces.flatMap(_.applications.toIterable))
    println(namespaces.flatMap(_.applications.toIterable).map(_.configurations.toIterable))
    println(namespaces.flatMap(_.gateways.toIterable))

    Ensure(j)
      .contain(
        Metadata("v1", "Namespace", "", ns.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Namespace
            |apiVersion: v1
            |metadata:
            |  name: ${ns.name}
            |  labels:
            |    name: ${ns.name}
        """.stripMargin)),
        Metadata("apps/v1", "Deployment", ns.name, app1.name) -> matchJsonString(yamlToJson(s"""
            |---
            |kind: Deployment
            |apiVersion: apps/v1
            |metadata:
            |  name: ${app1.name}
            |  namespace: ${ns.name}
            |  labels:
            |    name: ${app1.name}
            |""".stripMargin)),
        Metadata("v1", "Service", ns.name, app1.name) -> matchJsonString(yamlToJson(s"""
             |kind: Service
             |apiVersion: v1
             |metadata:
             |  name: ${app1.name}
             |  namespace: ${ns.name}
             |  labels:
             |    name: ${app1.name}
             |""".stripMargin))
      )
  }
}
