package com.virtuslab.dsl.v2

import magnolia._

abstract class Interpreter[A, R] {
  def interpret(obj: A, ns: Namespace): List[Support[_ /* reified in the Support */, R]]
}

trait InterpreterDerivation[R] {
  type Typeclass[A] = Interpreter[A, R]

  //noinspection TypeAnnotation
  def combine[A](ctx: CaseClass[Typeclass, A]): Interpreter[A, R] = (obj: A, ns: Namespace) => {
    ctx.parameters.flatMap { p =>
      p.typeclass.interpret(p.dereference(obj), ns)
    }.toList
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = ???

  import scala.language.experimental.macros
  def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}
