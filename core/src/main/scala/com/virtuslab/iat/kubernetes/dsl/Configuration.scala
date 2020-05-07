package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._

case class Configuration(labels: List[Label], data: Map[String, String])
  extends Named
  with Labeled
  with KeyValue
  with Patchable[Configuration]
  with Interpretable[Configuration]
