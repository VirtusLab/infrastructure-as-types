package com.virtuslab.iat.scala

object function {
  object ops extends FunctionOps
}

object tuple {
  object ops extends TupleOps
}

object unit {
  object ops extends UnitOps
}

object ops extends TupleOps with FunctionOps
