package com.virtuslab.interpreter

import com.virtuslab.dsl.{ Definition, RootDefinition }

trait Context {
  type Ret
}

trait RootInterpreter[Ctx <: Context, A, T] extends (RootDefinition[Ctx, A, T] => Iterable[Ctx#Ret])
trait Interpreter[Ctx <: Context, H, A, T] extends (Definition[Ctx, H, A, T] => Iterable[Ctx#Ret])
