package com.virtuslab.materializer

import com.virtuslab.interpreter.Context
import org.json4s.JValue

trait Resource[Ctx <: Context, A <: Ctx#Base] {
  def meta: Ctx#Meta
  def obj: A
  def asJValue: JValue
  def weak: Resource[Ctx, Ctx#Base]
}
