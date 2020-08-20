package com.virtuslab.iat.skuber

import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.iat.skuber.yaml.Yaml
import play.api.libs.json.{ JsValue, Json, Writes }
import skuber.ObjectResource

trait PlayJsonProcessors {
  import com.virtuslab.iat.scala.ops._

  def asJsValue[T: Writes](o: T): JsValue = Json.toJson(o)
  def asJsonString[T: Writes](o: T): String = Json.prettyPrint(asJsValue(o))
  def asYamlString[T: Writes](o: T): String = Yaml.prettyPrint(asJsValue(o))

  def asMetadata[T <: ObjectResource](o: T): Metadata = Metadata(o.apiVersion, o.kind, o.ns, o.name)
  def asMetaJsValue[T <: ObjectResource: Writes](o: T): (Metadata, JsValue) = asMetadata(o) -> asJsValue(o)

  implicit class ObjectResourceOps1[A <: ObjectResource: Writes](a: A) {
    def asJsValues: Seq[JsValue] = asJsValue(a) :: Nil
    def asMetaJsValues: Seq[(Metadata, JsValue)] = asMetaJsValue(a) :: Nil
  }

  implicit class ObjectResourceOps2[A1 <: ObjectResource: Writes, A2 <: ObjectResource: Writes](t: (A1, A2)) {
    def asJsValues: Seq[JsValue] = t.map(_.asJsValues, _.asJsValues).reduce(_ ++ _)
    def asMetaJsValues: Seq[(Metadata, JsValue)] = t.map(_.asMetaJsValues, _.asMetaJsValues).reduce(_ ++ _)
  }
}
