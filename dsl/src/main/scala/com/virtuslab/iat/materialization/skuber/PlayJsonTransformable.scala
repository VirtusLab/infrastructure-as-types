package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.kubernetes.skuber.{ Base, STransformer }
import play.api.libs.json.Format
import skuber.ResourceDefinition

trait PlayJsonTransformable {
  import play.api.libs.json.{ JsValue, Json, Writes }

  implicit def transformer[P <: Base: Writes]: STransformer[P, JsValue] =
    new STransformer[P, JsValue] {
      override def apply(p: P)(implicit f: Format[P], d: ResourceDefinition[P]): JsValue = Json.toJson(p)
    }
}
