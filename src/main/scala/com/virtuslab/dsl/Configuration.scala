package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume.ConfigMapVolumeSource

case class Configuration private (labels: Labels, data: Map[String, String]) extends KeyValue with Transformable[Configuration]

object Configuration {
  implicit val mountSource: MountSource[Configuration] = (obj: Configuration) => {
    ConfigMapVolumeSource(name = obj.name)
  }
}
