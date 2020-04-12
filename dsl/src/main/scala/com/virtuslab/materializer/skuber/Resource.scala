package com.virtuslab.materializer.skuber

import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.Converters
import com.virtuslab.materializer
import org.json4s.JValue
import play.api.libs.json.{ Format, JsValue }
import skuber.{ ObjectResource, ResourceDefinition }

case class Resource[A <: ObjectResource: Format: ResourceDefinition](obj: A) extends materializer.Resource[SkuberContext, A] {
  def format: Format[A] = implicitly[Format[A]]
  def definition: ResourceDefinition[A] = implicitly[ResourceDefinition[A]]
  def asJsValue: JsValue = format.writes(obj)

  override def weak: SkuberContext#Ret = this.asInstanceOf[SkuberContext#Ret]
  override def asJValue: JValue = Converters.toJson4s(asJsValue)
}
