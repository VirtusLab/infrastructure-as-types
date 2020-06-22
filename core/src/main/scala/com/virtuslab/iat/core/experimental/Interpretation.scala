package com.virtuslab.iat.core.experimental

trait Interpretation[A] {
  def arguments: A
  def interpretedWith[B](interpreter: A => B): (A, A => B, B => B) = (arguments, interpreter, identity)
  def interpretedImplicitly[B](
      implicit
      interpreter: A => B
    ): (A, A => B, B => B) = interpretedWith(interpreter)
}

trait InterpretationWithContext[A, C] {
  def arguments: (A, C)
  def interpretedWith[B](interpreter: (A, C) => B): ((A, C), ((A, C)) => B, B => B) =
    (arguments, interpreter.tupled, identity)
  def interpretedImplicitly[B](
      implicit
      interpreter: (A, C) => B
    ): ((A, C), ((A, C)) => B, B => B) = interpretedWith(interpreter)
}

trait Details[A, B] {
  def interpretationContext: (A, A => B, B => B)
  def withDetails(details: B => B): (A, A => B, B => B) = {
    val (arguments, interpreter, oldDetails) = interpretationContext
    (arguments, interpreter, oldDetails.andThen(details))
  }

  def withDetails[B1, B2](
      details: (B1, B2) => (B1, B2)
    )(implicit
      ev: B =:= (B1, B2)
    ): (A, A => B, B => B) = withDetails(details.tupled.asInstanceOf[B => B])

  def withDetails[B1, B2, B3](
      details: (B1, B2, B3) => (B1, B2, B3)
    )(implicit
      ev: B =:= (B1, B2, B3)
    ): (A, A => B, B => B) = withDetails(details.tupled.asInstanceOf[B => B])

  // TODO to maybe B1..B5?
}

trait Evolution[A, B] {
  def current: (A, A => B, B => B)
  def evolutionTo[AT, BT](target: (AT, AT => BT, BT => BT)): Evolution.Pair[A, B, AT, BT] = Evolution(current, target)
}

object Evolution {
  case class Pair[A, B, AT, BT](current: (A, A => B, B => B), target: (AT, AT => BT, BT => BT))
  trait Strategy[A, B, AT, BT]

  def apply[A, B, AT, BT](current: (A, A => B, B => B), target: (AT, AT => BT, BT => BT)): Pair[A, B, AT, BT] =
    Pair(current, target)
}
