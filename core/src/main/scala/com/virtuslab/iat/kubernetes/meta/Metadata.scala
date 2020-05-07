package com.virtuslab.iat.kubernetes.meta

case class Metadata(
    apiVersion: String,
    kind: String,
    namespace: String,
    name: String) {
  override def toString: String = (apiVersion, kind, namespace, name).toString()
}
