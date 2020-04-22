package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume.ConfigMapVolumeSource

case class Configuration private (labels: Labels, data: Map[String, String]) extends KeyValue with Transformable[Configuration] {
  def name: String = labels.name.value
}

object Configuration {
  implicit val mountSource: MountSource[Configuration] = (obj: Configuration) => {
    ConfigMapVolumeSource(name = obj.name)
  }
}
