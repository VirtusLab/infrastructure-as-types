package com.virtuslab.iat.kubernetes.dsl

import java.nio.file.Path

import com.virtuslab.iat.dsl.KeyValue

trait Mount {
  def name: String
  def mountPath: Path
}

final case class KeyValueMount[S](
    name: String,
    key: String,
    mountPath: Path,
    source: S)
  extends Mount

trait Mounts {
  def mounts: List[Mount]
}

object Mountable {
  trait MountSource[A, S] {
    def source(obj: A): S
  }

  trait KeyValueMountableOps[A <: KeyValue] {
    def obj: A
    def mount[S](
        name: String,
        key: String,
        as: Path
      )(implicit
        mountSource: MountSource[A, S]
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
