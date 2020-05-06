package com.virtuslab.iat.kubernetes

import _root_.skuber.ObjectResource
import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.kubernetes.Namespace
import com.virtuslab.iat.kubernetes.interpreter.skuber._
import com.virtuslab.iat.materializer.skuber.{ ApiOps, PlayJsonProcessors }
import com.virtuslab.iat.scala.TupleOps
import play.api.libs.json.JsValue

object skuber {
  type Base = ObjectResource

  object deployment extends ApiOps with DefaultInterpreters with DefaultDeinterpreters with InterpreterOps with TupleOps {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, List[Base]]
  }

  object playjson extends PlayJsonProcessors with DefaultInterpreters with InterpreterOps with TupleOps {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, JsValue]
  }

  object interpreter extends DefaultInterpreters
  object subinterpreter extends DefaultSubinterpreters
  object details extends DefaultDetails
  object deinterpreter extends DefaultDeinterpreters
}
