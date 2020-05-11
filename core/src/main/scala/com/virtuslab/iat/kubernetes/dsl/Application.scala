package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.Protocol.HasPort
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.Label.ops._

case class Application(
    labels: List[Label],
    containers: List[Container] = Nil,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil,
    mounts: List[Mount] = Nil)
  extends Named
  with Labeled
  with Containerized
  with Mounts
  with Patchable[Application]
  with Interpretable[Application]
  with Peer[Application] {
  def allPorts: List[HasPort] = containers.flatMap(_.ports)
  override def expressions: Expressions = Expressions(labels.asExpressions.toSet)
  override def protocols: Protocols = Protocols.port(allPorts: _*)
  override def identities: Identities = Identities(ClusterDNS(this))
}

case class Container(
    labels: List[Label],
    image: String,
    command: List[String] = Nil,
    args: List[String] = Nil,
    envs: List[(String, String)] = Nil,
    ports: List[HasPort] = Nil)
  extends Containerized.Container
