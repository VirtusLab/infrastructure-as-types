package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.Label.ops._

case class Gateway(
    labels: Seq[Label],
    inputs: Protocols = Protocols.Any,
    outputs: Protocols = Protocols.None)
  extends Named
  with Labeled
  with Patchable[Gateway]
  with Interpretable[Gateway]
  with Peer[Gateway] {
  override def expressions: Expressions = Expressions(labels.asExpressions.toSet)
  override def identities: Identities = Identities.Any
  override def protocols: Protocols = inputs
}
