package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter }

case class Definition[Ctx <: Context, A](obj: A, namespace: Namespace)(implicit ctx: Ctx, ev: Interpreter[Ctx, A]) {
  def interpret(): Ctx#Ret[_] = {
    ev(this)
  }
}
