package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.Protocol.HasPort
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.Label.ops._

case class Application(
    labels: Seq[Label],
    containers: Seq[Container] = Nil,
    configurations: Seq[Configuration] = Nil,
    secrets: Seq[Secret] = Nil,
    mounts: Seq[Mount] = Nil)
  extends Named
  with Labeled
  with Containerized
  with Mounts
  with Patchable[Application]
  with Interpretable[Application]
  with Peer[Application] {
  def allPorts: Seq[HasPort] = containers.flatMap(_.ports)
  override def expressions: Expressions = Expressions(labels.asExpressions.toSet)
  override def protocols: Protocols = Protocols.ports(allPorts: _*)
  override def identities: Identities = Identities(ClusterDNS(this))
}

object Application {
  case class IsApplication(selection: Selection[Application])
  import scala.language.implicitConversions
  implicit def proofIsApplication(s: Selection[Application]): IsApplication = IsApplication(s)
}

case class Container(
    labels: Seq[Label],
    image: String,
    command: Seq[String] = Nil,
    args: Seq[String] = Nil,
    envs: Seq[(String, String)] = Nil,
    ports: Seq[HasPort] = Nil)
  extends Containerized.Container
