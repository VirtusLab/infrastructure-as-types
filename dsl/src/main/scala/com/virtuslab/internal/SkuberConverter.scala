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
  def toMetaAndJsValue: Iterable[(ShortMeta, JsValue)] = {
    interpreter.resources.map { r =>
      ShortMeta(r.obj.apiVersion, r.obj.kind, r.obj.ns, r.obj.name) -> r.asJsValue
    }
  }
}

object SkuberConverter {
  def apply(interpreter: SystemInterpreter[SkuberContext]): SkuberConverter = new SkuberConverter(interpreter)
}
