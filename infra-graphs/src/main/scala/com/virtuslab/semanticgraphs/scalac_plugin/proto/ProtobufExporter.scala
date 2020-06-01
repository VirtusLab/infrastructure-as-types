package com.virtuslab.semanticgraphs.scalac_plugin.proto

import java.io.File
import java.nio.file.Path

import com.virtuslab.semanticgraphs.proto.model.graphnode.{Edge, GraphNode, Location, SemanticGraphFile}
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.exporters.Exporter

import scala.meta.internal.semanticdb
import scala.meta.internal.semanticdb.{SymbolInformation, SymbolOccurrence}

class ProtobufExporter extends Exporter {

  var map = Map.empty[String, GraphNode]

  override def createParentNode(
    symbolInformation: SymbolInformation,
    uri: String,
    symbolOccurrence: SymbolOccurrence
  ): String = {
    
    val displayName      = symbolInformation.displayName
    val id               = createId(symbolInformation.symbol, uri)
    val extractedPackage = extractPackage(symbolInformation)
    val isLocal          = symbolInformation.isLocal
    val kind             = symbolInformation.kind.toString

    val range = symbolOccurrence.range
    val startLine      = Integer.valueOf(range.map(_.startLine).getOrElse(0))
    val startCharacter = Integer.valueOf(range.map(_.startCharacter).getOrElse(0))
    val endLine        = Integer.valueOf(range.map(_.endLine).getOrElse(0))
    val endCharacter   = Integer.valueOf(range.map(_.endCharacter).getOrElse(0))

    val location = Location(uri, startLine, startCharacter, endLine, endCharacter)

    val graphNode = GraphNode(
      id = id,
      kind = kind,
      location = Some(location),
      displayName = displayName,
      properties = Map(
        "symbol"      -> symbolInformation.symbol,
        "displayName" -> displayName,
        "package"     -> extractedPackage,
        "isLocal"     -> isLocal.toString,
        "kind"        -> kind,
        "uri"         -> uri,
        "startLine"      -> startLine.toString,
        "startCharacter" -> startCharacter.toString,
        "endLine"        -> endLine.toString,
        "endCharacter"   -> endCharacter.toString,
        "access"         -> symbolInformation.access.toString,
        "isImplicit"     -> symbolInformation.isImplicit.toString,
        "isFinal"        -> symbolInformation.isFinal.toString,
        "isAbstract"     -> symbolInformation.isAbstract.toString,
        "isVar"          -> symbolInformation.isVar.toString()
      )
    )
    map = map.updated(id, graphNode)
    id
  }

  override def linkParentWithChildren(
    parentId: String,
    childrenSymbols: Seq[String],
    role: String,
    uri: String,
    range: Option[semanticdb.Range]
  ): Unit = {
    childrenSymbols.foreach { childSymbol =>
      val childId = createId(childSymbol, uri)
      val location = range.map { r =>
        Location(uri, r.startLine, r.startCharacter, r.endLine, r.endCharacter)
      }
      createEdge(parentId, Edge(childId, `type` = role, location = location))
    }
  }

  private def createEdge(id: String, edge: Edge): Unit = {
    val node = map.getOrElse(id, GraphNode())
    map = map.updated(id, node.copy(edges = node.edges :+ edge))
  }

  override def dumpFile(uri: String, projectPath: Path): Unit = {
    val fileUri = s"${projectPath.toAbsolutePath}/.semanticgraphs/$uri.semanticgraphdb"
    val semanticGraphFile = SemanticGraphFile(uri, map.values.toSeq)
    import java.io.FileOutputStream
    val file = new File(fileUri)
    file.getParentFile.mkdirs()
    file.createNewFile()
    val outputStream = new FileOutputStream(file, false)
    outputStream.write(semanticGraphFile.toByteArray)
    outputStream.close()
  }
}
