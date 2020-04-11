package com.virtuslab.exporter.skuber

import play.api.libs.json.{ Format, JsValue }
import skuber.{ ObjectResource, ResourceDefinition }

case class Resource[A <: ObjectResource: Format: ResourceDefinition](obj: A) {
  def format: Format[A] = implicitly[Format[A]]
  def definition: ResourceDefinition[A] = implicitly[ResourceDefinition[A]]
  def asJsValue: JsValue = format.writes(obj)
}

object Resource {
  def weak[A <: ObjectResource: Format: ResourceDefinition](obj: A): Resource[ObjectResource] =
    Resource(obj).asInstanceOf[Resource[ObjectResource]]
}
