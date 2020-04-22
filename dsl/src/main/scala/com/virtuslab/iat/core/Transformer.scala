package com.virtuslab.iat.core

import com.virtuslab.iat.core.Transformable.Transformer

import scala.annotation.implicitNotFound

sealed trait Support[P, R] {
  def transform: R = transformable.transform
  protected def transformable: Transformable[P, R] // available at construction
}

object Support {
  def apply[P, R](product: P)(implicit transformer: Transformer[P, R]): Support[P, R] = {
    new Support[P, R] {
      override def transformable: Transformable[P, R] = transformer(product)
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
  type Transformer[A, B] = A => Transformable[A, B]
}
