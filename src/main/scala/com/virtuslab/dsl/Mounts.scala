package com.virtuslab.dsl

trait Mounts {
  def mounts: List[Mount]
}

trait Mount
final case class KeyValueMount[A <: KeyValue](kv: A) extends Mount
final case class VolumeMount() extends Mount

trait Mountable[A] {}

object Mountable {}
