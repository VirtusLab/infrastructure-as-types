package com.virtuslab.dsl.v2

import magnolia.{ CaseClass, Magnolia, SealedTrait }

import scala.language.experimental.macros

trait Materializer[A] {
  def materialize[R](
      obj: A,
      namespace: Namespace
    )(implicit
      interpreter: Interpreter[A],
      transformer: Transformer[Interpreter[A]#R, R]
    ): Seq[R]
}

object Materializer {
  def apply[A]: Materializer[A] = new Materializer[A] {
    override def materialize[R](
        obj: A,
        namespace: Namespace
      )(implicit
        interpreter: Interpreter[A],
        transformer: Transformer[Interpreter[A]#R, R]
      ): Seq[R] = {
      transformer.apply(interpreter.interpret(obj, namespace))
    }
  }
}

trait MaterializerDerivation {
  type Typeclass[A] = Materializer[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override def materialize[R](
        obj: A,
        namespace: Namespace
      )(implicit
        interpreter: Interpreter[A],
        transformer: Transformer[Interpreter[A]#R, R]
      ): Seq[R] = {
      ctx.parameters.flatMap { p =>
        p.typeclass.materialize(p.dereference(obj), namespace)(???, ???)
      }
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = ???

  def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}
