package com.virtuslab.dsl

import com.virtuslab.interpreter.skuber.Skuber
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import skuber.LabelSelector._

class LabelExpressionsTest extends AnyFlatSpec with Matchers {
  import Expressions._

  import scala.language.postfixOps

  it should "allow to express all Kubernetes label selector expressions" in {

    val existence = Expressions("live")
    val requirement = Skuber.expressions(existence)
    requirement shouldEqual Seq(ExistsRequirement("live"))

    val complex = Expressions(
      "partition" in ("customerA", "customerB"),
      "environment" is "qa",
      "environment" isNot "dev",
      "live" doesNotExist
    )
    val requirements = Skuber.expressions(complex)
    requirements shouldEqual Seq(
      InRequirement("partition", List("customerA", "customerB")),
      IsEqualRequirement("environment", "qa"),
      IsNotEqualRequirement("environment", "dev"),
      NotExistsRequirement("live")
    )
  }
}
