package com.virtuslab.iat.kubernetes.interpreter.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ConfigMap, ResourceDefinition, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.Interpretable
import com.virtuslab.iat.dsl.kubernetes.{ Application, Configuration, Connection, Gateway, Namespace, Secret }
import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.Format
import skuber.ext.Ingress
import skuber.networking.NetworkPolicy

trait RepresentableOps {
  type R
  type BaseR = Any
  type Resource[B <: Base] <: core.Resource[B]
  type Result[X] <: core.Result[X]
  type Processor[B <: Base] = core.Processor[B, Base, R, BaseR, Resource, Result]

  def resource[B <: Base: Format: ResourceDefinition](o: B): Resource[B]
  def result(o: R): Result[R]

  class RootRepresentableOps1[A, B1 <: Base](obj: Interpretable[A]) {
    def interpret(
        implicit
        interpreter: A => B1,
        format: Format[B1],
        definition: ResourceDefinition[B1],
        processor1: Processor[B1]
      ): List[Result[R]] = interpret(identity)

    def interpret(
        details: B1 => B1
      )(implicit
        interpreter: A => B1,
        format1: Format[B1],
        definition1: ResourceDefinition[B1],
        processor1: Processor[B1]
      ): List[Result[R]] = {
      val modified = details(interpreter(obj.reference))
      val b1 = processor1.process(resource(modified))
      b1 :: Nil
    }
  }

  class RepresentableOps1[A, C, B1 <: Base](obj: Interpretable[A]) {
    def interpret(
        ctx: C,
        details: B1 => B1 = identity
      )(implicit
        interpreter: (A, C) => B1,
        format1: Format[B1],
        definition1: ResourceDefinition[B1],
        processor1: Processor[B1]
      ): List[Result[R]] = {
      val modified = details(interpreter(obj.reference, ctx))
      val b1 = processor1.process(resource(modified))
      b1 :: Nil
    }
  }

  class RepresentableOps2[A, C, B1 <: Base, B2 <: Base](obj: Interpretable[A]) {
    def interpret(
        ctx: C,
        details: ((B1, B2)) => (B1, B2) = identity
      )(implicit
        interpreter: (A, C) => (B1, B2),
        format1: Format[B1],
        format2: Format[B2],
        definition1: ResourceDefinition[B1],
        definition2: ResourceDefinition[B2],
        processor1: Processor[B1],
        processor2: Processor[B2]
      ): List[Result[R]] = {
      val interpreted = interpreter(obj.reference, ctx)
      val (m1, m2) = details(interpreted)
      val b1 = processor1.process(resource(m1)(format1, definition1))
      val b2 = processor2.process(resource(m2)(format2, definition2))
      b1 :: b2 :: Nil
    }
  }

  implicit class NamespaceRepresentableOps(ns: Namespace) extends RootRepresentableOps1[Namespace, SNamespace](ns)
  implicit class ApplicationRepresentableOps(app: Application)
    extends RepresentableOps2[Application, Namespace, Service, Deployment](app)
  implicit class ConnectionRepresentableOps(c: Connection) extends RepresentableOps1[Connection, Namespace, NetworkPolicy](c)
  implicit class GatewayRepresentableOps(g: Gateway) extends RepresentableOps1[Gateway, Namespace, Ingress](g)
  implicit class ConfigurationRepresentableOps(c: Configuration) extends RepresentableOps1[Configuration, Namespace, ConfigMap](c)
  implicit class SecretRepresentableOps(s: Secret) extends RepresentableOps1[Secret, Namespace, SSecret](s)
}
