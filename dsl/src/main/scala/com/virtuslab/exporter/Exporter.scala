package com.virtuslab.exporter

import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.yaml.Yaml
import play.api.libs.json.{ JsValue, Json }
import _root_.skuber.ObjectResource
import com.virtuslab.interpreter.{ Context, SystemInterpreter }

object Exporter {
  def toYaml[Ctx <: Context](interpreter: SystemInterpreter[Ctx]): Seq[String] = {
    toJsValues(interpreter).map(Yaml.prettyPrint)
  }

  def toJson[Ctx <: Context](interpreter: SystemInterpreter[Ctx]): Seq[String] = {
    toJsValues(interpreter).map(Json.prettyPrint)
  }

  private[virtuslab] def toJsValues[Ctx <: Context](interpreter: SystemInterpreter[Ctx]): Seq[JsValue] = {
    interpreter.resources.map(toJsValue)
  }

  private[virtuslab] def toJsValue[A <: ObjectResource](resource: Resource[A]): JsValue =
    Json.toJson(resource.obj)(resource.format)

}
