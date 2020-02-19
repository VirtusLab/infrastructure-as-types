package com.virtuslab.internal

sealed trait ValueFrom
object ValueFrom {
  case class ConfigMapKeyRefValue(
      key: String,
      name: String,
      optional: Boolean)
    extends ValueFrom
  case class FieldRefValue(fieldPath: String) extends ValueFrom {
    val apiVersion = ""
  }
  case class ResourceFieldRefValue(
      containerName: String,
      divisor: Any,
      resource: String)
    extends ValueFrom
  case class SecretKeyRefValue(
      key: String,
      name: String,
      optional: Boolean)
    extends ValueFrom
}

sealed trait ValueRef
object ValueRef {
  import ValueFrom._

  case class ConfigMapKeyRef(configMapKeyRef: ConfigMapKeyRefValue) extends ValueRef
  case class FieldRef private (fieldRef: FieldRefValue) extends ValueRef
  case class ResourceFieldRef private (resourceFieldRef: ResourceFieldRefValue) extends ValueRef
  case class SecretKeyRef private (secretKeyRef: SecretKeyRefValue) extends ValueRef
}

sealed trait EnvVar {
  def name: String
}

object EnvVar {
  case class StringEnvVarValue(name: String, value: String) extends EnvVar
  case class ValueFromRef(name: String, valueRef: ValueRef) extends EnvVar

  def apply(name: String, value: String): EnvVar = StringEnvVarValue(name, value)
}
