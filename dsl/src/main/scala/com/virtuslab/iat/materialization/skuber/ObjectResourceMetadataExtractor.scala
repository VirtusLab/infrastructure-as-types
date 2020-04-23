package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.core.Transformable
import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.{ JsValue, Json, Writes }
import skuber.ObjectResource

trait ObjectResourceMetadataExtractor {

  implicit def transformer[P <: ObjectResource: Writes]: Transformer[P, (Metadata, JsValue)] =
    p =>
      new Transformable[P, (Metadata, JsValue)] {
        def transform: (Metadata, JsValue) = asMetaJsValue(p)
      }

  def asMetadata[T <: Base](o: T): Metadata = Metadata(o.apiVersion, o.kind, o.ns, o.name)
  def asMetaJsValue[T <: Base: Writes](o: T): (Metadata, JsValue) = asMetadata(o) -> Json.toJson(o)
}
