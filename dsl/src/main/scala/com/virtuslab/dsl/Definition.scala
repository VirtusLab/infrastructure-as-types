package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter }

trait Definition[Ctx <: Context, A] {
  def obj: A
  def namespace: Namespace
  def interpret(): Iterable[Ctx#Ret]
}

object Definition {

  case class ADefinition[Ctx <: Context, A] private (obj: A, namespace: Namespace)(implicit ctx: Ctx, ev: Interpreter[Ctx, A]) extends Definition[Ctx, A] {
    def interpret(): Iterable[Ctx#Ret] = ev(this)
  }

  def apply[Ctx <: Context, A](obj: A, namespace: Namespace)(implicit ctx: Ctx, ev: Interpreter[Ctx, A]): Definition[Ctx, A] =
    ADefinition(obj, namespace)(ctx, ev)

  def apply[Ctx <: Context, A](
      obj: A
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, A],
      ns: NamespaceBuilder[Ctx]
    ): Definition[Ctx, A] =
    ADefinition[Ctx, A](obj, ns.namespace)(ctx, ev)
}
