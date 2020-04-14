package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }

trait RootDefinition[Ctx <: Context, A <: Labeled, T <: Labeled] {
  def obj: A
  def members: List[Definition[Ctx, A, T, Labeled]]
  def interpret(): Iterable[Ctx#Interpretation]
}

trait Definition[Ctx <: Context, H <: Labeled, A <: Labeled, T <: Labeled] extends RootDefinition[Ctx, A, T] {
  def holder: H
}

object Definition {

  final case class ARoot[Ctx <: Context, A <: Labeled, T <: Labeled] private (
      obj: A,
      members: List[Definition[Ctx, A, T, Labeled]]
    )(implicit
      ctx: Ctx,
      ev: RootInterpreter[Ctx, A, T])
    extends RootDefinition[Ctx, A, T] {
    def interpret(): Iterable[Ctx#Interpretation] = ev(this) ++ members.flatMap(_.interpret())
  }

  final case class ADefinition[Ctx <: Context, H <: Labeled, A <: Labeled, T <: Labeled] private (
      holder: H,
      obj: A,
      members: List[Definition[Ctx, A, T, Labeled]]
    )(
      ctx: Ctx,
      ev: Interpreter[Ctx, H, A, T])
    extends Definition[Ctx, H, A, T] {
    def interpret(): Iterable[Ctx#Interpretation] = ev(this) ++ members.flatMap(_.interpret())
  }

  def apply[Ctx <: Context, A <: Labeled, T <: Labeled](
      obj: A,
      members: List[Definition[Ctx, A, T, Labeled]]
    )(implicit
      ctx: Ctx,
      ev: RootInterpreter[Ctx, A, T]
    ): RootDefinition[Ctx, A, T] =
    ARoot[Ctx, A, T](obj, members)(ctx, ev)

  def apply[Ctx <: Context, H <: Labeled, A <: Labeled, T <: Labeled](
      holder: H,
      obj: A,
      members: List[Definition[Ctx, A, T, Labeled]] = Nil
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, H, A, T]
    ): Definition[Ctx, H, A, T] =
    ADefinition(holder, obj, members)(ctx, ev)

  def apply[Ctx <: Context, A <: Labeled, T <: Labeled](
      obj: A
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, Namespace, A, T],
      ns: NamespaceBuilder[Ctx]
    ): Definition[Ctx, Namespace, A, T] = ADefinition(ns.namespace, obj, Nil)(ctx, ev)
}
