package com.virtuslab.dsl.v2

import com.virtuslab.json.json4s.jackson.JsonMethods
import org.json4s.{ JValue }
import play.api.libs.json.{ JsValue, Json, Writes }

import scala.annotation.implicitNotFound

@implicitNotFound("implicit for Support[P=${P}, R=${R} not found")
trait Support[P, R] {
  def toTransformable: Transformable[P, R]
}

object Support {
  def apply[P, R](p: P)(implicit t: Transformable.Transformer[P, R]): Support[P, R] = {
    new Support[P, R] {
      override def toTransformable: Transformable[P, R] = t.apply(p)
    }
  }
}

trait Transformable[P, R] {
  def transform: R
}

object Transformable {
  type Transformer[A, B] = A => Transformable[A, B]
}

object PlayJsonTransformable extends PlayJsonTransformable
object JValueTransformable extends JValueTransformable

trait PlayJsonTransformable {
  implicit def toTransformer[P: Writes]: Transformable.Transformer[P, JsValue] =
    p =>
      new Transformable[P, JsValue] {
        def transform: JsValue = Json.toJson(p)
      }
}

trait JValueTransformable {
  implicit def toTransformer[P /*: Writer*/ ]: Transformable.Transformer[P, JValue] =
    p =>
      new Transformable[P, JValue] {
        def transform: JValue = {
          println(p)
          println(p.getClass)
          JsonMethods.asJValue /*[P]*/ (p)
        }
      }
}
