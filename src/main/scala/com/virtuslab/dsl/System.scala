package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.DefinedNamespace

case class System protected (name: String, namespaces: List[DefinedNamespace]) {

  def addNamespace(ns: DefinedNamespace): System = {
    copy(namespaces = ns :: namespaces)
  }
}

object System {
  def apply(name: String): System = {
    System(name, Nil)
  }
}
