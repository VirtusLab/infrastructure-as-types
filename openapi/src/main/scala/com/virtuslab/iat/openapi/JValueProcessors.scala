package com.virtuslab.iat.openapi

import com.virtuslab.iat.json.json4s.jackson.YamlMethods
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import org.json4s.jackson.JsonMethods
import org.json4s.{ Extraction, Formats }

trait JValueProcessors extends JValueMetadataExtractor {
  import com.virtuslab.iat.scala.ops._
  import org.json4s.JValue

  def asMeta: JValue => Metadata =
    (json: JValue) =>
      extract(json).fold(
        e =>
          throw new IllegalStateException( /* FIXME: how to make it compile-time? need better model */
            s"metadata extraction failed: $e\nJson:\n${JsonMethods.pretty(json)}\n"
          ),
        identity
      )

  def asJValue[A](a: A)(implicit formats: Formats): JValue = Extraction.decompose(a)
  def asMetaJValue[A](a: A)(implicit formats: Formats): (Metadata, JValue) = asMetaJValue(asJValue(a))
  def asJsonString[A](a: A)(implicit formats: Formats): String = asJsonString(asJValue[A](a))
  def asYamlString[A](a: A)(implicit formats: Formats): String = YamlMethods.pretty(asJValue[A](a))
  def asMetaJsonString[A](a: A)(implicit formats: Formats): (Metadata, String) =
    asMetaJValue(a).map(identity, asJsonString)
  def asMetaYamlString[A](a: A)(implicit formats: Formats): (Metadata, String) =
    asMetaJValue(a).map(identity, asYamlString)

  def asMetaJValue(json: JValue): (Metadata, JValue) = asMeta(json) -> json
  def asJsonString(json: JValue): String = JsonMethods.pretty(json)
  def asYamlString(json: JValue): String = YamlMethods.pretty(json)

  implicit class ApiModelOps1[A <: ApiModel](a: A) {
    def asJValues(implicit formats: Formats): List[JValue] = asJValue(a) :: Nil
    def asMetaJValues(implicit formats: Formats): List[(Metadata, JValue)] = asMetaJValue(a) :: Nil
    def asMetaJsonString(implicit formats: Formats): List[(Metadata, String)] =
      asMetaJValue(a).map(identity, asJsonString) :: Nil
    def asMetaYamlString(implicit formats: Formats): List[(Metadata, String)] =
      asMetaJValue(a).map(identity, asYamlString) :: Nil
  }

  implicit class ObjectResourceOps2[A1 <: ApiModel, A2 <: ApiModel](t: (A1, A2)) {
    def asJValues(implicit formats: Formats): List[JValue] =
      t.map(_.asJValues, _.asJValues).reduce(_ ++ _)
    def asMetaJValues(implicit formats: Formats): List[(Metadata, JValue)] =
      t.map(_.asMetaJValues, _.asMetaJValues).reduce(_ ++ _)
    def asMetaJsonString(implicit formats: Formats): List[(Metadata, String)] =
      t.map(_.asMetaJsonString, _.asMetaJsonString).reduce(_ ++ _)
    def asMetaYamlString(implicit formats: Formats): List[(Metadata, String)] =
      t.map(_.asMetaYamlString, _.asMetaYamlString).reduce(_ ++ _)
  }
}
