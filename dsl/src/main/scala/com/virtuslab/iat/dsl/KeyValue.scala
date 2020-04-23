package com.virtuslab.iat.dsl

trait KeyValue extends Labeled {
  def data: Map[String, String]
}
