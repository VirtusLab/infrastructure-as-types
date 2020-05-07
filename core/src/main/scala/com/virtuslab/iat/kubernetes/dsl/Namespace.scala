package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._

case class Namespace(labels: List[Label]) extends Named with Labeled with Patchable[Namespace] with Interpretable[Namespace]
