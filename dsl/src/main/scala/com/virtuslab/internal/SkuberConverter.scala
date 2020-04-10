package com.virtuslab.internal

import com.virtuslab.exporter.Exporter
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.interpreter.SystemInterpreter
import play.api.libs.json.JsValue

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}

class SkuberConverter(interpreter: SystemInterpreter[SkuberContext]) {
  def toMetaAndJsValue: Seq[(ShortMeta, JsValue)] = {
    interpreter.resources.map { resource =>
      val r = resource.obj
      ShortMeta(r.apiVersion, r.kind, r.ns, r.name) -> Exporter.toJsValue(resource)
    }
  }
}

object SkuberConverter {
  def apply(interpreter: SystemInterpreter[SkuberContext]): SkuberConverter = new SkuberConverter(interpreter)
}
