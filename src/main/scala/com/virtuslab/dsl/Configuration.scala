package com.virtuslab.dsl

import com.virtuslab.dsl.Mountable.MountSource
import skuber.Volume
import skuber.Volume.ConfigMapVolumeSource

case class Configuration private (labels: Labels, data: Map[String, String]) extends KeyValue with Transformable[Configuration]

object Configuration {
  implicit val mountSource = new MountSource[Configuration] {
    override def source(obj: Configuration): Volume.Source = {
      ConfigMapVolumeSource(name = obj.name)
    }
  }

//  def apply(
//      data: Map[String, String],
//      labels: Labels
//    )(implicit
//      builder: SystemBuilder
//    ): Configuration = {
//    val conf = new Configuration(
//      labels = labels,
//      data = data
//    )
//    builder.references(conf)
//    conf
//  }
}
