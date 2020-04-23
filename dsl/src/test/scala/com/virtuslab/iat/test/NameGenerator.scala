package com.virtuslab.iat.test

import scala.util.Random

object NameGenerator {

  def generateNames(maxRandomSuffix: Int = 5): (String, String) =
    (generateSystemName(maxRandomSuffix), generateNamespaceName(maxRandomSuffix))
  def generateSystemName(maxRandomSuffix: Int = 5): String =
    generatePrefixedName("system", maxRandomSuffix)
  def generateNamespaceName(maxRandomSuffix: Int = 5): String =
    generatePrefixedName("namespace", maxRandomSuffix)
  def generatePrefixedName(prefix: String, maxRandomSuffix: Int = 5) =
    s"$prefix-${Random.alphanumeric.take(maxRandomSuffix).mkString}"
}
