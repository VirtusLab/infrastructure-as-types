package com.virtuslab.iat.kubernetes

import com.virtuslab.iat.core.InterpreterDerivation
import com.virtuslab.iat.dsl.Namespace

object skuber {
  import com.virtuslab.iat.json.playjson.PlayJsonTransformable

  object playjason extends PlayJsonTransformable {
    import play.api.libs.json.JsValue
    object Interpreter extends InterpreterDerivation[Namespace, JsValue]
  }
}
