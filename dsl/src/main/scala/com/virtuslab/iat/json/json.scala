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
    import org.json4s.JsonAST.{ JString, JValue }

    def extract(json: JValue): Either[String, Metadata] = {
      val apiVersion = json \ "apiVersion" match {
        case JString(apiVersion) => Right(apiVersion)
        case _                   => Left("apiVersion not found")
      }
      val kind = json \ "kind" match {
        case JString(kind) => Right(kind)
        case _             => Left("kind not found")
      }
      val name = json \ "metadata" \ "name" match {
        case JString(name) => Right(name)
        case _             => Left("metadata.name not found")
      }
      val namespace = json \ "metadata" \ "namespace" match {
        case JString(namespace) => Right(namespace)
        case _                  => Right("") // namespace is optional
      }
      val m = apiVersion :: kind :: name :: namespace :: Nil
      val lefts = m.filter(_.isLeft).map(_.swap).map(_.toOption.get)
      if (lefts.nonEmpty)
        Left(lefts.mkString(", ") + "; JValue: " + json)
      else
        Right(
          Metadata(
            apiVersion = apiVersion.toOption.get,
            kind = kind.toOption.get,
            name = name.toOption.get,
            namespace = namespace.getOrElse("")
          )
        )
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
