package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2

import com.virtuslab.semanticgraphs.proto.model.graphnode.GraphNode
import com.virtuslab.semanticgraphs.proto.model.graphnode.Edge
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.semanticdb.SymbolOccurrence
import com.virtuslab.semanticgraphs.proto.model.graphnode.Location
import scala.meta.internal.semanticdb.Range
import scala.meta.internal.semanticdb.SymbolInformation.Kind.METHOD
import scala.meta.internal.semanticdb.SymbolInformation.Kind.LOCAL

class GraphAccumulator(uri: String) {
  private val nodes: scala.collection.mutable.Map[String, GraphNode] = scala.collection.mutable.Map.empty

  def exportNodes(): Seq[GraphNode] =
    nodes.values.toSeq

  def addNode(graphNode: GraphNode): Unit =
    nodes += (graphNode.id -> graphNode)

  def getNode(symbol: String): GraphNode =
    nodes(createId(symbol, uri))

  def findNode(symbol: String): Option[GraphNode] =
    nodes.get(createId(symbol, uri))

  def addEdge(symbol: String, edge: Edge): GraphNode = {
    val node    = nodes(symbol)
    val updated = node.copy(edges = node.edges :+ edge)
    nodes += (symbol -> updated)
    updated
  }

  def upsertEdge(parent: String, child: String, _type: String, location: Location): GraphNode = {
    def compareEdge(edge: Edge, child: String, _type: String) =
      edge.to == child && edge.`type` == _type
    val node    = nodes(parent)
    val edgeOpt = node.edges.find(compareEdge(_, child, _type))
    edgeOpt match {
      case Some(edge) =>
        val updatedEdge = edge.copy(location = Some(location))
        val updated     = node.copy(edges = node.edges.filterNot(compareEdge(_, child, _type)) :+ updatedEdge)
        nodes += (parent -> updated)
        updated
      case None =>
        val updated = node.copy(edges = node.edges :+ Edge(child, _type, Some(location)))
        nodes += (parent -> updated)
        updated
    }
  }

  def addLocationToEdge(parent: String, child: String, _type: String, location: Location): GraphNode = {
    def compareEdge(edge: Edge, child: String, _type: String) =
      edge.to == child && edge.`type` == _type
    val node    = nodes(parent)
    val edgeOpt = node.edges.find(compareEdge(_, child, _type))
    edgeOpt match {
      case Some(edge) =>
        val updatedEdge = edge.copy(location = Some(location))
        val updated     = node.copy(edges = node.edges.filterNot(compareEdge(_, child, _type)) :+ updatedEdge)
        nodes += (parent -> updated)
        updated
      case None =>
        node
    }
  }

  def createNode(
    symbolInformation: SymbolInformation,
    uri: String,
    location: Option[Location],
    children: Seq[Edge] = Seq.empty,
    additionalProperties: Map[String, String] = Map.empty
  ): GraphNode = {

    val displayName      = symbolInformation.displayName
    val id               = createId(symbolInformation.symbol, uri)
    val extractedPackage = extractPackage(symbolInformation)
    val kind             = extractKind(symbolInformation)

    val graphNode = GraphNode(
      id = id,
      kind = kind,
      location = location,
      displayName = displayName,
      properties = Map(
        "symbol"         -> symbolInformation.symbol,
        "displayName"    -> displayName,
        "package"        -> extractedPackage,
        "isLocal"        -> symbolInformation.isLocal.toString,
        "kind"           -> kind,
        "uri"            -> uri,
        "startLine"      -> location.map(_.startLine).getOrElse(0).toString,
        "startCharacter" -> location.map(_.startCharacter).getOrElse(0).toString,
        "endLine"        -> location.map(_.endLine).getOrElse(0).toString,
        "endCharacter"   -> location.map(_.endCharacter).getOrElse(0).toString,
        "access"         -> symbolInformation.access.toString,
        "isImplicit"     -> symbolInformation.isImplicit.toString,
        "isFinal"        -> symbolInformation.isFinal.toString,
        "isAbstract"     -> symbolInformation.isAbstract.toString,
        "isVar"          -> symbolInformation.isVar.toString(),
        "isVal"          -> symbolInformation.isVal.toString()
      ) ++ additionalProperties,
      edges = children
    )
    nodes += (id -> graphNode)
    graphNode
  }

  def createAggregationNode(
    symbol: String,
    displayName: String,
    kind: String,
    uri: String,
    location: Location,
    children: Seq[Edge] = Seq.empty,
    additionalProperties: Map[String, String] = Map.empty
  ): GraphNode = {

    val id = createId(symbol, uri)

    val graphNode = GraphNode(
      id = id,
      kind = kind,
      location = Some(location),
      displayName = displayName,
      properties = Map(
        "symbol"         -> id,
        "displayName"    -> displayName,
        "isLocal"        -> true.toString,
        "kind"           -> kind,
        "uri"            -> uri,
        "startLine"      -> location.startLine.toString,
        "startCharacter" -> location.startCharacter.toString,
        "endLine"        -> location.endLine.toString,
        "endCharacter"   -> location.endCharacter.toString
      ) ++ additionalProperties,
      edges = children
    )
    nodes += (id -> graphNode)
    graphNode
  }

  def addNodeProperties(node: GraphNode, properties: Map[String, String]): GraphNode = {
    val updated = node.copy(properties = node.properties ++ properties)
    nodes.update(node.id, updated)
    updated
  }

  def createId(symbol: String, uri: String): String =
    if (symbol.startsWith("local")) symbol + ":" + uri else symbol

  private def extractPackage(symbolInformation: SymbolInformation): String = {
    val fragments = symbolInformation.symbol.split("/")
    fragments.take(fragments.size - 1).mkString(".")
  }

  def extractKind(symbolInformation: SymbolInformation): String = symbolInformation.kind match {
    case METHOD | LOCAL if symbolInformation.isVal => "VALUE"
    case METHOD | LOCAL if symbolInformation.isVar => "VARIABLE"
    case other                                     => other.name
  }

}
