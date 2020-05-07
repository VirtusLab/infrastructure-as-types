package com.virtuslab.iat.openapi

import com.virtuslab.iat.core.Transformer
import com.virtuslab.iat.kubernetes.meta.Metadata
import org.json4s.JValue
import org.json4s.JsonAST.JString

trait JValueMetadataExtractor {

  implicit def jvalueMetadataTransformer: Transformer[JValue, Either[String, Metadata]] =
    (json: JValue) => extract(json)

  protected def extract(json: JValue): Either[String, Metadata] = {
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
