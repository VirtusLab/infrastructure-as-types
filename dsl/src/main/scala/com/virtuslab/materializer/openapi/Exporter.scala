package com.virtuslab.materializer.openapi

import com.virtuslab.interpreter.openapi.OpenAPI.OpenAPIContext
import com.virtuslab.json.json4s.jackson.{ JsonMethods, YamlMethods }
import com.virtuslab.materializer.Materializer

object Exporter {
  implicit val asYaml: Materializer[OpenAPIContext, String] = (r: OpenAPIContext#Ret) => YamlMethods.pretty(r.asJValue)
  implicit val asJson: Materializer[OpenAPIContext, String] = (r: OpenAPIContext#Ret) => JsonMethods.pretty(r.asJValue)
}
