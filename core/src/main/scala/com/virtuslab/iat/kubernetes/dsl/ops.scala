package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.{ Expressions, KeyValue, Label, Protocol, Protocols, TCP }
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]

  implicit class ApplicationConnectionOps(app: Application) {
    import Label.ops._

    def communicatesWith(other: Application): ConnectionBuilder = {
      communicatesWith(
        SelectedApplications(
          Expressions(other.labels.asExpressions: _*),
          Protocols.port(other.containers.flatMap(_.ports).map(TCP(_)): _*)
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
      // FIXME: un-HACK-me, hardcoded TCP
      val appPorts: Seq[Protocol.HasPort] = app.containers.flatMap(_.ports).map(TCP(_))
      val appProtocols = Protocols.port(appPorts: _*)
      ConnectionBuilder(
        resourceSelector = SelectedApplications(
          Expressions(app.labels.asExpressions: _*),
          appProtocols
        ),
        ingress = other,
        egress = other
      )
    }
  }
}
