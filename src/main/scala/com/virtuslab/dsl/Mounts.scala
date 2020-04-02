package com.virtuslab.dsl

import java.nio.file.Path

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume.Source

trait Mount {
  def name: String
  def mountPath: Path
}
final case class KeyValueMount[A <: KeyValue: MountSource](
    name: String,
    key: String,
    mountPath: Path,
    source: Source)
  extends Mount

trait Mounts {
  def mounts: List[Mount]
}

object Mountable {
  trait MountSource[A] {
    def source(obj: A): Source
  }

  implicit class KeyValueMountableOps[A <: KeyValue](obj: A) {
    def mount(
        name: String,
        key: String,
        as: Path
      )(implicit
        mountSource: MountSource[A]
      ): Mount = {
      KeyValueMount(
        name = name,
        key = key,
        mountPath = as,
        source = mountSource.source(obj)
      )
    }
  }
}
