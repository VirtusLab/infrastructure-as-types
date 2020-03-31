package com.virtuslab.dsl

import java.nio.file.Path

trait Mount {
  def name: String
  def mountPath: Path
}
final case class RawMount(name: String, mountPath: Path) extends Mount
final case class KeyValueMount[A <: KeyValue](
    name: String,
    key: String,
    mountPath: Path,
    underlying: A)
  extends Mount

trait Mounts {
  def mounts: List[Mount]
}

object Mountable {
  implicit class KeyValueMountableOps[A <: KeyValue](obj: A) {
    def mount(
        name: String,
        key: String,
        as: Path
      ): Mount = {
      KeyValueMount(
        name = name,
        key = key,
        mountPath = as,
        underlying = obj
      )
    }
  }
}
