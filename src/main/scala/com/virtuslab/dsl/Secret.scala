package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume.{ Secret => SecretVolumeSource }

case class Secret private (labels: Labels, data: Map[String, String]) extends KeyValue with Transformable[Secret]

object SecretDefinition {
  implicit val mountSource: MountSource[Secret] = (obj: Secret) => {
    SecretVolumeSource(secretName = obj.name)
  }

  /*
  def ref(
      labels: Labels,
      data: Map[String, String]
    )(implicit
      builder: SystemBuilder
    ): SecretReference = {
    val conf = SecretReference(
      labels = labels,
      data = data
    )
    builder.references(conf)
    conf
  }

  def apply(
      labels: Labels,
      data: Map[String, String]
    )(implicit
      builder: NamespaceBuilder
    ): SecretDefinition = {
    val conf = ref(
      labels = labels,
      data = data
    )(builder.systemBuilder).define
    builder.references(conf)
    conf
  }
 */
}
