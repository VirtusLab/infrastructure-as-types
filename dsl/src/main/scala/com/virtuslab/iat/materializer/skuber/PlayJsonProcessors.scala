package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.json.playjson.Yaml
import com.virtuslab.iat.kubernetes.skuber.Base
import com.virtuslab.iat.kubernetes.skuber.playjson.{ result, Processor, Resource }
import play.api.libs.json.{ JsValue, Json, Writes }

trait PlayJsonProcessors {
  implicit def objectResourceProcessor[P <: Base: Writes]: Processor[P] =
    (p: Resource[P]) => result(Json.toJson(p.product)(p.format))

  def asJsValue[T: Writes](o: T): JsValue = Json.toJson(o)
  def asJsonString[T: Writes](o: T): String = Json.prettyPrint(asJsValue(o))
  def asYamlString[T: Writes](o: T): String = Yaml.prettyPrint(asJsValue(o))
}
