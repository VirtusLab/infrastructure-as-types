package com.virtuslab.internal

import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.exporter.Exporter
import play.api.libs.json.JsValue

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}

class SkuberConverter(interpreter: SystemInterpreter) {
  def toMetaAndJsValue: Seq[(ShortMeta, JsValue)] = {
    interpreter.resources.map { resource =>
      val r = resource.obj
      ShortMeta(r.apiVersion, r.kind, r.ns, r.name) -> Exporter.toJsValue(resource)
    }
  }
}

object SkuberConverter {
  def apply(interpreter: SystemInterpreter): SkuberConverter = new SkuberConverter(interpreter)
}
