package com.virtuslab.iat.kubernetes.interpreter.skuber

import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.Interpretable
import com.virtuslab.iat.dsl.kubernetes.{ Application, Namespace }
import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.Format
import skuber.apps.v1.Deployment
import skuber.{ ResourceDefinition, Service, Namespace => SNamespace }

trait EffectsOps {
  type Error
  type Resource[B <: Base] <: core.Resource[B]
  type Result[R <: Either[Error, Base]] <: core.Result[R]
  type Processor[B <: Base] = core.Processor[B, Base, Either[Error, B], Either[Error, Base], Resource, Result]

  def resource[B <: Base: Format: ResourceDefinition](o: B): Resource[B]
  def result[B <: Base](o: Either[Error, B]): Result[Either[Error, B]]

  class RootEffectsOps1[A, B1 <: Base](obj: Interpretable[A]) {
    def process(
        details: B1 => B1 = identity,
        results: Result[Either[Error, B1]] => Result[Either[Error, B1]] = identity
      )(implicit
        interpreter: A => B1,
        deinterpreter: Result[Either[Error, B1]] => Either[Error, A],
        format1: Format[B1],
        definition1: ResourceDefinition[B1],
        processor1: Processor[B1]
      ): Either[Error, A] = {
      val interpreted = interpreter(obj.reference)
      val modified = details(interpreted)
      val b1 = processor1.process(resource(modified)(format1, definition1))
      val handled: Result[Either[Error, B1]] = results(b1)
      deinterpreter(handled)
    }
  }

  class EffectsOps1[A, C, B1 <: Base](obj: Interpretable[A]) {
    def process(
        ctx: C,
        details: B1 => B1 = identity,
        results: Result[Either[Error, B1]] => Result[Either[Error, B1]] = identity
      )(implicit
        interpreter: (A, C) => B1,
        deinterpreter: Result[Either[Error, B1]] => Either[Error, A],
        format1: Format[B1],
        definition1: ResourceDefinition[B1],
        processor1: Processor[B1]
      ): Either[Error, A] = {
      val interpreted = interpreter(obj.reference, ctx)
      val modified = details(interpreted)
      val b1 = processor1.process(resource(modified)(format1, definition1))
      val handled: Result[Either[Error, B1]] = results(b1)
      deinterpreter(handled)
    }
  }

  class EffectsOps2[A, C, B1 <: Base, B2 <: Base](obj: Interpretable[A]) {
    def process(
        ctx: C,
        details: ((B1, B2)) => (B1, B2) = identity,
        results: ((Result[Either[Error, B1]], Result[Either[Error, B2]])) => (Result[Either[Error, B1]],
            Result[Either[Error, B2]]) = identity
      )(implicit
        interpreter: (A, C) => (B1, B2),
        deinterpreter: (Result[Either[Error, B1]], Result[Either[Error, B2]]) => Either[Error, A],
        format1: Format[B1],
        format2: Format[B2],
        definition1: ResourceDefinition[B1],
        definition2: ResourceDefinition[B2],
        processor1: Processor[B1],
        processor2: Processor[B2]
      ): Either[Error, A] = {
      val interpreted = interpreter(obj.reference, ctx)
      val modified = details(interpreted)
      val b1 = processor1.process(resource(modified._1)(format1, definition1))
      val b2 = processor2.process(resource(modified._2)(format2, definition2))
      val handled = results(b1, b2)
      deinterpreter(handled._1, handled._2)
    }
  }

  implicit class NamespaceEffectsOps(ns: Namespace) extends RootEffectsOps1[Namespace, SNamespace](ns)
  implicit class ApplicationEffectsOps(app: Application) extends EffectsOps2[Application, Namespace, Service, Deployment](app)
}
