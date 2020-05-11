package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.{ Expressions, KeyValue, Label, Protocols }
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]

  implicit class ApplicationConnectionOps(app: Application) {
    import Label.ops._

    def communicatesWith(other: Application): ConnectionBuilder = {
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
          app.expressions,
          app.protocols
        ),
        ingress = other,
        egress = other
      )
    }
  }
}
