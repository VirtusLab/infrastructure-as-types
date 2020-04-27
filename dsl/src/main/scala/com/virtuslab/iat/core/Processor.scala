package com.virtuslab.iat.core

trait Processor[A <: BaseA, BaseA, B <: BaseB, BaseB, P[X <: BaseA] <: Resource[X], R[Y <: BaseB] <: Result[Y]] {
  def process(p: P[A]): R[B]
}
