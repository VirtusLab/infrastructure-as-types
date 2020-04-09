package com.virtuslab.dsl

case class Connection(
    labels: Labels,
    resourceSelector: Selector,
    ingress: Selector,
    egress: Selector)
  extends Labeled
  with Transformable[Connection]

object Connection {
  def apply(
      name: String,
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): Connection = Connection(
    Labels(Name(name)),
    resourceSelector,
    ingress,
    egress
  )

  def apply(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): Connection = Connection(
    Labels(Name(Connection.defaultName(resourceSelector, ingress, egress))),
    resourceSelector,
    ingress,
    egress
  )

  def defaultName(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): String = {
    val name = resourceSelector.asShortString +
      "-" + ingress.asShortString +
      "-" + egress.asShortString // 20*3 + 2 < 64
    val (isValid, msg) = Validation.IsQualifiedName(name)
    if (!isValid) {
      throw new IllegalStateException(s"Generated name '$name' is invalid: $msg")
    }
    name
  }
}
