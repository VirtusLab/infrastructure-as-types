package com.virtuslab.iat.json

object converters {
  import org.json4s
  import play.api.libs.{ json => pjson }

  def toJson4s(json: play.api.libs.json.JsValue): org.json4s.JValue = json match {
    case pjson.JsString(str)    => json4s.JString(str)
    case pjson.JsNull           => json4s.JNull
    case pjson.JsBoolean(value) => json4s.JBool(value)
    case pjson.JsNumber(value)  => json4s.JDecimal(value)
    case pjson.JsArray(items)   => json4s.JArray(items.map(toJson4s).toList)
    case pjson.JsObject(items)  => json4s.JObject(items.map { case (k, v) => k -> toJson4s(v) }.toList)
  }

  def toPlayJson(json: org.json4s.JValue): play.api.libs.json.JsValue = json match {
    case json4s.JString(str)    => pjson.JsString(str)
    case json4s.JNothing        => pjson.JsNull
    case json4s.JNull           => pjson.JsNull
    case json4s.JDecimal(value) => pjson.JsNumber(value)
    case json4s.JDouble(value)  => pjson.JsNumber(value)
    case json4s.JInt(value)     => pjson.JsNumber(BigDecimal(value))
    case json4s.JLong(value)    => pjson.JsNumber(BigDecimal(value))
    case json4s.JBool(value)    => pjson.JsBoolean(value)
    case json4s.JArray(fields)  => pjson.JsArray(fields.map(toPlayJson))
    case json4s.JSet(fields)    => pjson.JsArray(fields.map(toPlayJson).toSeq)
    case json4s.JObject(fields) => pjson.JsObject(fields.map { case (k, v) => k -> toPlayJson(v) }.toMap)
  }
}
