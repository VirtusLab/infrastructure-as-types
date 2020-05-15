package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.Label.ops._

case class Namespace(labels: List[Label]) extends Named with Labeled with Interpretable[Namespace] with Peer[Namespace] {
  override def expressions: Expressions = Expressions(labels.asExpressions.toSet)
  override def protocols: Protocols = Protocols.Any
  override def identities: Identities = Identities.Any
}
