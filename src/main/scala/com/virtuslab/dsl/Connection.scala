package com.virtuslab.dsl

case class Connection[A: Selectable, B: Selectable, C: Selectable](
    namespace: Namespace,
    resourceSelector: Selector[A],
    ingress: Selector[B],
    egress: Selector[C])
  extends Namespaced

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
