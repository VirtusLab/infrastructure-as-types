package com.virtuslab.iat.scala

trait TupleOps {
  import function.ops._

  implicit class TupleOps1[A1](t: Tuple1[A1]) {
    def map[B1](f1: A1 => B1): Tuple1[B1] = Tuple1(f1(t.untupled))
    def reduce[B](f: A1 => B): B = f(t.untupled)
    def untupled: A1 = t._1
  }

  implicit class TupleOps2[A1, A2](t: (A1, A2)) {
    def map[B1, B2](f1: A1 => B1, f2: A2 => B2): (B1, B2) = map(f1.merge(f2))
    def map[B1, B2](f1: (A1, A2) => (B1, B2)): (B1, B2) = f1.tupled(t)
    def map[B1, B2](f1: ((A1, A2)) => (B1, B2)): (B1, B2) = f1(t)
    def reduce[B](f: (A1, A2) => B): B = f.tupled(t)
    def reduce[B](f: ((A1, A2)) => B): B = f(t)
  }
}
