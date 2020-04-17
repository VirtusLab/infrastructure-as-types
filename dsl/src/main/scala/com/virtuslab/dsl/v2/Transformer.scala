package com.virtuslab.dsl.v2

import magnolia._

import scala.language.experimental.macros

trait Transformer[A, B] extends (A => Seq[B])

trait TransformerDerivation[R] {
  type Typeclass[A] = Transformer[A, R]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] =
    (obj: A) => {
      ctx.parameters
        .flatMap { p =>
          p.typeclass(p.dereference(obj))
        }
    }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = ???

  implicit def gen[A]: Transformer[A, R] = macro Magnolia.gen[A]
}
