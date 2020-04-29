package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl
import com.virtuslab.iat.dsl._

case class Application(
    labels: List[Label],
    containers: List[Container] = Nil,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil,
    mounts: List[Mount] = Nil)
  extends Named
  with Labeled
  with Containerized
  with Mounts
  with Patchable[Application]
  with Interpretable[Application]

case class Container(
    labels: List[Label],
    image: String,
    command: List[String] = Nil,
    args: List[String] = Nil,
    envs: List[(String, String)] = Nil,
    ports: List[Port] = Nil)
  extends dsl.Container
