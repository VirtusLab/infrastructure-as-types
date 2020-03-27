package com.virtuslab.dsl

trait Connection {
  def resourceSelector: Selector
  def ingress: Selector
  def egress: Selector
}

object Connection {
  case class ConnectionDraft(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector)
    extends Connection
    with Transformable[ConnectionDraft]
    with Definable[Connection, ConnectionDraft, ConnectionDefinition] {

    def asDefault(implicit builder: NamespaceBuilder): ConnectionDefinition = {
      named(defaultName(resourceSelector, ingress, egress))
    }

    def named(name: String)(implicit builder: NamespaceBuilder): ConnectionDefinition =
      labeled(Labels(Name(name)))

    def labeled(labels: Labels)(implicit builder: NamespaceBuilder): ConnectionDefinition =
      ConnectionDefinition(
        labels = labels,
        namespace = builder.namespace,
        resourceSelector = resourceSelector,
        ingress = ingress,
        egress = egress
      )
  }

  case class ConnectionDefinition(
      namespace: Namespace,
      labels: Labels,
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector)
    extends Connection
    with Labeled
    with Namespaced

  def apply(
      name: String,
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    )(implicit
      builder: NamespaceBuilder
    ): ConnectionDefinition = {
    val conn = ConnectionDraft(
      resourceSelector,
      ingress,
      egress
    ).named(name)
    builder.references(conn)
    conn
  }

  def apply(
      labels: Labels,
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    )(implicit
      builder: NamespaceBuilder
    ): ConnectionDefinition = {
    val conn = ConnectionDraft(
      resourceSelector,
      ingress,
      egress
    ).labeled(labels)
    builder.references(conn)
    conn
  }

  def defaultName(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): String = {
    val name = resourceSelector.selectable.asShortString +
      "-" + ingress.selectable.asShortString +
      "-" + egress.selectable.asShortString // 20*3 + 2 < 64
    val (isValid, msg) = Validation.IsQualifiedName(name)
    if (!isValid) {
      throw new IllegalStateException(s"Generated name '$name' is invalid: $msg")
    }
    name
  }
}
