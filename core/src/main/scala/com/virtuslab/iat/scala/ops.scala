package com.virtuslab.iat.scala

object function {
  object ops extends FunctionOps
}

object tuple {
  object ops extends TupleOps
}

object ops extends TupleOps with FunctionOps {
  type Not[T] = T => Nothing
  type =!=[A, B] = A =:= Not[B]
}
