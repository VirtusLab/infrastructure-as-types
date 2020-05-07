package com.virtuslab.iat.skuber

import _root_.skuber.{ ConfigMap, ObjectResource, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat.dsl.Interpretable
import com.virtuslab.iat.kubernetes.dsl._
import skuber.apps.v1.Deployment
import skuber.ext.Ingress
import skuber.networking.NetworkPolicy

trait InterpreterOps {
  trait RootInterpreterOps1[A, B1 <: ObjectResource] {
    def obj: Interpretable[A]
    def interpret(
        implicit
        interpreter: A => B1
      ): B1 = interpreter(obj.reference)
  }

  trait InterpreterOps1[A, C, B1 <: ObjectResource] {
    def obj: Interpretable[A]
    def interpret(
        ctx: C
      )(implicit
        interpreter: (A, C) => B1
      ): B1 = interpreter(obj.reference, ctx)
  }

  trait InterpreterOps2[A, C, B1 <: ObjectResource, B2 <: ObjectResource] {
    def obj: Interpretable[A]
    def interpret(
        ctx: C
      )(implicit
        interpreter: (A, C) => (B1, B2)
      ): (B1, B2) = interpreter(obj.reference, ctx)
  }

  implicit class NamespaceInterpretationOps(val obj: Interpretable[Namespace]) extends RootInterpreterOps1[Namespace, SNamespace]
  implicit class ApplicationInterpreterOps(val obj: Interpretable[Application])
    extends InterpreterOps2[Application, Namespace, Service, Deployment]
  implicit class ConnectionInterpretationOps(val obj: Connection) extends InterpreterOps1[Connection, Namespace, NetworkPolicy]
  implicit class GatewayInterpretationOps(val obj: Gateway) extends InterpreterOps1[Gateway, Namespace, Ingress]
  implicit class ConfigurationInterpretationOps(val obj: Configuration)
    extends InterpreterOps1[Configuration, Namespace, ConfigMap]
  implicit class SecretInterpretationOps(val obj: Secret) extends InterpreterOps1[Secret, Namespace, SSecret]
}
