package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume.{ Secret => SecretVolumeSource }

case class Secret private (labels: Labels, data: Map[String, String]) extends KeyValue with Transformable[Secret]

object Secret {
  implicit val mountSource: MountSource[Secret] = (obj: Secret) => {
    SecretVolumeSource(secretName = obj.name)
  }
}
