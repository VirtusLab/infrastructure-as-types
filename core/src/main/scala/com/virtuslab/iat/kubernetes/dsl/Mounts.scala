package com.virtuslab.iat.kubernetes.dsl

import java.nio.file.Path

import com.virtuslab.iat.dsl.KeyValue

trait Mount {
  def name: String
  def mountPath: Path
}

// FIXME not sure this is the right approach, feels unnecessary any heavy
final case class KeyValueMount[S](
    name: String,
    mountPath: Path,
    key: String,
    readOnly: Boolean,
    source: S)
  extends Mount

trait Mounts {
  def mounts: Seq[Mount]
}

object Mountable {
  trait MountSource[A, S] {
    def source(obj: A): S
  }

  trait KeyValueMountableOps[A <: KeyValue] {
    def obj: A
    def mount[S](
        name: String,
        path: Path,
        key: String = "",
        readOnly: Boolean = true
      )(implicit
        mountSource: MountSource[A, S]
      ): Mount = {
      KeyValueMount(
        name = name,
        key = key,
        mountPath = path,
        readOnly = readOnly,
        source = mountSource.source(obj)
      )
    }
  }
}
