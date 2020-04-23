package com.virtuslab.iat.core

import magnolia._

trait RootInterpreter[A, P, R] extends Interpreter[A, Nothing, P, R] {
  def interpret(obj: A): List[Support[_ <: P /* reified in the Support */, R]]
  override def interpret(obj: A, ctx: Nothing): List[Support[_ <: P, R]] = interpret(obj)
}

trait Interpreter[A, C, P, R] {
  def interpret(obj: A, ctx: C): List[Support[_ <: P /* reified in the Support */, R]]
}

trait InterpreterDerivation[C, P, R] {
  type Typeclass[A] = Interpreter[A, C, P, R]

  def combine[A](caseClass: CaseClass[Typeclass, A]): Typeclass[A] = (obj: A, ctx: C) => {
    caseClass.parameters.flatMap { p =>
      p.typeclass.interpret(p.dereference(obj), ctx)
    }.toList
  }

  def dispatch[A](sealedTrait: SealedTrait[Typeclass, A]): Typeclass[A] = (obj: A, ctx: C) => {
    sealedTrait.dispatch(obj) { sub =>
      sub.typeclass.interpret(sub.cast(obj), ctx)
    }
  }

  import scala.language.experimental.macros
  implicit def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}

object Interpreter {
  def interpret[A, P, R](obj: A)(implicit i: RootInterpreter[A, P, R]): List[R] =
    i.interpret(obj).map(_.transform)
  def interpret[A, C, P, R](obj: A, ctx: C)(implicit i: Interpreter[A, C, P, R]): List[R] =
    i.interpret(obj, ctx).map(_.transform)
}
