package com.virtuslab.scalatest.yaml

import com.virtuslab.json4s.jackson.YamlMethods
import org.json4s.jackson.JsonMethods

object Converters {
  def yamlToJson(yaml: String): String = {
    JsonMethods.pretty(YamlMethods.parse(yaml))
  }
}
