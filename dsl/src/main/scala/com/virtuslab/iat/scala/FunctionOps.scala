package com.virtuslab.iat.scala

trait FunctionOps {
  implicit class FunctionOps1[A1, B1](f1: A1 => B1) {
    def merge[A2, B2](f2: A2 => B2): (A1, A2) => (B1, B2) = (a1: A1, a2: A2) => (f1(a1), f2(a2))
  }
}
