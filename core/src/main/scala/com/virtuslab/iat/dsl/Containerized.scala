package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Protocol.HasPort

trait Containerized {
  def containers: List[Containerized.Container]
}

object Containerized {
  trait Container extends Named with Labeled with Patchable[Container] {
    def labels: List[Label]
    def image: String
    def command: List[String]
    def args: List[String]
    def envs: List[(String, String)]
    def ports: List[HasPort]
  }
}
