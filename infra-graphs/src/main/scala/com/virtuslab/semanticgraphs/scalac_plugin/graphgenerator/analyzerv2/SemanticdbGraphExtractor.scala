package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2

import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.semanticdb.SymbolInformation
import com.virtuslab.semanticgraphs.proto.model.graphnode.Edge
import scala.meta.internal.semanticdb.Type.Empty
import scala.meta.internal.semanticdb.{
  ClassSignature,
  ExistentialType,
  Locator,
  MethodSignature,
  TextDocument,
  TextDocuments,
  Type,
  TypeRef,
  TypeSignature,
  ValueSignature
}
import com.virtuslab.semanticgraphs.proto.model.graphnode.Location
import scala.meta.internal.semanticdb.AnnotatedType
import scala.meta.internal.semanticdb.RepeatedType
import scala.meta.internal.semanticdb.SingleType
import com.typesafe.scalalogging.Logger
import scala.meta.internal.semanticdb.StructuralType
import scala.meta.internal.semanticdb.ConstantType
import scala.meta.internal.semanticdb.ThisType
import scala.meta.internal.semanticdb.SuperType
import scala.meta.internal.semanticdb.WithType
import scala.meta.internal.semanticdb.UnionType
import scala.meta.internal.semanticdb.UniversalType

class SemanticdbGraphExtractor(graphAccumulator: GraphAccumulator, semanticdbHelper: SemanticdbHelper) {

  val uri    = semanticdbHelper.uri
  val logger = Logger(classOf[SemanticdbGraphExtractor])

  def createInitialGraphBasedOnSemanticDB(): Unit = {
    val textDocument = semanticdbHelper.textDocument
    textDocument.symbols.foreach(extract)
  }

  def extract(symbolInformation: SymbolInformation): Unit = {
    val parentId = symbolInformation.symbol

    val edges = symbolInformation.signature match {
      case classSignature: ClassSignature =>
        extractEdgesFromClassSignature(classSignature, parentId)
      case methodSignature: MethodSignature =>
        extractEdgesFromMethodSignature(methodSignature, parentId)
      case valueSignature: ValueSignature =>
        extractEdgesFromType(valueSignature.tpe, EdgeType.VALUE_TYPE, parentId)
      case typeSignature: TypeSignature =>
        extractEdgesFromType(typeSignature.upperBound, EdgeType.TYPE_PARAMETER, parentId) ++
          extractEdgesFromType(typeSignature.lowerBound, EdgeType.TYPE_PARAMETER, parentId) ++
          createEdges(typeSignature.typeParameters.toSeq.flatMap(_.symlinks), EdgeType.TYPE_PARAMETER)
      case s => logger.debug(s"Not supported signature $s"); Seq.empty[Edge]
    }

    val annotationsEdges = symbolInformation.annotations.flatMap { annotation =>
      extractEdgesFromType(annotation.tpe, "ANNOTATED", parentId)
    }

    // symbolInformation.references.foreach { reference =>
    //   exporter.linkParentWithChildren(
    //     parentId,
    //     Seq(reference.symbol),
    //     reference.role.toString(),
    //     symbolDef.uri,
    //     reference.range
    //   )
    // }
    graphAccumulator.createNode(
      symbolInformation,
      uri,
      extractLocation(symbolInformation.symbol),
      annotationsEdges ++ edges
    )
  }

  private def extractEdgesFromClassSignature(classSignature: ClassSignature, parentId: String): Seq[Edge] = {
    val declarationsEdges = classSignature.declarations
      .map { declaration =>
        createEdges(declaration.symlinks, EdgeType.DECLARATION)
      }
      .getOrElse(Seq.empty)
    val parametersEdges = classSignature.typeParameters
      .map(typeParameters => createEdges(typeParameters.symlinks, EdgeType.TYPE_PARAMETER))
      .getOrElse(Seq.empty)

    val extendsEdges = classSignature.parents.flatMap(extractEdgesFromType(_, EdgeType.EXTEND, parentId))
    declarationsEdges ++ parametersEdges ++ extendsEdges
  }

  private def extractEdgesFromMethodSignature(methodSignature: MethodSignature, parentId: String): Seq[Edge] = {
    val parametersEdges =
      methodSignature.parameterLists.flatMap(parameters => createEdges(parameters.symlinks, "METHOD_PARAMETER"))
    val returnTypeEdges = extractEdgesFromType(methodSignature.returnType, "RETURN_TYPE", parentId)
    parametersEdges ++ returnTypeEdges
  }

  private def extractEdgesFromType(_type: Type, role: String, parentId: String): Seq[Edge] = _type match {
    case SingleType(prefix, symbol) =>
      Seq(createEdge(symbol, role))
    case TypeRef(prefix, symbol, typeArguments) =>
      createEdge(symbol, role) +: typeArguments.flatMap(extractEdgesFromType(_, role, parentId))
    case ExistentialType(tpe, declarations) =>
      extractEdgesFromType(tpe, role, parentId) ++ declarations
        .map(scope =>
          scope.symlinks.map { link =>
            createEdge(link, role)
          }
        )
        .getOrElse(Seq.empty)
    case AnnotatedType(annotations, tpe) =>
      annotations.map(_.tpe).flatMap(extractEdgesFromType(_, role, parentId)) ++ extractEdgesFromType(
        tpe,
        role,
        parentId
      )
    case Empty             => Seq.empty
    case RepeatedType(tpe) => extractEdgesFromType(tpe, role, parentId)
    case StructuralType(tpe, declarations) =>
      extractEdgesFromType(tpe, role, parentId) ++ declarations
        .map(scope =>
          scope.symlinks.map { link =>
            createEdge(link, role)
          }
        )
        .getOrElse(Seq.empty)
    case ConstantType(constant) => //TODO?
      Seq.empty
    case ThisType(symbol) =>
      Seq(createEdge(symbol, role))
    case SuperType(prefix, symbol) =>
      Seq(createEdge(symbol, role))
    case WithType(types) =>
      types.flatMap(extractEdgesFromType(_, role, parentId))
    case UnionType(types) =>
      types.flatMap(extractEdgesFromType(_, role, parentId))
    case UniversalType(typeParameters, tpe) =>
      extractEdgesFromType(tpe, role, parentId)
    case ref =>
      logger.debug(s"Not supported type in $role - $ref") //TODO implement the rest
      Seq.empty
  }

  def createEdges(symlinks: Seq[String], _type: String): Seq[Edge] =
    symlinks.map(createEdgeForDefinition(_, _type))

  def createEdge(child: String, _type: String) = {
    val childId = graphAccumulator.createId(child, uri)
    Edge(childId, _type, None)
  }

  def createEdgeForDefinition(symbol: String, _type: String): Edge = {
    val childId = graphAccumulator.createId(symbol, uri)
    Edge(childId, _type, extractLocation(symbol))
  }

  private def extractLocation(symbol: String): Option[Location] = {
    val occurrence = semanticdbHelper.findOccurence(symbol)
    occurrence.flatMap(_.range).map { r =>
      Location(uri, r.startLine, r.startCharacter, r.endLine, r.endCharacter)
    }
  }

}
