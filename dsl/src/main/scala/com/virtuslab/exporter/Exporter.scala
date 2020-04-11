package com.virtuslab.exporter

import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.playjson.Yaml
import play.api.libs.json.{ JsValue, Json }
import _root_.skuber.ObjectResource
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.interpreter.{ Context, SystemInterpreter }

object Exporter {
  def toYaml(interpreter: SystemInterpreter[SkuberContext]): Seq[String] = {
    toJsValues(interpreter).map(Yaml.prettyPrint)
  }

  def toJson(interpreter: SystemInterpreter[SkuberContext]): Seq[String] = {
    toJsValues(interpreter).map(Json.prettyPrint)
  }

  private[virtuslab] def toJsValues(interpreter: SystemInterpreter[SkuberContext]): Seq[JsValue] = {
    interpreter.resources.map(toJsValue)
  }

  private[virtuslab] def toJsValue[A <: ObjectResource](resource: Resource[A]): JsValue =
    Json.toJson(resource.obj)(resource.format)

}
