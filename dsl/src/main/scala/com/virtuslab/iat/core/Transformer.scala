package com.virtuslab.iat.core

import com.virtuslab.iat.core.Transformable.Transformer

import scala.annotation.implicitNotFound

trait Support[P, R] {
  def product: P
  def result: R
}

object Support {
  def apply[P, R](p: P)(implicit transformer: Transformer[P, R]): Support[P, R] = {
    new Support[P, R] {
      override def product: P = p
      override def result: R = transformer(product).transform
    }
  }
}

@implicitNotFound("""
    |implicit for Transformable[P=${P}, R=${R}] not found,
    |make sure you have a correct implicit imported
    |or otherwise available in scope""".stripMargin)
trait Transformable[P, R] {
  def transform: R
}

object Transformable {
  type Transformer[P, R] = P => Transformable[P, R]
}
