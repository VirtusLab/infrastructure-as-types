package com.virtuslab.iast

import com.virtuslab.dsl.{ Labels, Name }
import com.virtuslab.json.Converters.yamlToJson
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.materializer.openapi.Metadata
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.should.Matchers

class SystemTest extends AnyFlatSpec with Matchers with JsonMatchers {
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

  case class Ensure[M, A](resources: Map[M, A]) {
    def ignore(p: M => Boolean): Ensure[M, A] = {
      resources.filter(e => p(e._1)).foreach {
        case (meta, _) => info(s"ignoring $meta")
      }
      Ensure(resources.filterNot(e => p(e._1)))
    }
    def contain(cases: (M, Matcher[A])*): Unit = contain(cases.toMap)

    def contain(cases: Map[M, Matcher[A]]): Unit = {
      zipper(resources, cases) {
        case (_, Some(actual), Some(expected)) => actual.should(expected)
        case (meta, Some(_), None)             => fail(s"unexpected $meta (got the resource, but no test case)")
        case (meta, None, Some(_))             => fail(s"unexpected $meta test case (got the test case but no resource)")
        case (meta, None, None)                => fail(s"unexpected $meta -> None/None, this should never happen")
      }
    }
  }
  object Ensure {
    def apply[M, A](resources: Iterable[(M, A)]): Ensure[M, A] = Ensure(resources.toMap)
  }

  def zipper[A, B, C, D](map1: Map[A, B], map2: Map[A, C])(f: (A, Option[B], Option[C]) => D): Map[A, D] = {
    (for (key <- map1.keys ++ map2.keys) yield key -> f(key, map1.get(key), map2.get(key))).toMap
  }
}
