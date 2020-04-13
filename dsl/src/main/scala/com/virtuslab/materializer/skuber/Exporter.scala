package com.virtuslab.materializer.skuber

import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.json.playjson.Yaml
import com.virtuslab.materializer.Materializer
import play.api.libs.json.{ JsValue, Json }

case class ShortMeta(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}

object Exporter {
  implicit val shortMetaAndJsValue: Materializer[SkuberContext, (ShortMeta, JsValue)] =
    (r: SkuberContext#Ret) => ShortMeta(r.obj.apiVersion, r.obj.kind, r.obj.ns, r.obj.name) -> r.asJsValue
  implicit val asYaml: Materializer[SkuberContext, String] = (r: SkuberContext#Ret) => Yaml.prettyPrint(r.asJsValue)
  implicit val asJson: Materializer[SkuberContext, String] = (r: SkuberContext#Ret) => Json.prettyPrint(r.asJsValue)
}
