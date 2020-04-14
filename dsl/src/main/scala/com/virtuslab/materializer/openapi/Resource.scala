package com.virtuslab.materializer.openapi

import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.json.json4s.jackson.JsonMethods
import com.virtuslab.materializer
import org.json4s.JValue

case class Metadata(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}

case class Resource[A <: OpenAPIContext#Base](meta: OpenAPIContext#Meta, obj: A)
  extends materializer.Resource[OpenAPIContext, A] {
  override def weak: OpenAPIContext#Interpretation = this.asInstanceOf[OpenAPIContext#Interpretation]
  override def asJValue: JValue = JsonMethods.asJValue(obj)
}
