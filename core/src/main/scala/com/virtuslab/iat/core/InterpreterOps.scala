package com.virtuslab.iat.core

import com.virtuslab.iat.dsl.Interpretable

trait InterpreterOps[Base] {
  trait RootInterpreterOps1[A, B1 <: Base] {
    def obj: Interpretable[A]
    def interpret(
        implicit
        interpreter: A => B1
      ): B1 = interpreter(obj.reference)
  }

  trait InterpreterOps1[A, C, B1 <: Base] {
    def obj: Interpretable[A]
    def interpretWith(
        ctx: C
      )(implicit
        interpreter: (A, C) => B1
      ): B1 = interpreter(obj.reference, ctx)
  }

  trait InterpreterOps2[A, C, B1 <: Base, B2 <: Base] {
    def obj: Interpretable[A]
    def interpretWith(
        ctx: C
      )(implicit
        interpreter: (A, C) => (B1, B2)
      ): (B1, B2) = interpreter(obj.reference, ctx)
  }

  trait Contextualized1[A <: Interpretable[A], C, B1 <: Base] {
    def arguments: (A, C)
    def obj: A = arguments._1
    def ctx: C = arguments._2
    def interpret(
        implicit
        interpreter: (A, C) => B1
      ): B1 = interpreter(obj.reference, ctx)
  }

  trait Contextualized2[A <: Interpretable[A], C, B1 <: Base, B2 <: Base] {
    def arguments: (A, C)
    def obj: A = arguments._1
    def ctx: C = arguments._2
    def interpret(
        implicit
        interpreter: (A, C) => (B1, B2)
      ): (B1, B2) = interpreter(obj.reference, ctx)
  }
}
