package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.{ Identity, Named, Peer }
import com.virtuslab.iat.kubernetes.dsl.ClusterDNS.defaultClusterDomain

case class ClusterDNS(
    name: String,
    namespace: String,
    clusterDomain: String = defaultClusterDomain)
  extends Identity {
  def host = s"$name.$namespace.$clusterDomain"
}

case class PartialClusterDNS(name: String, clusterDomain: String = defaultClusterDomain) extends Identity {
  def withNamespace(ns: Namespace): ClusterDNS = ClusterDNS(
    name = this.name,
    namespace = ns.name,
    clusterDomain = this.clusterDomain
  )
}

object ClusterDNS {
  val defaultClusterDomain = "svc.cluster.local."
  def apply[A <: Peer[A]](namedPeer: Named with Peer[A]): PartialClusterDNS = PartialClusterDNS(namedPeer.name)
}
