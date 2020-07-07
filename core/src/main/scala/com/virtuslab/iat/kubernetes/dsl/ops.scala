package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.KeyValue
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]
}

object experimental {
  import com.virtuslab.iat.dsl.Interpretable

  implicit class NamespacedOps[A <: Interpretable[A]](obj: Interpretable[A]) {
    def inNamespace(ns: Namespace): (A, Namespace) = (obj.reference, ns)
  }
}
