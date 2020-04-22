package com.virtuslab.iat.dsl

trait Named { self: Labeled =>
  def name: String = labels.find(l => l.key == "name").map(_.value).getOrElse("") // FIXME, should be compile time
}

trait Labeled {
  def labels: List[Label]
}
