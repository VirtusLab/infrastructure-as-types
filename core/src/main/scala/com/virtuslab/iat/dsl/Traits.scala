package com.virtuslab.iat.dsl

trait Named { self: Labeled =>
  def name: String = labels.find(l => l.key == "name").map(_.value).getOrElse("") // TODO, make it compile time

  require(labels.exists(_.key == "name"))
}

trait Labeled {
  def labels: List[Label]

  require(labels.nonEmpty)
}

trait Patchable[A] { self: A =>
  def patch(f: A => A): A = f(self)
}

trait Interpretable[A] { self: A =>
  def reference: A = self
}
