package com.virtuslab.iat.json

import com.virtuslab.iat.core.Transformable
import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.kubernetes.Metadata

object json4s {

  trait JValueTransformable {
    import com.virtuslab.json.json4s.jackson.JsonMethods
    import org.json4s.JValue

    implicit def transformer[P /*: Writer*/ ]: Transformer[P, JValue] =
      p =>
        new Transformable[P, JValue] {
          def transform: JValue = JsonMethods.asJValue /*[P]*/ (p)
        }
  }

  trait JValueMetadataExtractor {
    import org.json4s.JsonAST.{ JField, JObject, JString, JValue }

    def extract(json: JValue): Either[String, Metadata] = {
      val metas = for {
        JString(apiVersion) <- json \ "apiVersion"
        JString(kind) <- json \ "kind"
        JObject(metadata) <- json \ "metadata"
        JField("name", JString(name)) <- metadata
        namespace = metadata.collectFirst { case JField("namespace", JString(namespace)) => namespace }
      } yield Metadata(
        apiVersion = apiVersion,
        kind = kind,
        name = name,
        namespace = namespace.getOrElse("")
      )
      if (metas.size > 1) Left("expected 1 metadata, got: " + metas.size)
      else if (metas.isEmpty) Left(s"expected apiVersion, kind, metadata.name, metadata.namespace is optional")
      else Right(metas.head)
    }
  }

}

object playjson {
  trait PlayJsonTransformable {
    import play.api.libs.json.{ JsValue, Json, Writes }

    implicit def transformer[P: Writes]: Transformer[P, JsValue] =
      p =>
        new Transformable[P, JsValue] {
          def transform: JsValue = Json.toJson(p)
        }
  }
}
