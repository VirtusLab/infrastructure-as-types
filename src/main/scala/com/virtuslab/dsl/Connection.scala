package com.virtuslab.dsl

case class Connection(
    namespace: Namespace,
    labels: Labels,
    resourceSelector: Selector,
    ingress: Selector,
    egress: Selector)
  extends Labeled
  with Namespaced

object Connection {

  def defaultName(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
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

  def apply(
      resourceSelector: Selector,
      ingress: Selector = EmptySelector,
      egress: Selector = EmptySelector
    )(implicit
      builder: NamespaceBuilder
    ): Connection = {
    val name = defaultName(resourceSelector, ingress, egress)
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
