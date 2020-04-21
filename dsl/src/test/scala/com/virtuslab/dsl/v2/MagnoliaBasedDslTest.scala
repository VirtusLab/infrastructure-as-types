package com.virtuslab.dsl.v2

import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MagnoliaBasedDslTest extends AnyFlatSpec with Matchers with JsonMatchers {
  it should "work" in {
    import openApi.interpreters._ // needed
    import JValueTransformable._ // needed

    val namespace: Namespace = Namespace("foo")

    case class MyDef(
        superApp: Application = Application(name = "bar"),
        myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
        mySecret: Secret = Secret("config-foo", data = Map.empty))

    val myNs = MyDef()

    object JValueInterpreter extends InterpreterDerivation[JValue]
    val myDefInterpreter: JValueInterpreter.Typeclass[MyDef] = JValueInterpreter.gen[MyDef]
    val r = myDefInterpreter.interpret(myNs, namespace)
    println(r)
    println(r.map(_.toTransformable.transform))

  }
}
