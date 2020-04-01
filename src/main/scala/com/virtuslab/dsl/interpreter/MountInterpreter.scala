package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.{ KeyValueMount, Mount, RawMount }
import skuber.Volume

trait MountInterpreter extends ((Mount) => (Volume, Volume.Mount)) {
  def apply(mount: Mount): (Volume, Volume.Mount) = mount match {
    case RawMount(name, mountPath) => ???
    case KeyValueMount(name, key, mountPath, source) =>
      val volume = Volume(name, source)
      val volumeMount = Volume.Mount(name = name, mountPath = mountPath.toString, subPath = key)

      volume -> volumeMount
  }
}

object MountInterpreter extends MountInterpreter
