package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl.{ KeyValue, Label, Labeled, Named, Patchable }
import com.virtuslab.iat.dsl.kubernetes.Mountable.MountSource
import skuber.Volume.{ Secret => SecretVolumeSource }

case class Secret(labels: List[Label], data: Map[String, String]) extends Named with Labeled with KeyValue with Patchable[Secret]

object Secret {
  implicit val mountSource: MountSource[Secret] = (obj: Secret) => {
    SecretVolumeSource(secretName = obj.name)
  }

  object ops {
    import java.util.Base64
    import java.nio.charset.StandardCharsets

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
