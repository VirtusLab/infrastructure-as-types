package com.virtuslab.kubernetes.client.custom

import java.util.Base64

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

case class B64Encoded private (value: String) extends AnyVal

object B64Encoded {
  def apply(str: String): B64Encoded = {
    new B64Encoded(new String(Base64.getEncoder.encode(str.getBytes)))
  }

  val json4sCustomSerializer = new CustomSerializer[B64Encoded](
    _ =>
      ({
        case JString(str) => B64Encoded(str)
      }, {
        case B64Encoded(value) => JString(value)
      })
  )
}
