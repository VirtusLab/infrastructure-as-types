package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._

case class Gateway(labels: List[Label], protocols: Protocols)
  extends Named
  with Labeled
  with Patchable[Gateway]
  with Interpretable[Gateway]
