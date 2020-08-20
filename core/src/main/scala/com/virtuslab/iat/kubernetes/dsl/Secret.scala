package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._

case class Secret(labels: Seq[Label], data: Map[String, String])
  extends Named
  with Labeled
  with KeyValue
  with Patchable[Secret]
  with Interpretable[Secret]

object Secret {
  object ops {
    import java.nio.charset.StandardCharsets
    import java.util.Base64

    def base64encode(value: String): Array[Byte] = {
      val bytes = value.getBytes(StandardCharsets.UTF_8)
      Base64.getEncoder.encode(bytes)
    }

    def base64decode(value: Array[Byte]): String = {
      val bytes = Base64.getDecoder.decode(value)
      new String(bytes, StandardCharsets.UTF_8)
    }
  }
}
