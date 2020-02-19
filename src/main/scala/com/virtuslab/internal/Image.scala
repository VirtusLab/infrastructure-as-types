package com.virtuslab.internal

import enumeratum._

sealed trait ImagePullPolicy extends EnumEntry
object ImagePullPolicy extends Enum[ImagePullPolicy] {
  case object Always extends ImagePullPolicy
  case object Never extends ImagePullPolicy
  case object IfNotPresent extends ImagePullPolicy

  override def values = findValues
}

sealed trait Image {
  def image: String
}
object Image {
  private case class ImageByTag(name: String, tag: String) extends Image {
    override val image: String = s"$name:$tag"
  }

  private case class ImageByHash(name: String, hash: String) extends Image {
    override val image: String = s"$name@$hash"
  }

  def byTag(name: String, tag: String): Image = {
    ImageByTag(name, tag)
  }

  def byHash(name: String, hash: String): Image = {
    ImageByHash(name, hash)
  }
}
