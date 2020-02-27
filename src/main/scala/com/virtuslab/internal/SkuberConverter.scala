package com.virtuslab.internal

import com.virtuslab.dsl.{ System, SystemInterpreter }
import play.api.libs.json.{ JsValue, Json }

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String)

class SkuberConverter(interpreter: SystemInterpreter) {
  import skuber.json.format._

  def toMetaAndJson(system: System): Seq[(ShortMeta, JsValue)] = {
    interpreter(system) flatMap {
      case (service, deployment) =>
        Seq(
          ShortMeta(service.apiVersion, service.kind, service.ns, service.name) -> Json.toJson(service),
          ShortMeta(deployment.apiVersion, deployment.kind, deployment.ns, deployment.name) -> Json.toJson(deployment)
        )
      case r => throw new IllegalArgumentException(s"Resource $r was not expected")
    }
  }
}

object SkuberConverter {
  def apply(interpreter: SystemInterpreter): SkuberConverter = new SkuberConverter(interpreter)
}
