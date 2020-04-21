package com.virtuslab.dsl.v2

import com.virtuslab.dsl.v2.Transformable.Transformer

import scala.annotation.implicitNotFound

sealed trait Support[P, R] {
  def transform: R = transformable.transform
  protected def transformable: Transformable[P, R] // available at construction
}

object Support {
  def apply[P, R](product: P)(implicit transformer: Transformer[P, R]): Support[P, R] = {
    new Support[P, R] {
      override def transformable: Transformable[P, R] = transformer(product)
    }
  }
}

@implicitNotFound("implicit for Transformable[P=${P}, R=${R} not found")
trait Transformable[P, R] {
  def transform: R
}

object Transformable {
  type Transformer[A, B] = A => Transformable[A, B]
}

//trait Transformer[A, B] extends (A => Transformable[A, B])

trait PlayJsonTransformable {
  import play.api.libs.json.{ JsValue, Json, Writes }

  implicit def transformer[P: Writes]: Transformer[P, JsValue] =
    p =>
      new Transformable[P, JsValue] {
        def transform: JsValue = Json.toJson(p)
      }
}

trait JValueTransformable {
  import org.json4s.JValue
  import com.virtuslab.json.json4s.jackson.JsonMethods

  implicit def transformer[P /*: Writer*/ ]: Transformer[P, JValue] =
    p =>
      new Transformable[P, JValue] {
        def transform: JValue = JsonMethods.asJValue /*[P]*/ (p)
      }
}
