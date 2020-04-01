package com.virtuslab.dsl.interpreter

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.{ DistributedSystem, Namespace, NamespaceBuilder, SystemBuilder }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

abstract class InterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers {
  trait Builders {
    final private val max = 5
    final val systemName = s"system-${Random.alphanumeric.take(max).mkString}"
    final val namespaceName = s"namespace-${Random.alphanumeric.take(max).mkString}"

    implicit val systemBuilder: SystemBuilder = DistributedSystem.ref(systemName).builder
    implicit val namespaceBuilder: NamespaceBuilder = Namespace.ref(namespaceName).builder
  }

  def builders(names: => (String, String) = namesGenerator(5)): (SystemBuilder, NamespaceBuilder) = {
    val (systemName, namespaceName) = names

    implicit val systemBuilder: SystemBuilder = DistributedSystem.ref(systemName).builder
    val namespaceBuilder: NamespaceBuilder = Namespace.ref(namespaceName).builder
    (systemBuilder, namespaceBuilder)
  }

  def namesGenerator(maxRandomSuffix: Int): (String, String) = {
    val systemName = s"system-${Random.alphanumeric.take(maxRandomSuffix).mkString}"
    val namespaceName = s"namespace-${Random.alphanumeric.take(maxRandomSuffix).mkString}"
    (systemName, namespaceName)
  }
}
