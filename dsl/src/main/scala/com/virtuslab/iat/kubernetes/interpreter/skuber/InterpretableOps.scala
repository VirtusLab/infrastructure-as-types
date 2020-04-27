package com.virtuslab.iat.kubernetes.interpreter.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ResourceDefinition, Service, Namespace => SNamespace }
import com.virtuslab.iat.dsl.Interpretable
import com.virtuslab.iat.dsl.kubernetes.{ Application, Namespace }
import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.Format

trait InterpretableOps {
  type Resource[B <: Base]
  def resource[B <: Base: Format: ResourceDefinition](o: B): Resource[B]

  class RootInterpretableOps1[A, B1 <: Base](obj: Interpretable[A]) {
    def interpret(
        implicit
        interpreter: A => B1,
        format: Format[B1],
        definition: ResourceDefinition[B1]
      ): List[Resource[B1]] = interpret(identity)

    def interpret(
        details: B1 => B1
      )(implicit
        interpreter: A => B1,
        format1: Format[B1],
        definition1: ResourceDefinition[B1]
      ): List[Resource[B1]] = {
      val modified = details(interpreter(obj.reference))
      resource(modified) :: Nil
    }
  }

  class InterpretableOps1[A, C, B1 <: Base](obj: Interpretable[A]) {
    def interpret(
        ctx: C,
        details: B1 => B1 = identity
      )(implicit
        interpreter: (A, C) => B1,
        format1: Format[B1],
        definition1: ResourceDefinition[B1]
      ): List[Resource[B1]] = {
      val modified = details(interpreter(obj.reference, ctx))
      resource(modified) :: Nil
    }
  }

  class InterpretableOps2[A, C, B1 <: Base, B2 <: Base](obj: Interpretable[A]) {
    def interpret(
        ctx: C,
        details: ((B1, B2)) => (B1, B2) = identity
      )(implicit
        interpreter: (A, C) => (B1, B2),
        format1: Format[B1],
        format2: Format[B2],
        definition1: ResourceDefinition[B1],
        definition2: ResourceDefinition[B2]
      ): List[Resource[_ <: Base]] = {
      val interpreted = interpreter(obj.reference, ctx)
      val modified = details(interpreted)
      resource(modified._1) :: resource(modified._2) :: Nil
    }
  }

  implicit class NamespaceInterpretableOps(ns: Namespace) extends RootInterpretableOps1[Namespace, SNamespace](ns)
  implicit class ApplicationInterpretableOps(app: Application)
    extends InterpretableOps2[Application, Namespace, Service, Deployment](app)
}
