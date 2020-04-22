package com.virtuslab.materializer.skuber

import com.virtuslab.iat.json.playjson.Yaml
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import com.virtuslab.materializer.Materializer
import play.api.libs.json.{ JsValue, Json }

object Exporter {
  implicit val metaAndJsValue: Materializer[SkuberContext, (SkuberContext#Meta, JsValue)] =
    (r: SkuberContext#Interpretation) => r.meta -> r.asJsValue
  implicit val asYaml: Materializer[SkuberContext, String] = (r: SkuberContext#Interpretation) => Yaml.prettyPrint(r.asJsValue)
  implicit val asJson: Materializer[SkuberContext, String] = (r: SkuberContext#Interpretation) => Json.prettyPrint(r.asJsValue)
}
