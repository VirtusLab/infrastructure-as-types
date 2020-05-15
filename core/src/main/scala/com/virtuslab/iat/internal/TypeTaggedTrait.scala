package com.virtuslab.iat.internal

import scala.reflect.runtime.universe._

trait TypeTaggedTrait[Self] { self: Self =>
  val selfTypeTag: TypeTag[Self]

  def hasType[Other: TypeTag]: Boolean =
    typeOf[Other] =:= selfTypeTag.tpe

  def cast[Other: TypeTag]: Option[Other] =
    if (hasType[Other])
      Some(this.asInstanceOf[Other])
    else
      None
}

abstract class TypeTagged[Self: TypeTag] extends TypeTaggedTrait[Self] { self: Self =>
  val selfTypeTag: TypeTag[Self] = typeTag[Self]
}
