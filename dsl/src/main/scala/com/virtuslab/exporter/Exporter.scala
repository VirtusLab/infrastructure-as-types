package com.virtuslab.exporter

import _root_.skuber.ObjectResource
import com.virtuslab.interpreter.SystemInterpreter
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.playjson.Yaml
import com.virtuslab.materializer.skuber.Resource
import play.api.libs.json.{ JsValue, Json }

object Exporter {
  def toYaml(interpreter: SystemInterpreter[SkuberContext]): Iterable[String] = {
    toJsValues(interpreter).map(Yaml.prettyPrint)
  }

  def toJson(interpreter: SystemInterpreter[SkuberContext]): Iterable[String] = {
    toJsValues(interpreter).map(Json.prettyPrint)
  }

  private[virtuslab] def toJsValues(interpreter: SystemInterpreter[SkuberContext]): Iterable[JsValue] = {
    interpreter.resources.map(toJsValue)
  }

  private[virtuslab] def toJsValue[A <: ObjectResource](resource: Resource[A]): JsValue = resource.asJsValue
}
