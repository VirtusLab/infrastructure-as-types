package com.virtuslab.interpreter

import com.virtuslab.dsl.Definition

trait Context {
  type Ret
}

trait Interpreter[Ctx <: Context, A] extends (Definition[Ctx, A] => Iterable[Ctx#Ret])
