package com.virtuslab.dsl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LabelExpressionsTest extends AnyFlatSpec with Matchers {
  import LabelExpressions._

  it should "allow to express all Kubernetes label selector expressions" in {
//    LabelExpressions("live" doesNotExist)
//    LabelExpressions("partition"in ("customerA, customerB"), "environment" is "qa", "live" doesNotExist)
//    LabelExpressions("partition" in ("customerA", "customerB"), "environment" isNot "qa")
  }
}
