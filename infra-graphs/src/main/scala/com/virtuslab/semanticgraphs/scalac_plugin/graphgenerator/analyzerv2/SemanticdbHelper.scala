package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2

import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.semanticdb.SymbolInformation
import com.virtuslab.semanticgraphs.proto.model.graphnode.GraphNode
import scala.meta.internal.semanticdb.SymbolOccurrence
import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import com.typesafe.scalalogging.Logger

class SemanticdbHelper(val textDocument: TextDocument, val uri: String) {

  // All the symbols defined in the file (semantic informations)
  private val symbolInformationMap: Map[String, SymbolInformation] = textDocument.symbols.map(s => s.symbol -> s).toMap

  private val occurencesWithRange = textDocument.occurrences.filter(_.range.isDefined)

  private val logger = Logger(classOf[SemanticdbHelper])

  // Symbol possition in the code
  // there are bugs in semanticdb, some expected symbols might be not in the place we expect them. Option[SymbolOccurence] is inevitable...
  def findOccurence(
    name: String,
    startLine: Int,
    startCharacter: Int,
    endLine: Int,
    endCharacter: Int
  ): Option[SymbolOccurrence] = {
    occurencesWithRange
      .find { occurence =>
        val range = occurence.getRange
        range.startLine == startLine && range.startCharacter == startCharacter && range.endLine == endLine && range.endCharacter == endCharacter
      }
      .orElse {
        logger.debug(
          s"There is no occurence of $name in ${textDocument.uri} in line $startLine:$startCharacter;$endLine:$endCharacter"
        )
        None
      }
  }

  def findSymbol(s: String): SymbolInformation =
    symbolInformationMap(s)

  /**
   * Find SymbolInformation that should be in given position
   *
   * @param position position of the symbol in the source code
   * @param name Optional, just to provide better error rising
   * @return
   */
  def findSymbol(position: Position, name: String = ""): Option[(SymbolOccurrence, SymbolInformation)] = {
    val occurence =
      findOccurence(name, position.startLine, position.startColumn, position.endLine, position.endColumn)
    occurence.map(o => o -> findSymbol(o.symbol))
  }

  def isDefinedInFile(symbol: String) = occurencesWithRange.filter(_.role == Role.DEFINITION).exists(_.symbol == symbol)

  def findOccurence(symbol: String): Option[SymbolOccurrence] =
    occurencesWithRange
      .filter(_.role == Role.DEFINITION)
      .find(_.symbol == symbol)
      .orElse(
        occurencesWithRange.find(_.symbol == symbol)
      )

}
