package com.virtuslab.iat.scala

trait TupleOps {
  import function.ops._
  /*
  implicit class TupleOps1[A1](t: Tuple1[A1]) {
    def map[B1](f1: A1 => B1): Tuple1[B1] = Tuple1(f1(t.untupled))
    def reduce[B](f: A1 => B): B = f(t.untupled)
    def untupled: A1 = t._1
    def append[A2](a2: A2): (A1, A2) = (t.untupled, a2)
    def append[A2](a2: Tuple1[A2]): (A1, A2) = (t.untupled, a2.untupled)
  }*/

  implicit class TupleOps2[A1, A2](t: (A1, A2)) {
    def map[B1, B2](f1: A1 => B1, f2: A2 => B2): (B1, B2) = map(f1.merge(f2))
    def map[B1, B2](f1: (A1, A2) => (B1, B2)): (B1, B2) = f1.tupled(t)
    def map[B1, B2](f1: ((A1, A2)) => (B1, B2)): (B1, B2) = f1(t)
    def map[B1, B2](fs: (A1 => B1, A2 => B2)): (B1, B2) = map(fs._1.merge(fs._2))

    def map[B1](
        f1: A1 => B1
      )(implicit
        d: DummyImplicit // polymorphism hack
      ): (B1, A2) = (f1(t._1), t._2)
    def map[B2](
        f2: A2 => B2
      )(implicit
        d1: DummyImplicit,
        d2: DummyImplicit // polymorphism hack
      ): (A1, B2) = (t._1, f2(t._2))

    def reduce[B](f: (A1, A2) => B): B = f.tupled(t)
    def reduce[B](f: ((A1, A2)) => B): B = f(t)
    def append[A3](a3: A3): (A1, A2, A3) = (t._1, t._2, a3)
  }
}
