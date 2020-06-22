package com.virtuslab.iat.scala

trait Function1I1[-A1, I1, +R] {
  def apply(a1: A1)(implicit i1: I1): R
}

trait Function1I2[-A1, I1, I2, +R] {
  def apply(a1: A1)(implicit i1: I1, i2: I2): R
}

trait Function1I3[-A1, I1, I2, I3, +R] {
  def apply(
      a1: A1
    )(implicit
      i1: I1,
      i2: I2,
      i3: I3
    ): R
}

trait Function2I1[-A1, -A2, I1, +R] {
  def apply(a1: A1, a2: A2)(implicit i1: I1): R
}

trait FunctionOps {
  implicit class FunctionOps1to1[A1, B1](f1: A1 => B1) {
    def merge[A2, B2](f2: A2 => B2): (A1, A2) => (B1, B2) = (a1: A1, a2: A2) => (f1(a1), f2(a2))

    def mergeImplicit[A2, B2](f2: A2 => B2): Function1I1[A1, A2, (B1, B2)] =
      new Function1I1[A1, A2, (B1, B2)] {
        override def apply(a1: A1)(implicit a2: A2): (B1, B2) = (f1(a1), f2(a2))
      }
  }

  implicit class FunctionOps2to1[A1, A2, B1](f1: (A1, A2) => B1) {
    def andThen[C1](g: B1 => C1): (A1, A2) => C1 = f1.tupled.andThen(g)(_, _)
    def swap: (A2, A1) => B1 = (a2: A2, a1: A1) => f1(a1, a2)
    def merge[B2](f2: (A1, A2) => B2): (A1, A2) => (B1, B2) = (a1: A1, a2: A2) => (f1(a1, a2), f2(a1, a2))
  }

  implicit class FunctionOps2to2[A1, A2, B1, B2](f1: (A1, A2) => (B1, B2)) {
    def andThen[C1, C2](g: (B1, B2) => (C1, C2)): (A1, A2) => (C1, C2) = f1.tupled.andThen(g.tupled)(_, _)
    def swap: (A2, A1) => (B2, B1) = (a2: A2, a1: A1) => f1(a1, a2).swap
  }
}
