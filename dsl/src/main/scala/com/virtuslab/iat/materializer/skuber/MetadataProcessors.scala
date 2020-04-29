package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.kubernetes.skuber.Base
import com.virtuslab.iat.kubernetes.skuber.metadata.{ result, Processor, Resource }
import play.api.libs.json.{ JsValue, Json, Writes }

trait MetadataProcessors {

  implicit def objectResourceMetadataProcessor[P <: Base]: Processor[P] =
    (p: Resource[P]) => result(asMetaJsValue(p.product)(p.format))

  def asMetadata[T <: Base](o: T): Metadata = Metadata(o.apiVersion, o.kind, o.ns, o.name)
  def asMetaJsValue[T <: Base: Writes](o: T): (Metadata, JsValue) = asMetadata(o) -> Json.toJson(o)
}
