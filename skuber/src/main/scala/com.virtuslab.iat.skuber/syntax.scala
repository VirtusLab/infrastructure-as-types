package com.virtuslab.iat.skuber

import com.virtuslab.iat.kubernetes.dsl.Mountable.MountSource
import com.virtuslab.iat.kubernetes.dsl.{ Configuration, Secret }
import com.virtuslab.iat.scala.TupleOps

object dsl {
  import skuber.Volume.{ ConfigMapVolumeSource, Secret => SecretVolumeSource }

  implicit val secretMountSource: MountSource[Secret, SecretVolumeSource] =
    (obj: Secret) => SecretVolumeSource(secretName = obj.name)

  implicit val configurationMountSource: MountSource[Configuration, ConfigMapVolumeSource] =
    (obj: Configuration) => ConfigMapVolumeSource(name = obj.name)
}

object deployment extends ApiOps
  with DefaultInterpreters
  with DefaultDeinterpreters
  with InterpreterOps
  with TupleOps

object playjson extends PlayJsonProcessors
  with DefaultInterpreters
  with InterpreterOps
  with TupleOps

object interpreter extends DefaultInterpreters
object subinterpreter extends DefaultSubinterpreters
object details extends DefaultDetails
object deinterpreter extends DefaultDeinterpreters
