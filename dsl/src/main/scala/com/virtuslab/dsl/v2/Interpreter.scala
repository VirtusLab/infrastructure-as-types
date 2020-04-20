package com.virtuslab.dsl.v2

import play.api.libs.json.{JsValue, Json, Writes}
import shapeless._

object TypedAsFuck {

  trait Representable[A, R] {
    def represent: R
  }

  import scala.annotation.implicitNotFound
  import scala.language.implicitConversions

  implicit def toRepresentable[A: Writes]: A => Representable[A, JsValue] = a => new Representable[A, JsValue] {
    def represent: JsValue = Json.toJson(a)
  }

  @implicitNotFound("You need more shit to convert from elements of ${A} to ${R}")
  trait Interpreter[A, R] {
    def interpret(obj: A, ns: Namespace): List[Representable[_, R]]
  }

  trait Support[T, R] {
    def toRepresentables(t: T): List[Representable[_, R]]
  }

  implicit def hnilSupport[R]: Support[HNil, R] = (_: HNil) => List.empty


  trait LowerFuckingImplicitScope {
    implicit def hconsSupport[R, H, T <: HList](implicit reprHead: H => Representable[H, R], tailSupport: Support[T, R]): Support[H :: T, R] =
      (t: H :: T) => List(reprHead(t.head)) ++ tailSupport.toRepresentables(t.tail)
  }

  object HigherFuckingImplicitScope extends LowerFuckingImplicitScope {
    implicit def hconsSupportHighPrio[R, H, T <: HList](implicit reprHead: H => Representable[H, R], tailSupport: Support[T, R]): Support[H :: T, R] =
      (t: H :: T) => List(reprHead(t.head)) ++ tailSupport.toRepresentables(t.tail)
  }

  implicit def genInterpreter[A, XD, R](implicit gen: Generic[A] { type Repr = XD }, support: Support[XD, R]): Interpreter[A, R] =
    (obj: A, ns: Namespace) => support.toRepresentables(gen.to(obj))


}