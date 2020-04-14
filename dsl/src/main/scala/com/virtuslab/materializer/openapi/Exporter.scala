package com.virtuslab.materializer.openapi

import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.json.json4s.jackson.{ JsonMethods, YamlMethods }
import com.virtuslab.materializer.Materializer
import org.json4s.JValue

object Exporter {
  implicit val metaAndJsValue: Materializer[OpenAPIContext, (OpenAPIContext#Meta, JValue)] =
    (r: OpenAPIContext#Interpretation) => r.meta -> r.asJValue
  implicit val asYaml: Materializer[OpenAPIContext, String] = (r: OpenAPIContext#Interpretation) => YamlMethods.pretty(r.asJValue)
  implicit val asJson: Materializer[OpenAPIContext, String] = (r: OpenAPIContext#Interpretation) => JsonMethods.pretty(r.asJValue)
}
