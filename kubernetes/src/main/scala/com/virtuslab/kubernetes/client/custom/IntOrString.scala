package com.virtuslab.kubernetes.client.custom

case class IntOrString(value: Either[Int, String]) {
  def isInt: Boolean = value.isLeft
  def int: Int = if (!isInt) throw new IllegalStateException("Not an integer") else value.swap.getOrElse(-1)
  def string: String = if (isInt) throw new IllegalStateException("Not a string") else value.getOrElse("")
}

object IntOrString {
  def apply(int: Int): IntOrString = new IntOrString(Left(int))
  def apply(string: String): IntOrString = new IntOrString(Right(string))

  import org.json4s.CustomSerializer
  import org.json4s.JsonAST._

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  case object Serializer
    extends CustomSerializer[IntOrString](
      _ =>
        ({
          case JInt(i)    => IntOrString(Left(i.intValue))
          case JString(s) => IntOrString(Right(s))
          case JNull      => null
        }, {
          case IntOrString(Left(i))  => JInt(i)
          case IntOrString(Right(s)) => JString(s)
        })
    )
}
