package com.virtuslab.interpreter

import com.virtuslab.dsl.{ DistributedSystem, Namespace, NamespaceBuilder, SystemBuilder }
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.materializer.skuber.ShortMeta
import org.json4s.Formats
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.should.Matchers

import scala.util.Random

abstract class InterpreterSpec extends AnyFlatSpec with Matchers {
  implicit val formats: Formats = JsonMethods.defaultFormats

  def builders[Ctx <: Context](
      names: (String, String) = generateNames()
    )(implicit
      ctx: Ctx
    ): (SystemBuilder[Ctx], NamespaceBuilder[Ctx]) = {
    val (systemName, namespaceName) = names
    info(s"system: $systemName, namespace: $namespaceName, context: ${ctx.getClass.getSimpleName}")

    implicit val systemBuilder: SystemBuilder[Ctx] = DistributedSystem(systemName).builder
    val namespaceBuilder: NamespaceBuilder[Ctx] = Namespace(namespaceName).builder
    (systemBuilder, namespaceBuilder)
  }

  def generateNames(maxRandomSuffix: Int = 5): (String, String) =
    (generateSystemName(maxRandomSuffix), generateNamespaceName(maxRandomSuffix))
  def generateSystemName(maxRandomSuffix: Int = 5): String =
    generatePrefixedName("system", maxRandomSuffix)
  def generateNamespaceName(maxRandomSuffix: Int = 5): String =
    generatePrefixedName("namespace", maxRandomSuffix)
  def generatePrefixedName(prefix: String, maxRandomSuffix: Int = 5) =
    s"$prefix-${Random.alphanumeric.take(maxRandomSuffix).mkString}"

  case class Ensure[A](resources: Map[ShortMeta, A]) {
    def ignore(p: ShortMeta => Boolean): Ensure[A] = {
      resources.filter(e => p(e._1)).foreach {
        case (meta, _) => info(s"ignoring $meta")
      }
      Ensure(resources.filterNot(e => p(e._1)))
    }
    def contain(cases: (ShortMeta, Matcher[A])*): Unit = contain(cases.toMap)

    def contain(cases: Map[ShortMeta, Matcher[A]]): Unit = {
      zipper(resources, cases) {
        case (_, Some(actual), Some(expected)) => actual.should(expected)
        case (meta, Some(_), None)             => fail(s"unexpected $meta (got the resource, but no test case)")
        case (meta, None, Some(_))             => fail(s"unexpected $meta test case (got the test case but no resource)")
        case (meta, None, None)                => fail(s"unexpected $meta -> None/None, this should never happen")
      }
    }
  }
  object Ensure {
    def apply[A](resources: Iterable[(ShortMeta, A)]): Ensure[A] = Ensure(resources.toMap)
  }

  def zipper[A, B, C, D](map1: Map[A, B], map2: Map[A, C])(f: (A, Option[B], Option[C]) => D): Map[A, D] = {
    (for (key <- map1.keys ++ map2.keys) yield key -> f(key, map1.get(key), map2.get(key))).toMap
  }
}
