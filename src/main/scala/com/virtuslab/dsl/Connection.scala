package com.virtuslab.dsl

case class Connection[A <: Selectable, B <: Selectable, C <: Selectable](
    namespace: Namespace,
    labels: Labels,
    resourceSelector: Selector[A],
    ingress: Selector[B],
    egress: Selector[C])
  extends Labeled
  with Namespaced

object Connection {

  def defaultName[A <: Selectable, B <: Selectable, C <: Selectable](
      resourceSelector: Selector[A],
      ingress: Selector[B],
      egress: Selector[C]
    ): String = {
    val name = resourceSelector.selectable.asShortString +
      "-" + ingress.selectable.asShortString +
      "-" + egress.selectable.asShortString
    val (isValid, msg) = Validation.IsQualifiedName(name)
    if (!isValid) {
      throw new IllegalStateException(s"Generated name '$name' is invalid: $msg")
    }
    name
  }

  def apply[A <: Selectable, B <: Selectable, C <: Selectable](
      resourceSelector: Selector[A],
      ingress: Selector[B] = EmptySelector,
      egress: Selector[C] = EmptySelector
    )(implicit
      builder: NamespaceBuilder
    ): Connection[A, B, C] = {
    val name = defaultName[A, B, C](resourceSelector, ingress, egress)
    val conn = Connection(
      namespace = builder.namespace,
      labels = Labels(Name(name)),
      resourceSelector = resourceSelector,
      ingress = ingress,
      egress = egress
    )
    builder.connections(conn)
    conn
  }
}
