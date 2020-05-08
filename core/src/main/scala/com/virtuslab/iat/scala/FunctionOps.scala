package com.virtuslab.iat.scala

trait Function1I1[A1, I1, R] {
  def apply(a1: A1)(implicit i1: I1): R
}

trait Function2I1[A1, A2, I1, R] {
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
    def merge[B2](f2: (A1, A2) => B2): (A1, A2) => (B1, B2) = (a1: A1, a2: A2) => (f1(a1, a2), f2(a1, a2))
  }
}
