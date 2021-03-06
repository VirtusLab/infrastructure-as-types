package com.virtuslab.iat.skuber.yaml

import com.virtuslab.iat.skuber.yaml.jackson.JacksonYaml
import play.api.libs.json.{ JsValue, Json }

object Yaml {
  import java.io.InputStream

  def parse(input: String): JsValue = StaticBinding.parseJsValue(input)
  def parse(input: InputStream): JsValue = StaticBinding.parseJsValue(input)
  def parse(input: Array[Byte]): JsValue = StaticBinding.parseJsValue(input)

  def stringify(json: JsValue): String =
    StaticBinding.generateFromJsValue(json, escapeNonASCII = false)

  def toBytes(json: JsValue): Array[Byte] = StaticBinding.toBytes(json)

  //We use unicode \u005C for a backlash in comments, because Scala will replace unicode escapes during lexing
  //anywhere in the program.
  def asciiStringify(json: JsValue): String =
    StaticBinding.generateFromJsValue(json, escapeNonASCII = true)

  def prettyPrint(json: JsValue): String = StaticBinding.prettyPrint(json)

  def yamlToJson(yaml: String): String = Json.prettyPrint(parse(yaml))
}

// Adapted from play.api.libs.json
object StaticBinding {

  /** Parses a {@code JsValue} from raw data. */
  def parseJsValue(data: Array[Byte]): JsValue =
    JacksonYaml.parseJsValue(data)

  /** Parses a {@code JsValue} from a string content. */
  def parseJsValue(input: String): JsValue =
    JacksonYaml.parseJsValue(input)

  /** Parses a {@code JsValue} from a stream. */
  def parseJsValue(stream: java.io.InputStream): JsValue =
    JacksonYaml.parseJsValue(stream)

  def generateFromJsValue(jsValue: JsValue, escapeNonASCII: Boolean): String =
    JacksonYaml.generateFromJsValue(jsValue, escapeNonASCII)

  def prettyPrint(jsValue: JsValue): String = JacksonYaml.prettyPrint(jsValue)

  def toBytes(jsValue: JsValue): Array[Byte] =
    JacksonYaml.jsValueToBytes(jsValue)
}
