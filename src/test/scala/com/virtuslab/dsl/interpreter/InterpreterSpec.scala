package com.virtuslab.dsl.interpreter

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.{ DistributedSystem, Namespace }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Random

abstract class InterpreterSpec extends AnyFlatSpec with Matchers with JsonMatchers {
  trait Builders {
    final private val max = 5
    final val systemName = s"system-${Random.alphanumeric.take(max).mkString}"
    final val namespaceName = s"namespace-${Random.alphanumeric.take(max).mkString}"

    implicit val systemBuilder = DistributedSystem.ref(systemName).builder
    implicit val namespaceBuilder = Namespace.ref(namespaceName).builder
  }
}
