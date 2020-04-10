package com.virtuslab.dsl

case class Gateway(labels: Labels, protocols: Protocols) extends Labeled with Transformable[Gateway]
