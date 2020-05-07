package com.virtuslab.iat.scala

object function {
  object ops extends FunctionOps
}

object tuple {
  object ops extends TupleOps
}

object ops extends TupleOps with FunctionOps
