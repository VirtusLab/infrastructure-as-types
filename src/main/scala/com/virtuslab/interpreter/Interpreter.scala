package com.virtuslab.interpreter

import com.virtuslab.dsl.Definition

trait Context {
  type Ret[A]
}

trait Interpreter[Ctx <: Context, A] extends (Definition[Ctx, A] => Ctx#Ret[_])
