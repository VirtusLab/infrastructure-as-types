package com.virtuslab.dsl.v2

import magnolia._

import scala.language.experimental.macros

object SeqToParams {
  def apply[T](seq: Seq[T]): Any = {
    seq match {
      case Seq()           => ()
      case Seq(v1)         => v1
      case Seq(v1, v2)     => (v1, v2)
      case Seq(v1, v2, v3) => (v1, v2, v3)
      case _               => throw new IllegalArgumentException(s"Cannot convert $seq to params!")
    }
  }
}

trait Interpreter[A] {
  type R
  def interpret(obj: A, ns: Namespace): R
}

trait InterpreterDerivation {
  type Typeclass[A] = Interpreter[A]

  def combine[A, Ret](ctx: CaseClass[Typeclass, A]): Typeclass[A] = new Typeclass[A] {
    override type R = Ret

    override def interpret(obj: A, ns: Namespace): R = {
      val params = ctx.parameters.map { p =>
        p.typeclass.interpret(p.dereference(obj), ns)
      }

      SeqToParams(params).asInstanceOf[Ret]
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = ???

  def gen[A]: Typeclass[A] = macro Magnolia.gen[A]
}
