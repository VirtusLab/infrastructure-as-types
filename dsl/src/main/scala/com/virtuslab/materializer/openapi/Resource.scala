package com.virtuslab.materializer.openapi

import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.materializer
import org.json4s.JValue

case class Resource[A <: OpenAPIContext#T](obj: A) extends materializer.Resource[OpenAPIContext, A] {
  override def weak: OpenAPIContext#Ret = this.asInstanceOf[OpenAPIContext#Ret]
  override def asJValue: JValue = JsonMethods.asJValue(obj)
}
