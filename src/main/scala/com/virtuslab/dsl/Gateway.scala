package com.virtuslab.dsl

trait Gateway extends Labeled {
  def protocols: Protocols
}

object Gateway {
  case class GatewayReference(labels: Labels, protocols: Protocols)
    extends Gateway
    with Transformable[GatewayReference]
    with Definable[Gateway, GatewayReference, GatewayDefinition] {
    def define(implicit builder: NamespaceBuilder): GatewayDefinition = {
      GatewayDefinition(
        labels = labels,
        namespace = builder.namespace,
        protocols = protocols
      )
    }
  }

  case class GatewayDefinition(
      labels: Labels,
      namespace: Namespace,
      protocols: Protocols)
    extends Gateway
    with Namespaced

  def ref(labels: Labels, protocols: Protocols)(implicit builder: SystemBuilder): GatewayReference = {
    val gw = GatewayReference(labels, protocols)
    builder.references(gw)
    gw
  }

  def apply(labels: Labels, protocols: Protocols)(implicit builder: NamespaceBuilder): GatewayDefinition = {
    ref(labels, protocols)(builder.systemBuilder).define
  }
}
