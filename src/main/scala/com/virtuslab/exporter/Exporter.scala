package com.virtuslab.exporter

import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.yaml.Yaml
import play.api.libs.json.{ JsValue, Json }
import _root_.skuber.ObjectResource

object Exporter {
  def toYaml(interpreter: SystemInterpreter): Seq[String] = {
    toJsValues(interpreter).map(Yaml.prettyPrint)
  }

  def toJson(interpreter: SystemInterpreter): Seq[String] = {
    toJsValues(interpreter).map(Json.prettyPrint)
  }

  private[virtuslab] def toJsValues(interpreter: SystemInterpreter): Seq[JsValue] = {
    interpreter.resources.map(toJsValue)
  }

  private[virtuslab] def toJsValue[A <: ObjectResource](resource: Resource[A]): JsValue =
    Json.toJson(resource.obj)(resource.format)

}
