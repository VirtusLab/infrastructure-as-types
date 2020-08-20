package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Protocol.HasPort

trait Containerized {
  def containers: Seq[Containerized.Container]
}

object Containerized {
  trait Container extends Named with Labeled with Patchable[Container] {
    def labels: Seq[Label]
    def image: String
    def command: Seq[String]
    def args: Seq[String]
    def envs: Seq[(String, String)]
    def ports: Seq[HasPort]
  }
}
