package com.virtuslab.dsl

abstract class Connection[A: Selectable, B: Selectable, C: Selectable] extends Namespaced {
  def namespace: Namespace
  def resourceSelector: Selector[A]
  def ingress: Selector[B]
  def egress: Selector[C]
}

object Connection {
  final case class DefinedConnection[A: Selectable, B: Selectable, C: Selectable](
      namespace: Namespace,
      resourceSelector: Selector[A],
      ingress: Selector[B],
      egress: Selector[C])
    extends Connection[A, B, C]
    with Namespaced

  def apply[A: Selectable, B: Selectable, C: Selectable](
      resourceSelector: Selector[A],
      ingress: Selector[B] = EmptySelector,
      egress: Selector[C] = EmptySelector
    )(implicit
      builder: NamespaceBuilder
    ): DefinedConnection[A, B, C] = {
    val conn = DefinedConnection(builder.namespace, resourceSelector, ingress, egress)
    builder.connections(conn)
    conn
  }
}
