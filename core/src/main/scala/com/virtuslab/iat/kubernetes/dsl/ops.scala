package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.KeyValue
import com.virtuslab.iat.kubernetes.dsl.Mountable.KeyValueMountableOps

object ops {
  implicit class AKeyValueMountableOps[A <: KeyValue](val obj: A) extends KeyValueMountableOps[A]
}
