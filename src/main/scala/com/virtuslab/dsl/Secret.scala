package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume
import skuber.Volume.{ Secret => SecretVolumeSource }

trait Secret extends KeyValue

object Secret {

  final case class SecretDefinition(
      labels: Labels,
      namespace: Namespace,
      data: Map[String, String])
    extends Secret

  object SecretDefinition {
    implicit val mountSource = new MountSource[SecretDefinition] {
      override def source(obj: SecretDefinition): Volume.Source = {
        SecretVolumeSource(secretName = obj.name)
      }
    }
  }

  final case class SecretReference(labels: Labels, data: Map[String, String]) extends Secret with Definable[Secret, SecretReference, SecretDefinition] {
    def define(implicit builder: NamespaceBuilder): SecretDefinition = {
      SecretDefinition(
        labels = labels,
        namespace = builder.namespace,
        data = data
      )
    }
  }

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
}
