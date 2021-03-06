package com.virtuslab.iat.skuber.yaml.jackson

import java.io.{ InputStream, StringWriter }

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper, ObjectWriter }
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.virtuslab.iat.scala.unit.ops._
import play.api.libs.json.jackson.PlayJsonModule
import play.api.libs.json.{ JsValue, JsonParserSettings }

// Adapted from play.api.libs.json.jackson
private[yaml] object JacksonYaml {
  private lazy val mapper = (new ObjectMapper).registerModule(new PlayJsonModule(JsonParserSettings.settings))

  private lazy val yamlFactory = new YAMLFactory(mapper)

  private def stringYamlGenerator(out: java.io.StringWriter) =
    yamlFactory.createGenerator(out)

  def parseJsValue(data: Array[Byte]): JsValue =
    mapper.readValue(yamlFactory.createParser(data), classOf[JsValue])

  def parseJsValue(input: String): JsValue =
    mapper.readValue(yamlFactory.createParser(input), classOf[JsValue])

  def parseJsValue(stream: InputStream): JsValue =
    mapper.readValue(yamlFactory.createParser(stream), classOf[JsValue])

  private def withStringWriter[T](f: StringWriter => T): T = {
    val sw = new StringWriter()

    try {
      f(sw)
    } catch {
      case err: Throwable => throw err
    } finally {
      if (sw != null) try {
        sw.close()
      } catch {
        case _: Throwable => ()
      }
    }
  }

  def generateFromJsValue(jsValue: JsValue, escapeNonASCII: Boolean): String =
    withStringWriter { sw =>
      val gen = stringYamlGenerator(sw)

      if (escapeNonASCII) {
        // Note that in Scala 2.12, @nowarn is provided by scala-compat (and not by Scala itself);
        // silencer-plugin is used to actually take the annotation into consideration and suppress the deprecation warning.
        // In Scala 2.13, @nowarn is a part of the language library.
        @scala.annotation.nowarn
        val feature = JsonGenerator.Feature.ESCAPE_NON_ASCII
        gen.enable(feature).toUnit()
      }

      mapper.writeValue(gen, jsValue)
      sw.flush()
      sw.getBuffer.toString
    }

  def prettyPrint(jsValue: JsValue): String = withStringWriter { sw =>
    val gen = stringYamlGenerator(sw).setPrettyPrinter(
      new DefaultPrettyPrinter()
    )
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()

    writer.writeValue(gen, jsValue)
    sw.flush()
    sw.getBuffer.toString
  }

  def jsValueToBytes(jsValue: JsValue): Array[Byte] =
    mapper.writeValueAsBytes(jsValue)

  def jsValueToJsonNode(jsValue: JsValue): JsonNode =
    mapper.valueToTree(jsValue)

  def jsonNodeToJsValue(jsonNode: JsonNode): JsValue =
    mapper.treeToValue(jsonNode, classOf[JsValue])
}
