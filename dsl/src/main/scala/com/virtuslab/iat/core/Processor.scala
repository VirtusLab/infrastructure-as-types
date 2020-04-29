package com.virtuslab.iat.core

import scala.annotation.implicitNotFound

@implicitNotFound("""Found no Processor[${A}] implicitly, please make sure you have all the correct imports.
The requested processor takes ${A} <: ${BaseA}
and returns ${B} <: ${BaseB}.
The ${A} is wrapped in ${P}
and ${B} is wrapped in ${R}""")
trait Processor[A <: BaseA, BaseA, B <: BaseB, BaseB, P[X <: BaseA] <: Resource[X], R[Y <: BaseB] <: Result[Y]] {
  def process(p: P[A]): R[B]
}
