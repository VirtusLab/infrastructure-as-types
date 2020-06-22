package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.KeyValue
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]
}

object experimental {
  import com.virtuslab.iat.core.experimental._
  import com.virtuslab.iat.dsl.Interpretable

  implicit class NamespacedOps[A <: Interpretable[A]](obj: Interpretable[A]) {
    def inNamespace(ns: Namespace): (A, Namespace) = (obj.reference, ns)
  }

  implicit class InterpretationOps[A <: Interpretable[A]](val arguments: A) extends Interpretation[A]

  implicit class InterpretationOpsC[A <: Interpretable[A]](val arguments: (A, Namespace))
    extends InterpretationWithContext[A, Namespace]

  implicit class DetailsOps[A <: Interpretable[A], B](val interpretationContext: ((A, Namespace), ((A, Namespace)) => B, B => B))
    extends Details[(A, Namespace), B]

  implicit class EvolutionOps[A <: Interpretable[A], AT <: Interpretable[A], B](
      val current: ((A, Namespace), ((A, Namespace)) => B, B => B))
    extends Evolution[(A, Namespace), B]
}
