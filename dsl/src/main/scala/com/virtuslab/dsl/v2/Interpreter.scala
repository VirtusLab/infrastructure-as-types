package com.virtuslab.dsl.v2

import magnolia._

abstract class Interpreter[A, R] {
  def interpret(obj: A, ns: Namespace): List[Support[_, R]]
}

trait InterpreterDerivation[R] {
  type Typeclass[A] = Interpreter[A, R]

  //noinspection TypeAnnotation
  def combine[A](ctx: CaseClass[Typeclass, A]) = new Interpreter[A, R] {
    override def interpret(obj: A, ns: Namespace): List[Support[_, R]] = {
      val params = ctx.parameters.map { p =>
        p.typeclass.interpret(p.dereference(obj), ns)
      }
      params.flatten.toList
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = ???

  import scala.language.experimental.macros
  def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}
