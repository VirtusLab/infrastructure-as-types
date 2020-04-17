package com.virtuslab.dsl.v2

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpreterDerivationTest extends AnyFlatSpec with Matchers {

  it should "work" in {
    import openApi.interpreters._

    object Foo extends InterpreterDerivation

    val namespace: Namespace = Namespace("foo")

    case class MyDef(
        superApp: Application = Application(name = "bar"),
        myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
        mySecret: Secret = Secret("config-foo", data = Map.empty))

    val myNs = MyDef()

    println(Foo.gen[MyDef].interpret(myNs, namespace))
  }

}
