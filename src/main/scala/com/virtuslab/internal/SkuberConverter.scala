package com.virtuslab.internal

import com.virtuslab.dsl.interpreter.SystemInterpreter
import play.api.libs.json.{ JsValue, Json }

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String)

class SkuberConverter(interpreter: SystemInterpreter) {
  import skuber.json.format._

  def toMetaAndJson: Seq[(ShortMeta, JsValue)] = {
    interpreter.resources.map {
      case namespace: skuber.Namespace =>
        ShortMeta(namespace.apiVersion, namespace.kind, namespace.ns, namespace.name) -> Json.toJson(namespace)
      case deployment: skuber.apps.v1.Deployment =>
        ShortMeta(deployment.apiVersion, deployment.kind, deployment.ns, deployment.name) -> Json.toJson(deployment)
      case service: skuber.Service =>
        ShortMeta(service.apiVersion, service.kind, service.ns, service.name) -> Json.toJson(service)
      case r => throw new IllegalArgumentException(s"Resource $r was not expected")
    }.toList
  }
}

object SkuberConverter {
  def apply(interpreter: SystemInterpreter): SkuberConverter = new SkuberConverter(interpreter)
}
