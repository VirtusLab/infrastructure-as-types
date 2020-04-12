package com.virtuslab.interpreter

import com.virtuslab.dsl.{ Definition, Labeled, RootDefinition }

trait Context {
  type T
  type Ret
}

trait RootInterpreter[Ctx <: Context, A <: Labeled, T <: Labeled] extends (RootDefinition[Ctx, A, T] => Iterable[Ctx#Ret])
trait Interpreter[Ctx <: Context, H <: Labeled, A <: Labeled, T <: Labeled]
  extends (Definition[Ctx, H, A, T] => Iterable[Ctx#Ret])
