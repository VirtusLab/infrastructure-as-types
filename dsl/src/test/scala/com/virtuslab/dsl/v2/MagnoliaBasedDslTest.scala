package com.virtuslab.dsl.v2

import com.virtuslab.kubernetes.client.openapi.model.{ ConfigMap, Deployment, Service }
import com.virtuslab.scalatest.json4s.jackson.JsonMatchers
import org.json4s.JsonAST.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MagnoliaBasedDslTest extends AnyFlatSpec with Matchers with JsonMatchers {
  it should "work" in {
    import openApi.interpreters._
    import openApi.json4sSerializers._
    import com.virtuslab.kubernetes.client.openapi.model

    val namespace: Namespace = Namespace("foo")

    case class MyDef(
        superApp: Application = Application(name = "bar"),
        myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
        mySecret: Secret = Secret("config-foo", data = Map.empty))

    val myNs = MyDef()

    object CustomInterpreter extends InterpreterDerivation
    implicit val myDefInterpreter: Interpreter[MyDef] = CustomInterpreter.gen[MyDef]
    println(myDefInterpreter.interpret(myNs, namespace))

    object CustomTransformer extends TransformerDerivation[JValue]
    val aaa: Transformer[((Service, Deployment), ConfigMap, model.Secret), JValue] =
      CustomTransformer.gen[((Service, Deployment), ConfigMap, model.Secret)]

    aaa.apply(myDefInterpreter.interpret(myNs, namespace))

    //Materializer[MyDef].materialize[JValue](myNs, namespace)(myDefInterpreter, ???)
  }
}
