package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl.{ Label, Labeled, Named, Patchable }

case class Namespace(labels: List[Label]) extends Named with Labeled with Patchable[Namespace]
