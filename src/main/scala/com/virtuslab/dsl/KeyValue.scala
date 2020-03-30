package com.virtuslab.dsl

trait KeyValue extends Labeled {
  def data: Map[String, String]
}
