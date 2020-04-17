package com.virtuslab.dsl.v2

import org.json4s.JsonAST.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TransformerDerivationTest extends AnyFlatSpec with Matchers {

  it should "work" in {
    import com.virtuslab.kubernetes.client.openapi.model._
    import com.virtuslab.dsl.v2.openApi.json4sSerializers._
    object Foo extends TransformerDerivation[JValue]

    Foo.gen[(Service, Deployment)]
  }

}
