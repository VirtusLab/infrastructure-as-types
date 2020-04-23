package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl.{ Label, Labeled, Named, Patchable, Protocols }

case class Gateway(labels: List[Label], protocols: Protocols) extends Named with Labeled with Patchable[Gateway]
