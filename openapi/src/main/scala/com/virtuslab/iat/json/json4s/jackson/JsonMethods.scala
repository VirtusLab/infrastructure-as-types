package com.virtuslab.iat.json.json4s.jackson

import com.virtuslab.kubernetes.client.custom.B64Encoded
import com.virtuslab.kubernetes.client.openapi.api.EnumsSerializers
import com.virtuslab.kubernetes.client.openapi.core.Serializers
import org.json4s.{ DefaultFormats, Extraction, Formats, JValue }

trait JsonMethods extends org.json4s.jackson.JsonMethods {
  val defaultFormats: Formats =
    DefaultFormats ++ EnumsSerializers.all ++ Serializers.all + B64Encoded.json4sCustomSerializer

  def asJValue(obj: Any): JValue = Extraction.decompose(obj)(defaultFormats)
}

object JsonMethods extends JsonMethods
