package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl.kubernetes.Mountable.MountSource
import com.virtuslab.iat.dsl._
import skuber.Volume.ConfigMapVolumeSource

case class Configuration(labels: List[Label], data: Map[String, String])
  extends Named
  with Labeled
  with KeyValue
  with Patchable[Configuration]
  with Interpretable[Configuration]

object Configuration {
  implicit val mountSource: MountSource[Configuration] = (obj: Configuration) => {
    ConfigMapVolumeSource(name = obj.name)
  }
}
