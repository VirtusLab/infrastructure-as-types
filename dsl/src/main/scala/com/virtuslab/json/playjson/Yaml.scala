package com.virtuslab.json.playjson

import com.virtuslab.json.playjson.jackson.JacksonYaml
import play.api.libs.json.JsValue

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
}

// Adapted from play.api.libs.json
object StaticBinding {

  /** Parses a [[JsValue]] from raw data. */
  def parseJsValue(data: Array[Byte]): JsValue =
    JacksonYaml.parseJsValue(data)

  /** Parses a [[JsValue]] from a string content. */
  def parseJsValue(input: String): JsValue =
    JacksonYaml.parseJsValue(input)

  /** Parses a [[JsValue]] from a stream. */
  def parseJsValue(stream: java.io.InputStream): JsValue =
    JacksonYaml.parseJsValue(stream)

  def generateFromJsValue(jsValue: JsValue, escapeNonASCII: Boolean): String =
    JacksonYaml.generateFromJsValue(jsValue, escapeNonASCII)

  def prettyPrint(jsValue: JsValue): String = JacksonYaml.prettyPrint(jsValue)

  def toBytes(jsValue: JsValue): Array[Byte] =
    JacksonYaml.jsValueToBytes(jsValue)
}
