package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.json.playjson.Yaml
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.{ JsValue, Json, Writes }

trait PlayJsonProcessors {
  def asJsValue[T: Writes](o: T): JsValue = Json.toJson(o)
  def asJsonString[T: Writes](o: T): String = Json.prettyPrint(asJsValue(o))
  def asYamlString[T: Writes](o: T): String = Yaml.prettyPrint(asJsValue(o))

  def asMetadata[T <: Base](o: T): Metadata = Metadata(o.apiVersion, o.kind, o.ns, o.name)
  def asMetaJsValue[T <: Base: Writes](o: T): (Metadata, JsValue) = asMetadata(o) -> asJsValue(o)

  import com.virtuslab.iat.scala.ops._

  implicit class BaseOps[A <: Base: Writes](a: A) {
    def asJsValues: List[JsValue] = asJsValue(a) :: Nil
    def asMetaJsValues: List[(Metadata, JsValue)] = asMetaJsValue(a) :: Nil
  }

  implicit class BaseOps2[A1 <: Base: Writes, A2 <: Base: Writes](t: (A1, A2)) {
    def asJsValues: List[JsValue] = t.map(_.asJsValues, _.asJsValues).reduce(_ ++ _)
    def asMetaJsValues: List[(Metadata, JsValue)] = t.map(_.asMetaJsValues, _.asMetaJsValues).reduce(_ ++ _)
  }
}
