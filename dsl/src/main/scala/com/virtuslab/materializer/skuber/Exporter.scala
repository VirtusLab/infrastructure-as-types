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

case object ShortMetaAndJsValue extends Materializer[SkuberContext, (ShortMeta, JsValue)] {
  override def apply(r: SkuberContext#Ret): (ShortMeta, JsValue) =
    ShortMeta(r.obj.apiVersion, r.obj.kind, r.obj.ns, r.obj.name) -> r.asJsValue
}

case object AsYaml extends Materializer[SkuberContext, String] {
  override def apply(resource: SkuberContext#Ret): String = Yaml.prettyPrint(resource.asJsValue)
}

case object AsJson extends Materializer[SkuberContext, String] {
  override def apply(resource: SkuberContext#Ret): String = Json.prettyPrint(resource.asJsValue)
}
