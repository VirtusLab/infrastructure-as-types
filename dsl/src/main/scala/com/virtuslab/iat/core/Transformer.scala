package com.virtuslab.iat.core

import scala.annotation.implicitNotFound

trait Support[P, R] extends Resource[P] with Result[R] {
  def product: P
  def result: R
}

trait Resource[P] {
  def product: P
}

trait Result[R] {
  def result: R
}

object Support {
  def apply[P, R](p: P)(implicit transformer: Transformer[P, R]): Support[P, R] = {
    new Support[P, R] {
      override def product: P = p
      override def result: R = transformer(product)
    }
  }
}

@implicitNotFound("""
    |implicit for Transformer[P=${P}, R=${R}] not found,
    |make sure you have a correct implicit imported
    |or otherwise available in scope""".stripMargin)
trait Transformer[P, R] extends (P => R)
