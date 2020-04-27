package com.virtuslab.iat.dsl

trait Label {
  def key: String
  def value: String

  def asTuple: (String, String) = key -> value
  override def toString: String = asTuple.toString()
}

object Label {
  final case class Name(value: String) extends Label {
    override def key: String = "name"
  }

  final case class Role(value: String) extends Label {
    override def key: String = "role"
  }

  final case class Tier(value: String) extends Label {
    override def key: String = "tier"
  }

  final case class App(value: String) extends Label {
    override def key: String = "app"
  }

  type UntypedLabel = Label
  object UntypedLabel {
    def apply(k: String, v: String): UntypedLabel = new Label {
      override def key: String = k
      override def value: String = v
    }
  }

  object ops {
    def fromMap(m: (String, String)): Label = m match {
      case (k, v) if k == "name" => Name(v)
      case (k, v) if k == "role" => Role(v)
      case (k, v) if k == "tier" => Tier(v)
      case (k, v) if k == "app"  => App(v)
      case (k, v)                => UntypedLabel(k, v)
    }

    implicit class LabelsOps(ls: List[Label]) {
      def asMap: Map[String, String] = ls.map(_.asTuple).toMap
    }
  }
}
