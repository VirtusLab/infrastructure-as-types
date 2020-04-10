package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter }

case class Definition[Ctx <: Context, A](obj: A, namespace: Namespace)(implicit ctx: Ctx, ev: Interpreter[Ctx, A]) {
  def interpret(): Ctx#Ret[_] = ev(this)
}

object Definition {
  def apply[Ctx <: Context, A](
      obj: A
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, A],
      ns: NamespaceBuilder[Ctx]
    ): Definition[Ctx, A] =
    new Definition(obj, ns.namespace)(ctx, ev)
}
