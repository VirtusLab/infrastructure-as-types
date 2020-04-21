package com.virtuslab.iat.json

import com.virtuslab.iat.core.Transformable
import com.virtuslab.iat.core.Transformable.Transformer

object json4s {
  trait JValueTransformable {
    import com.virtuslab.json.json4s.jackson.JsonMethods
    import org.json4s.JValue

    implicit def transformer[P /*: Writer*/ ]: Transformer[P, JValue] =
      p =>
        new Transformable[P, JValue] {
          def transform: JValue = JsonMethods.asJValue /*[P]*/ (p)
        }
  }
}

object playjson {
  trait PlayJsonTransformable {
    import play.api.libs.json.{ JsValue, Json, Writes }

    implicit def transformer[P: Writes]: Transformer[P, JsValue] =
      p =>
        new Transformable[P, JsValue] {
          def transform: JsValue = Json.toJson(p)
        }
  }
}
