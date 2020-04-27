package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.kubernetes.skuber.{ Base, STransformer }
import play.api.libs.json.{ Format, JsValue, Json, Writes }
import skuber.ResourceDefinition

trait ObjectResourceMetadataExtractor {

  implicit def transformer[P <: Base]: STransformer[P, (Metadata, JsValue)] =
    new STransformer[P, (Metadata, JsValue)] {
      override def apply(p: P)(implicit f: Format[P], d: ResourceDefinition[P]): (Metadata, JsValue) =
        asMetaJsValue(p)
    }

  def asMetadata[T <: Base](o: T): Metadata = Metadata(o.apiVersion, o.kind, o.ns, o.name)
  def asMetaJsValue[T <: Base: Writes](o: T): (Metadata, JsValue) = asMetadata(o) -> Json.toJson(o)
}
