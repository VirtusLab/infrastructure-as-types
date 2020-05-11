package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.{ Expressions, KeyValue, Label, Peer, Protocols }
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]

  implicit class PeerConnectionOps[A](peer: Peer[A]) {
    import Label.ops._

    def communicatesWith[B](other: Peer[B]): ConnectionBuilder = {
      communicatesWith(
        SelectedApplications(
          other.expressions,
          other.protocols
        )
      )
    }

    def communicatesWith(other: Namespace): ConnectionBuilder = {
      communicatesWith(
        SelectedNamespaces(
          Expressions(other.labels.asExpressions: _*),
          Protocols.Any
        )
      )
    }

    def communicatesWith(other: Selector): ConnectionBuilder = {
      ConnectionBuilder(
        resourceSelector = SelectedApplications(
          peer.expressions,
          peer.protocols
        ),
        ingress = other,
        egress = other
      )
    }
  }
}
