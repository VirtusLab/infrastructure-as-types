package com.virtuslab.materializer.skuber

import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters
import com.virtuslab.materializer
import org.json4s.JValue
import play.api.libs.json.{ Format, JsValue }
import skuber.ResourceDefinition

case class Metadata(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}

case class Resource[A <: SkuberContext#Base: Format: ResourceDefinition](obj: A) extends materializer.Resource[SkuberContext, A] {
  def format: Format[A] = implicitly[Format[A]]
  def definition: ResourceDefinition[A] = implicitly[ResourceDefinition[A]]
  def asJsValue: JsValue = format.writes(obj)

  override def weak: SkuberContext#Interpretation = this.asInstanceOf[SkuberContext#Interpretation]
  override def meta: SkuberContext#Meta = Metadata(obj.apiVersion, obj.kind, obj.ns, obj.name)
  override def asJValue: JValue = Converters.toJson4s(asJsValue)
}
