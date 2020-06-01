package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.exporters

import java.nio.file.Path

import scala.meta.internal.semanticdb.{SymbolInformation, SymbolOccurrence, Range => SemanticDBRange}

trait Exporter {
  def createParentNode(symbolInformation: SymbolInformation, uri: String, symbolOccurrence: SymbolOccurrence): String
  def linkParentWithChildren(parentId: String, childrenSymbols: Seq[String], role: String, uri: String, range: Option[SemanticDBRange] = None): Unit

  protected def createId(symbol: String, uri: String): String = if (symbol.startsWith("local")) symbol + ":" + uri else symbol

  protected def extractPackage(symbolInformation: SymbolInformation): String = {
    val fragments = symbolInformation.symbol.split("/")
    fragments.take(fragments.size - 1).mkString(".")
  }

  def finish(): Unit = ()
  def prepare(): Unit = ()

  def dumpFile(uri: String, projectPath: Path): Unit = ()
}
