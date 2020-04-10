package com.virtuslab.interpreter

import com.virtuslab.dsl.Definition
import scala.language.higherKinds

trait Context {
  type Ret[A]
}

trait Interpreter[Ctx <: Context, A] extends (Definition[Ctx, A] => Ctx#Ret[_])
