package com.virtuslab.materializer

import com.virtuslab.interpreter.Context

trait Materializer[Ctx <: Context, B] extends (Ctx#Interpretation => B)
