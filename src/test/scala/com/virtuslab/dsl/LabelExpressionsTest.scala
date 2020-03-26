package com.virtuslab.dsl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class LabelExpressionsTest extends AnyFlatSpec with Matchers {

  import Expressions._
  it should "allow to express all Kubernetes label selector expressions" in {
    Expressions("live") // FIXME
    Expressions("partition" in ("customerA, customerB"), "environment" is "qa", "live" doesNotExist)
    Expressions("partition" in ("customerA", "customerB"), "environment" isNot "qa")
  }
}
