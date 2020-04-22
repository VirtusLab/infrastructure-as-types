package com.virtuslab.iat.dsl

trait Label {
  def key: String
  def value: String

  def asTuple: (String, String) = key -> value
  override def toString: String = asTuple.toString()
}

object Label {
  def apply[A](a: A)(v: A => String): Label = new Label {
    override def key: String = a.getClass.getSimpleName.toLowerCase
    override def value: String = v(a)
  }

  final case class Name(value: String)
  implicit val nameLabel: Name => Label = name => Label(name)(_.value)

  type UntypedLabel = Label
  object UntypedLabel {
    def apply(k: String, v: String): UntypedLabel = new Label {
      override def key: String = k
      override def value: String = v
    }
  }

  object ops {
    implicit class LabelsOps(ls: List[Label]) {
      def asMap: Map[String, String] = ls.map(_.asTuple).toMap
    }
  }
}
