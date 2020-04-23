package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.core.Transformable
import com.virtuslab.iat.core.Transformable.Transformer

trait PlayJsonTransformable {
  import play.api.libs.json.{ JsValue, Json, Writes }

  implicit def transformer[P: Writes]: Transformer[P, JsValue] =
    p =>
      new Transformable[P, JsValue] {
        def transform: JsValue = Json.toJson(p)
      }
}
