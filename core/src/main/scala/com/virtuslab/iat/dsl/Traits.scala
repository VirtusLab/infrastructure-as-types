package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Label.Name

trait Named { self: Labeled =>
  def name: String = labels.collectFirst { case Name(v) => v }.getOrElse("") // TODO, make it compile time
}

trait Labeled {
  def labels: List[Label]
}

trait Patchable[A] { self: A =>
  def patch(f: A => A): A = f(self)
}

trait Interpretable[A] extends Reference[A] { self: A =>
}

trait Reference[A] { self: A =>
  def reference: A = self
}
