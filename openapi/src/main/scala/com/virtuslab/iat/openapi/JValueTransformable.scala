package com.virtuslab.iat.openapi

import com.virtuslab.iat.core.Transformer
import com.virtuslab.iat.json.json4s.jackson.JsonMethods

trait JValueTransformable {
  import org.json4s.JValue

  implicit def jvalueTransformer[P /*: Writer*/ ]: Transformer[P, JValue] =
    (p: P) => JsonMethods.asJValue /*[P]*/ (p)
}
