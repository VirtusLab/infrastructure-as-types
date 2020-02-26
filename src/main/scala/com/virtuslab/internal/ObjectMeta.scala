package com.virtuslab.internal

case class ObjectMeta(
    name: String,
    namespace: String = "", // empty is equivalent to 'default'
    labels: Map[String, String] = Map(),
    annotations: Map[String, String] = Map())
