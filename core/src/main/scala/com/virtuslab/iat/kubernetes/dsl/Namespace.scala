package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.Label.ops._

case class Namespace(labels: Seq[Label])
  extends Named
  with Labeled
  with Patchable[Namespace]
  with Interpretable[Namespace]
  with Peer[Namespace] {
  override def expressions: Expressions = Expressions(labels.asExpressions.toSet)
  override def protocols: Protocols = Protocols.Any
  override def identities: Identities = Identities.Any
}

object Namespace {
  case class IsNamespace(selection: Selection[Namespace])
  import scala.language.implicitConversions
  implicit def proofIsNamespace(s: Selection[Namespace]): IsNamespace = IsNamespace(s)
}
