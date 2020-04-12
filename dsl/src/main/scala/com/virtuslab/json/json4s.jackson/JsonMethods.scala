package com.virtuslab.json.json4s.jackson

import com.virtuslab.kubernetes.client.openapi.api.EnumsSerializers
import com.virtuslab.kubernetes.client.openapi.core.Serializers
import org.json4s.{ DefaultFormats, Extraction, Formats, JValue }

trait JsonMethods extends org.json4s.jackson.JsonMethods {
  val formats: Formats = DefaultFormats ++ EnumsSerializers.all ++ Serializers.all

  def asJValue(obj: Any): JValue = Extraction.decompose(obj)(formats)
}

object JsonMethods extends JsonMethods
