package com.virtuslab.iat.core

import magnolia._

trait RootInterpreter[A, R] extends Interpreter[A, Nothing, R] {
  def interpret(obj: A): List[Support[_ /* reified in the Support */, R]]
  override def interpret(obj: A, ctx: Nothing): List[Support[_, R]] = interpret(obj)
}

trait Interpreter[A, C, R] {
  def interpret(obj: A, ctx: C): List[Support[_ /* reified in the Support */, R]]
}

trait InterpreterDerivation[C, R] {
  type Typeclass[A] = Interpreter[A, C, R]

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
  def interpret[A, R](obj: A)(implicit i: RootInterpreter[A, R]): List[Support[_, R]] = i.interpret(obj)
  def interpret[A, C, R](obj: A, ctx: C)(implicit i: Interpreter[A, C, R]): List[Support[_, R]] = i.interpret(obj, ctx)
}
