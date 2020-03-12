package com.virtuslab.dsl

class Connection[A: Selectable, B: Selectable, C: Selectable](
    namespace: Namespace,
    resourceSelector: Selector[A],
    ingress: Selector[B],
    egress: Selector[C])

object Connection {
  def apply[A: Selectable, B: Selectable, C: Selectable](
      resourceSelector: Selector[A],
      ingress: Selector[B] = EmptySelector,
      egress: Selector[C] = EmptySelector
    )(implicit
      ns: Namespace
    ): Connection[A, B, C] =
    new Connection(ns, resourceSelector, ingress, egress)
}
