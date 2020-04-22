package com.virtuslab.iat.kubernetes

import com.virtuslab.dsl.{ Labels, Name }
import com.virtuslab.iat.dsl.{ Application, Configuration, Namespace, Secret }
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.json.json4s.jackson.JsonMethods
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpreterDerivationTest extends AnyFlatSpec with Matchers with EnsureMatchers {

  it should "derive a one level nested interpreter" in {
    import openApi._
    import openApi.json4s._

    case class Group1(
        superApp: Application = Application(name = "bar"),
        myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
        mySecret: Secret = Secret("config-foo", data = Map.empty))

    val group1 = Group1()
    val namespace1: Namespace = Namespace("foo", Labels(Name("foo")))

    val myDefInterpreter = Interpreter.gen[Group1]
    val r = myDefInterpreter.interpret(group1, namespace1)

    val js = r.map(_.transform).map(JsonMethods.pretty)
    js should contain theSameElementsAs List(
      """|{
         |  "metadata" : {
         |    "name" : "bar",
         |    "namespace" : "foo"
         |  },
         |  "spec" : { }
         |}""".stripMargin,
      """|{
         |  "metadata" : {
         |    "name" : "bar",
         |    "namespace" : "foo"
         |  },
         |  "spec" : {
         |    "template" : {
         |      "spec" : {
         |        "containers" : [ {
         |          "name" : "bar"
         |        } ]
         |      }
         |    }
         |  }
         |}""".stripMargin,
      """|{
         |  "data" : { },
         |  "metadata" : {
         |    "name" : "config-foo",
         |    "namespace" : "foo"
         |  }
         |}""".stripMargin,
      """|{
         |  "data" : { },
         |  "metadata" : {
         |    "name" : "config-foo",
         |    "namespace" : "foo"
         |  }
         |}""".stripMargin
    )
  }
}
