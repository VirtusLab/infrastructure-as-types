package com.virtuslab.dsl.v2

import java.time.ZonedDateTime

import org.scalatest.FlatSpec
import play.api.libs.json._
import TypedAsFuck._

class TypedAsFuckTest extends FlatSpec {

  it should "fuck off" in {
    case class FUCKTHISSHIT(x: String)
    case class Yolo(a: String, b: Int, c: FUCKTHISSHIT)

    implicitly[Interpreter[Yolo, JsValue]].interpret(Yolo("x", 1, FUCKTHISSHIT("NOPE")), Namespace("chuj")).foreach {
      repr => println(repr.represent)
    }

  }


}
