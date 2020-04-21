package com.virtuslab.dsl.v2

import org.json4s.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpreterDerivationTest extends AnyFlatSpec with Matchers {

  it should "work" in {
    import openApi.interpreters._
    import JValueTransformable._
    object JValueInterpreter extends InterpreterDerivation[JValue]

    val namespace: Namespace = Namespace("foo")

    case class MyDef(
        superApp: Application = Application(name = "bar"),
        myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
        mySecret: Secret = Secret("config-foo", data = Map.empty))

    val myNs = MyDef()

    val myDefInterpreter = JValueInterpreter.gen[MyDef]
    val r = myDefInterpreter.interpret(myNs, namespace)
    println(r)
  }

}
