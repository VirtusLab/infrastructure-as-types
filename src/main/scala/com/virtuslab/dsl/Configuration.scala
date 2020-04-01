package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume
import skuber.Volume.ConfigMapVolumeSource

trait Configuration extends KeyValue

object Configuration {
  final case class ConfigurationDefinition(
      labels: Labels,
      namespace: Namespace,
      data: Map[String, String])
    extends Configuration
    with Namespaced

  object ConfigurationDefinition {
    implicit val mountSource = new MountSource[ConfigurationDefinition] {
      override def source(obj: ConfigurationDefinition): Volume.Source = {
        ConfigMapVolumeSource(name = obj.name)
      }
    }
  }

  final case class ConfigurationReference(labels: Labels, data: Map[String, String])
    extends Configuration
    with Transformable[ConfigurationReference]
    with Definable[Configuration, ConfigurationReference, ConfigurationDefinition] {
    def define(implicit builder: NamespaceBuilder): ConfigurationDefinition = {
      ConfigurationDefinition(
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
    ): ConfigurationReference = {
    val conf = ConfigurationReference(
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
    ): ConfigurationDefinition = {
    val conf = ref(
      labels = labels,
      data = data
    )(builder.systemBuilder).define
    builder.references(conf)
    conf
  }
}
