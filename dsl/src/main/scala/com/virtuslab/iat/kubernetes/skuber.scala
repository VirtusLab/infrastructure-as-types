package com.virtuslab.iat.kubernetes

import _root_.skuber.{ ObjectResource, ResourceDefinition }
import com.virtuslab.iat.core
import com.virtuslab.iat.core.Resource
import com.virtuslab.iat.dsl.kubernetes.Namespace
import com.virtuslab.iat.kubernetes.interpreter.skuber._
import com.virtuslab.iat.materializer.skuber.{ MetadataProcessors, PlayJsonProcessors, UpsertDeployment }
import play.api.libs.json.{ Format, JsValue }

object skuber {
  type Base = ObjectResource

  case class SResource[P <: Base: Format: ResourceDefinition](product: P) extends Resource[P] {
    def format: Format[P] = implicitly[Format[P]]
    def definition: ResourceDefinition[P] = implicitly[ResourceDefinition[P]]
  }

  object deployment extends DefaultInterpreters with DefaultDeinterpreters with EffectsOps {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, List[Base]]
    object Upsert extends UpsertDeployment

    override type Error = Throwable
    override type Resource[B <: Base] = SResource[B]
    override type Result[B] = core.Result[B]
    override type MaybeResult[B <: Base] = Result[Either[Error, B]]

    override def resource[B <: Base: Format: ResourceDefinition](o: B): SResource[B] = SResource(o)
    override def result[B <: Base](o: Either[Error, B]): Result[Either[Throwable, B]] = new MaybeResult[B] {
      override def result: Either[Error, B] = o
    }
  }

  object metadata extends MetadataProcessors with DefaultInterpreters with RepresentableOps {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, (Metadata, JsValue)]

    override type R = (Metadata, JsValue)
    override type Resource[B <: Base] = SResource[B]
    override type Result[B] = core.Result[B]
    override def resource[B <: Base: Format: ResourceDefinition](o: B): SResource[B] = SResource(o)
    override def result(o: R): Result[R] = new Result[(Metadata, JsValue)] {
      override def result: (Metadata, JsValue) = o
    }
  }

  object playjson extends PlayJsonProcessors with DefaultInterpreters with RepresentableOps {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, JsValue]

    override type R = JsValue
    override type Resource[B <: Base] = SResource[B]
    override type Result[B] = core.Result[B]

    override def resource[B <: Base: Format: ResourceDefinition](o: B): SResource[B] = SResource(o)
    override def result(o: R): Result[R] = new Result[JsValue] {
      override def result: JsValue = o
    }
  }

  object subinterpreter extends DefaultSubinterpreters
  object details extends DefaultDetails
}
