package com.virtuslab.scalatest.yaml

import com.virtuslab.yaml.Yaml
import play.api.libs.json.Json

object Converters {
  def yamlToJson(yaml: String): String = {
    Json.prettyPrint(Yaml.parse(yaml))
  }
}
