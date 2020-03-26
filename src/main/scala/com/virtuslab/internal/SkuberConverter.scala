package com.virtuslab.internal

import com.virtuslab.dsl.interpreter.SystemInterpreter
import play.api.libs.json.{ Format, JsValue, Json }
import skuber.{ ObjectResource, ResourceDefinition }

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String)

class SkuberConverter(interpreter: SystemInterpreter) {
  def toMetaAndJson: Seq[(ShortMeta, JsValue)] = {
    interpreter.resources.map { resource =>
      val r = resource.obj
      ShortMeta(r.apiVersion, r.kind, r.ns, r.name) -> Json.toJson(resource.obj)(resource.format)
    }
  }
}

object SkuberConverter {
  case class Resource[A <: ObjectResource: Format: ResourceDefinition](obj: A) {
    def format: Format[A] = implicitly[Format[A]]
    def definition: ResourceDefinition[A] = implicitly[ResourceDefinition[A]]
  }
  def apply(interpreter: SystemInterpreter): SkuberConverter = new SkuberConverter(interpreter)
}
