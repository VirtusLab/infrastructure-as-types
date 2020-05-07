package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Expressions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import skuber.LabelSelector._

class SkuberLabelExpressionsTest extends AnyFlatSpec with Matchers {
  import com.virtuslab.iat.dsl.Expressions._

  import scala.language.postfixOps

  it should "allow to express all Kubernetes label selector expressions" in {
    import iat.skuber.subinterpreter

    val existence = Expressions("live")
    val requirement = subinterpreter.expressions(existence)
    requirement shouldEqual Seq(ExistsRequirement("live"))

    val complex = Expressions(
      "partition".in("customerA", "customerB"),
      "environment".is("qa"),
      "environment".isNot("dev"),
      "live".doesNotExist
    )
    val requirements = subinterpreter.expressions(complex)
    requirements shouldEqual Seq(
      InRequirement("partition", List("customerA", "customerB")),
      IsEqualRequirement("environment", "qa"),
      IsNotEqualRequirement("environment", "dev"),
      NotExistsRequirement("live")
    )
  }
}
