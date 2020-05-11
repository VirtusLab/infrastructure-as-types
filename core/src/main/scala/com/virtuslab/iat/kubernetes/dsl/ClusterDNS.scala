package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.{ Identity, Named, Peer }

case class ClusterDNS(name: String, clusterComain: String = "svc.cluster.local.") extends Identity {
  def hostWith(ns: Namespace) = s"$name.${ns.name}.$clusterComain"
}

object ClusterDNS {
  def apply[A <: Peer[A]](namedPeer: Named with Peer[A]): ClusterDNS = new ClusterDNS(namedPeer.name)
}
