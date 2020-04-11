package com.virtuslab.json4s.jackson

import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.json4s.JsonInput
import org.json4s.jackson.{ Json4sScalaModule, JsonMethods }

trait YamlMethods extends JsonMethods {
  private[this] lazy val yamlMapper = {
    val m = new ObjectMapper(
      new YAMLFactory()
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    )
    m.registerModule(new Json4sScalaModule)
    // for backwards compatibility
    m.configure(USE_BIG_INTEGER_FOR_INTS, true)
    m
  }
  override def mapper: ObjectMapper = yamlMapper

  type YamlInput = JsonInput
}

object YamlMethods extends YamlMethods
