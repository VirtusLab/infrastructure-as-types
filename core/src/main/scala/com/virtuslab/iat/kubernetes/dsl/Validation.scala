package com.virtuslab.iat.kubernetes.dsl

object Validation {
  // From Kubernetes:
  // https://github.com/kubernetes/kubernetes/blob/v1.18.0-rc.1/staging/src/k8s.io/apimachinery/pkg/util/validation/validation.go
  import scala.util.matching.Regex

  val qualifiedNameCharFmt: String = "[A-Za-z0-9]"
  val qualifiedNameExtCharFmt: String = "[-A-Za-z0-9_.]"
  val qualifiedNameFmt: String = "(" + qualifiedNameCharFmt + qualifiedNameExtCharFmt + "*)?" + qualifiedNameCharFmt
  val qualifiedNameMaxLength: Int = 63

  val qualifiedNameRegexp: Regex = ("^" + qualifiedNameFmt + "$").r

  def IsQualifiedName(value: String): (Boolean, String) = {
    if (value.length == 0) {
      (false, "value is empty")
    } else if (value.length > qualifiedNameMaxLength) {
      (false, s"value length exceeds $qualifiedNameMaxLength characters")
    } else if (!qualifiedNameRegexp.pattern.matcher(value).matches()) {
      (false, s"value doesn not match pattern '$qualifiedNameRegexp'")
    } else {
      (true, "")
    }
  }
}
