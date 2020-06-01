package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2

import scala.meta.Tree
import scala.meta.internal.semanticdb.TextDocument
import com.virtuslab.semanticgraphs.proto.model.graphnode.SemanticGraphFile
import scala.meta.Source
import scala.meta.Defn
import scala.meta.internal.semanticdb.SymbolInformation
import com.virtuslab.semanticgraphs.proto.model.graphnode.GraphNode
import scala.meta.internal.semanticdb.SymbolOccurrence
import scala.meta.internal.semanticdb.ClassSignature
import com.virtuslab.semanticgraphs.proto.model.graphnode.Edge
import scala.meta.internal.semanticdb.TypeRef
import com.virtuslab.semanticgraphs.proto.model.graphnode.Location
import scala.meta._
import scala.meta.inputs.Position
import scala.meta.internal.semanticdb.SymbolOccurrence.Role
import scala.util.Try
import scala.util.Failure
import com.typesafe.scalalogging.Logger

class AstGraphExtractor(graphAccumulator: GraphAccumulator, semanticDbHelper: SemanticdbHelper) {

  val uri = semanticDbHelper.uri

  val logger = Logger(this.getClass)

  var counter = 0

  def augmentTheInitialGraph(tree: Tree): SemanticGraphFile = tree match {
    case source: Source =>
      val pos      = source.pos
      val fileNode = new GraphNode(uri, "FILE", None)
      graphAccumulator.addNode(fileNode)
      source.stats.foreach { stat =>
        Try(extractStat(stat, fileNode)) match {
          case Failure(e) => logger.debug(s"Exception: ${e.getMessage()}")
          case _          =>
        }
      }
      SemanticGraphFile(uri, graphAccumulator.exportNodes())
  }

  def extractStat(node: Stat, parent: GraphNode): Unit = node match {
    case _package: Pkg              => extractPackage(_package, parent)
    case _packageObject: Pkg.Object => extractPackageObject(_packageObject, parent)
    case _import: Import            => //ignore for now
    case decl: Decl                 => extractDecl(decl, parent)
    case _defn: Defn                => extractDefn(_defn, parent)
    case term: Term                 => extractTerm(term, parent)
    case unsupported                => logger.debug(s"Unsupported stat ${unsupported.getClass()} ${posToLocation(unsupported.pos)}")
  }

  def extractPackage(_package: Pkg, parent: GraphNode) =
    _package.stats.foreach(extractStat(_, parent))

  def extractPackageObject(_packageObject: Pkg.Object, parent: GraphNode) = {
    val packageObjectNode = extractTermName(_packageObject.name, parent)
    extractTemplate(_packageObject.templ, packageObjectNode.getOrElse(parent))
  }

  def extractDecl(decl: Decl, parent: GraphNode): Unit = decl match {
    case Decl.Val(mods: List[Mod], pats: List[Pat], decltpe: scala.meta.Type) =>
      extractType(decltpe, parent, EdgeType.DECLARATION)
      pats.foreach(extractPattern(_, parent))
    case Decl.Var(mods: List[Mod], pats: List[Pat], decltpe: scala.meta.Type) =>
      extractType(decltpe, parent, EdgeType.DECLARATION)
      pats.foreach(extractPattern(_, parent))
    case Decl.Def(
        mods: List[Mod],
        name: Term.Name,
        tparams: List[scala.meta.Type.Param],
        paramss: List[List[Term.Param]],
        decltpe: scala.meta.Type
        ) =>
      semanticDbHelper.findSymbol(name.pos).foreach {
        case (occurence, symbol) =>
          val defNode = graphAccumulator.getNode(symbol.symbol)
          paramss.flatten.foreach(extractTermParam(_, defNode, EdgeType.METHOD_PARAMETER))
          extractType(decltpe, defNode, EdgeType.RETURN_TYPE)
          tparams.foreach(extractTypeParam(_, defNode, EdgeType.TYPE_PARAMETER))
      }
    case Decl.Type(
        mods: List[Mod],
        name: scala.meta.Type.Name,
        tparams: List[scala.meta.Type.Param],
        bounds: scala.meta.Type.Bounds
        ) =>
      val declTypeNodeOpt = extractTypeNameDeclaration(name, parent, EdgeType.DECLARATION)
      declTypeNodeOpt.foreach { declTypeNode =>
        tparams.foreach(extractTypeParam(_, declTypeNode, EdgeType.TYPE_PARAMETER))
        extractBounds(bounds, declTypeNode, EdgeType.TYPE_PARAMETER)
      }
  }

  def extractDefn(_defn: Defn, parent: GraphNode): Unit = _defn match {
    case _object: Defn.Object => extractObject(_object, parent)
    case _class: Defn.Class   => extractClass(_class, parent)
    case _def: Defn.Def       => extractDef(_def, parent)
    case _val: Defn.Val       => extractVal(_val, parent)
    case _var: Defn.Var       => extractVar(_var, parent)
    case _trait: Defn.Trait   => extractTrait(_trait, parent)
    case _type: Defn.Type     => extractDefnType(_type, parent)
    case _type: Defn.Macro    => extractMacro(_type, parent)
    case unsupported          => logger.debug(s"Unsupported def ${unsupported.getClass()} ${posToLocation(unsupported.pos)}")
  }

  def extractMacro(_macro: Defn.Macro, parent: GraphNode): Unit = {
    //TODO macro support
  }

  def extractTrait(_trait: Defn.Trait, parent: GraphNode): Unit = {
    val pos = _trait.name.pos
    semanticDbHelper.findSymbol(pos).foreach {
      case (occurence, symbol) =>
        semanticDbHelper.findSymbol(pos).foreach {
          case (occurence, symbol) =>
            val traitNode = getNodeWithLOC(symbol.symbol, _trait.pos)
            _trait.templ.inits.foreach(extractInit(_, traitNode))
            _trait.tparams.foreach(extractTypeParam(_, traitNode, EdgeType.TYPE_PARAMETER))
            _trait.templ.stats.foreach(extractStat(_, traitNode))
        }
    }
  }

  def extractClass(_class: Defn.Class, parent: GraphNode): Unit = {
    val loc = _class.pos.endLine - _class.pos.startLine
    val pos = _class.name.pos
    semanticDbHelper.findSymbol(pos).foreach {
      case (occurence, symbol) =>
        val location = posToLocation(pos)

        if (symbol.isCase) { // special case to deal with automatically generated companion object
          val caseLocation =
            _class.mods.collect { case _case: Mod.Case => posToLocation(_case.pos) }.headOption.getOrElse(location)
          val companionObjectSymbol = symbol.symbol.take(symbol.symbol.size - 1) + "."
          if (!semanticDbHelper.isDefinedInFile(companionObjectSymbol)) {
            val objectNode  = graphAccumulator.getNode(companionObjectSymbol)
            val updatedNode = objectNode.copy(location = Some(caseLocation))
            graphAccumulator.addNode(updatedNode)
          }
        }

        val classNode = getNodeWithLOC(symbol.symbol, _class.pos)
        _class.templ.inits.foreach(extractInit(_, classNode))
        extractConstructor(_class.ctor)
        _class.templ.stats.foreach(stat => extractStat(stat, classNode))
    }
  }

  def extractConstructor(_ctor: Ctor.Primary): Unit = {
    val pos = _ctor.name.pos
    semanticDbHelper.findSymbol(pos).foreach {
      case (occurence, symbol) =>
        val ctorNode = graphAccumulator.getNode(symbol.symbol)
        _ctor.paramss.flatten.foreach(extractTermParam(_, ctorNode, EdgeType.METHOD_PARAMETER))
    }
  }

  def extractObject(_object: Defn.Object, parent: GraphNode): Unit = {
    val pos = _object.name.pos
    semanticDbHelper.findSymbol(pos).foreach {
      case (occurence, symbol) =>
        val objectNode = getNodeWithLOC(symbol.symbol, _object.pos)

        _object.templ.inits.foreach(extractInit(_, objectNode))
        _object.templ.stats.foreach(extractStat(_, objectNode))
    }
  }

  /**
   * Extracting extensions, everything what inits first before the current object/class/trait
   * everything after `extends`
   * @param init
   * @param role
   * @param parent
   */
  def extractInit(init: Init, parent: GraphNode, role: String = EdgeType.EXTEND): Unit = {
    extractType(init.tpe, parent, role)
    init.argss.flatten.foreach(extractTerm(_, parent))
  }

  def extractDef(_def: Defn.Def, parent: GraphNode): Unit = {
    val (occurence, symbol) = semanticDbHelper.findSymbol(_def.name.pos).get
    val defNode             = getNodeWithLOC(symbol.symbol, _def.pos)
    _def.paramss.flatten.foreach(extractTermParam(_, defNode, EdgeType.METHOD_PARAMETER))
    _def.decltpe.foreach(extractType(_, defNode, EdgeType.RETURN_TYPE))
    extractTerm(_def.body, defNode)
  }

  def extractVal(_val: Defn.Val, parent: GraphNode): Unit = {
    _val.pats.head match {
      case Pat.Var(termName) => 
    }
    _val.pats.foreach(extractPattern(_, parent)) // linking definitions with parent
    extractTerm(_val.rhs, parent)
  }

  def extractVar(_var: Defn.Var, parent: GraphNode): Unit = {
    _var.pats.foreach(extractPattern(_, parent)) // linking definitions with parent
    _var.rhs.foreach(extractTerm(_, parent))
  }

  def extractDefnType(_type: Defn.Type, parent: GraphNode): Unit = {
    val typeNodeOpt = extractTypeNameDeclaration(_type.name, parent, EdgeType.DECLARATION)
    typeNodeOpt.foreach { typeNode =>
      _type.tparams.foreach(extractTypeParam(_, typeNode, EdgeType.TYPE_PARAMETER))
      extractType(_type.body, typeNode, EdgeType.TYPE_PARAMETER)
    }
  }

  def extractPattern(pat: Pat, parent: GraphNode): Unit = pat match {
    case Pat.Var(name: scala.meta.Term.Name) =>
      extractTermName(name, parent)
    case Pat.Wildcard()    =>
    case Pat.SeqWildcard() =>
    case Pat.Bind(lhs: Pat, rhs: Pat) =>
      extractPattern(lhs, parent)
      extractPattern(rhs, parent)
    case Pat.Alternative(lhs: Pat, rhs: Pat) =>
      extractPattern(lhs, parent)
      extractPattern(rhs, parent)
    case Pat.Tuple(args: List[Pat]) =>
      args.foreach(extractPattern(_, parent))
    case Pat.Extract(fun: Term, args: List[Pat]) =>
      extractTerm(fun, parent)
      args.foreach(extractPattern(_, parent))
    case Pat.ExtractInfix(lhs: Pat, op: Term.Name, rhs: List[Pat]) =>
      extractPattern(lhs, parent)
      extractTerm(op, parent)
      rhs.foreach(extractPattern(_, parent))
    case Pat.Interpolate(prefix: Term.Name, parts: List[Lit], args: List[Pat]) =>
      extractTermName(prefix, parent)
      //TODO Lit
      args.foreach(extractPattern(_, parent))
    case Pat.Xml(parts: List[Lit], args: List[Pat]) =>
      //TODO Lit
      args.foreach(extractPattern(_, parent))
    case Pat.Typed(lhs: Pat, rhs: Type) =>
      extractPattern(lhs, parent)
      extractType(rhs, parent, EdgeType.TYPE_PARAMETER)
    case unsupported =>
      logger.debug(s"Unsupported term: ${unsupported.getClass()} ${posToLocation(unsupported.pos)}")
  }

  def extractTermParam(param: Term.Param, parent: GraphNode, edgeType: String): Unit = {
    if (param.name.value != "") { // annonymous param can be ignored
      val paramNodeOpt = extractName(param.name)
      paramNodeOpt.foreach { paramNode =>
        graphAccumulator.upsertEdge(parent.id, paramNode.id, edgeType, posToLocation(param.name.pos))
        param.decltpe.foreach(extractType(_, paramNode, EdgeType.VALUE_TYPE))
      }
    }
  }

  def extractTypeParam(param: Type.Param, parent: GraphNode, edgeType: String): Unit = param match {
    case Type.Param(
        mods: List[Mod],
        name: meta.Name,
        tparams: List[Type.Param],
        tbounds: Type.Bounds,
        vbounds: List[Type],
        cbounds: List[Type]
        ) =>
      val paramNodeOpt = extractName(name)
      paramNodeOpt.foreach { paramNode =>
        graphAccumulator.upsertEdge(parent.id, paramNode.id, edgeType, posToLocation(name.pos))
        tparams.foreach(extractTypeParam(_, paramNode, edgeType))
        extractBounds(tbounds, paramNode, edgeType)
        vbounds.foreach(extractType(_, paramNode, edgeType))
        cbounds.foreach(extractType(_, paramNode, edgeType))
      }
  }

  def extractBounds(bounds: Type.Bounds, parent: GraphNode, edgeType: String): Unit = {
    bounds.lo.foreach(extractType(_, parent, edgeType))
    bounds.hi.foreach(extractType(_, parent, edgeType))
  }

  def extractTerm(term: Term, parent: GraphNode): Unit = term match {
    case termName: Term.Name =>
      extractTermName(termName, parent)
    case Term.This(qual: scala.meta.Name) => //TODO
    //extractName(qual)
    case Term.Super(thisp: scala.meta.Name, superp: scala.meta.Name) =>
    case termSelect: Term.Select =>
      extractTerm(termSelect.qual, parent)
      extractTerm(termSelect.name, parent)
    case Term.Interpolate(prefix: Name, parts: List[Lit], args: List[Term]) =>
      //TODO prefix and parts
      args.foreach(extractTerm(_, parent))
    case Term.Apply(fun: Term, args: List[Term]) =>
      args.foreach(extractTerm(_, parent))
      extractTerm(fun, parent)
    case Term.ApplyType(fun: Term, targs: List[Type]) =>
      fun match {
        case termName: Term.Name
            if termName.value == "classOf" => // https://github.com/scalameta/scalameta/pull/1910 this bug is still real
          extractTerm(fun, parent)
        case other =>
          targs.foreach(extractType(_, parent, EdgeType.CALL))
          extractTerm(other, parent)
      }
    case Term.ApplyInfix(lhs: Term, op: Name, targs: List[Type], args: List[Term]) =>
      extractTerm(lhs, parent)
      extractTerm(op, parent)
      targs.foreach(extractType(_, parent, EdgeType.CALL))
      args.foreach(extractTerm(_, parent))
    case Term.ApplyUnary(op: Name, arg: Term) =>
      //TODO what about unary op extraction?
      extractTerm(op, parent)
    case Term.Assign(lhs: Term, rhs: Term) =>
      extractTerm(rhs, parent)
      extractTerm(lhs, parent)
    case Term.Return(expr: Term) =>
      extractTerm(expr, parent)
    case Term.Throw(expr: Term) =>
      extractTerm(expr, parent)
    case Term.Ascribe(expr: Term, tpe: Type) =>
      extractType(tpe, parent, EdgeType.CALL)
      extractTerm(expr, parent)
    case Term.Annotate(expr: Term, annots: List[Mod.Annot]) =>
      //TODO annots
      extractTerm(expr, parent)
    case Term.Tuple(args: List[Term]) =>
      args.foreach(extractTerm(_, parent))
    case Term.Block(stats: List[Stat]) =>
      stats.foreach(extractStat(_, parent))
    case Term.If(cond: Term, thenp: Term, elsep: Term) =>
      extractTerm(cond, parent)
      extractTerm(thenp, parent)
      extractTerm(elsep, parent)
    case Term.Match(expr: Term, cases: List[scala.meta.Case]) =>
      extractTerm(expr, parent)
      cases.foreach(extractCase(_, parent))
    case Term.Try(expr: Term, catchp: List[Case], finallyp: Option[Term]) =>
      extractTerm(expr, parent)
      catchp.foreach(extractCase(_, parent))
      finallyp.foreach(extractTerm(_, parent))
    case Term.TryWithHandler(expr: Term, catchp: Term, finallyp: Option[Term]) =>
      extractTerm(expr, parent)
      extractTerm(catchp, parent)
      finallyp.foreach(extractTerm(_, parent))
    case f @ Term.Function(params: List[Term.Param], body: Term) =>
      val symbol = s"localFunction$counter"
      val functionNode = graphAccumulator.addNodeProperties(
        graphAccumulator
          .createAggregationNode(symbol, symbol, "TERM_FUNCTION", uri, posToLocation(f.pos)),
        Map("LOC" -> posToLOC(f.pos).toString)
      )
      graphAccumulator.upsertEdge(parent.id, functionNode.id, EdgeType.AGGREGATE, posToLocation(f.pos))
      counter += 1
      params.foreach(extractTermParam(_, functionNode, EdgeType.METHOD_PARAMETER))
      extractTerm(body, functionNode)
    case Term.PartialFunction(cases: List[Case]) =>
      cases.foreach(extractCase(_, parent))
    case Term.While(expr: Term, body: Term) =>
      extractTerm(expr, parent)
      extractTerm(body, parent)
    case Term.Do(body: Term, expr: Term) =>
      extractTerm(body, parent)
      extractTerm(expr, parent)
    case Term.For(enums: List[scala.meta.Enumerator], body: Term) =>
      enums.foreach(extractEnumerator(_, parent))
      extractTerm(body, parent)
    case Term.ForYield(enums: List[Enumerator], body: Term) =>
      enums.foreach(extractEnumerator(_, parent))
      extractTerm(body, parent)
    case Term.New(init: Init) =>
      extractInit(init, parent, EdgeType.CALL) //TODO verify
    case Term.NewAnonymous(templ: Template) =>
      extractTemplate(templ, parent)
    case Term.Placeholder() =>
    case Term.Eta(expr: Term) =>
      extractTerm(expr, parent)
    case Term.Repeated(expr: Term) =>
      extractTerm(expr, parent)
    case _: Lit              => // ignore for now
    case _: Term.Interpolate => // ignore
    case termTuple: Term.Tuple =>
      termTuple.args.foreach(extractTerm(_, parent))
    case _new: Term.New =>
      extractType(_new.init.tpe, parent, EdgeType.CALL) //TODO verify
      _new.init.argss.flatten.foreach(extractTerm(_, parent))
    case assign: Term.Assign =>
      extractTerm(assign.lhs, parent)
      extractTerm(assign.rhs, parent)
    case unsupported =>
      logger.debug(s"Unsupported term: ${unsupported.getClass()} ${posToLocation(unsupported.pos)}");
  }

  def extractTermName(termName: Term.Name, parent: GraphNode): Option[GraphNode] = {
    val pos      = termName.pos
    val location = posToLocation(pos)
    val occurence = semanticDbHelper
      .findOccurence(termName.value, pos.startLine, pos.startColumn, pos.endLine, pos.endColumn)
    occurence.flatMap { o =>
      if (o.role == Role.DEFINITION) {
        val symbol = semanticDbHelper.findSymbol(o.symbol)
        if (symbol.isLocal) { // if it is not local, we will already have a declaration edge
          graphAccumulator.addEdge(
            parent.id,
            Edge(graphAccumulator.createId(o.symbol, uri), EdgeType.DECLARATION, Some(location))
          )
        }
        Some(graphAccumulator.getNode(o.symbol))
      } else {
        graphAccumulator
          .addEdge(parent.id, Edge(graphAccumulator.createId(o.symbol, uri), EdgeType.CALL, Some(location)))
        None
      }
    }
  }

  def extractTemplate(template: scala.meta.Template, parent: GraphNode) = template match {
    case Template(early: List[Stat], inits: List[Init], self: Self, stats: List[Stat]) =>
      early.foreach(extractStat(_, parent))
      inits.foreach(extractInit(_, parent))
      stats.foreach(extractStat(_, parent))
  }

  def extractCase(_case: Case, parent: GraphNode): Unit = _case match {
    case scala.meta.Case(pat: Pat, cond: Option[Term], body: Term) =>
      extractPattern(pat, parent)
      cond.foreach(extractTerm(_, parent))
      extractTerm(body, parent)
  }

  def extractEnumerator(enumerator: Enumerator, parent: GraphNode): Unit = enumerator match {
    case Enumerator.Generator(pat: Pat, rhs: Term) =>
      extractPattern(pat, parent)
      extractTerm(rhs, parent)
    case Enumerator.Val(pat: Pat, rhs: Term) =>
      extractPattern(pat, parent)
      extractTerm(rhs, parent)
    case Enumerator.Guard(cond: Term) =>
      extractTerm(cond, parent)
  }

  def extractName(name: Name): Option[GraphNode] = {
    semanticDbHelper.findSymbol(name.pos, name.value).map {
      case (symbol, occurence) =>
        graphAccumulator.getNode(symbol.symbol)
    }
  }

  def extractTypeName(name: Type.Name, parent: GraphNode, edgeType: String): Unit = {
    val pos      = name.pos
    val location = posToLocation(pos)
    val occurence = semanticDbHelper
      .findOccurence(name.value, pos.startLine, pos.startColumn, pos.endLine, pos.endColumn)
    occurence.foreach { o =>
      val childId = graphAccumulator.createId(o.symbol, uri)
      graphAccumulator
        .upsertEdge(parent.id, childId, edgeType, location)
    }
  }

  def extractTypeNameDeclaration(name: Type.Name, parent: GraphNode, edgeType: String): Option[GraphNode] = {
    val pos      = name.pos
    val location = posToLocation(pos)
    val occurence = semanticDbHelper
      .findOccurence(name.value, pos.startLine, pos.startColumn, pos.endLine, pos.endColumn)
    occurence.flatMap(o => graphAccumulator.findNode(o.symbol)).map { typeNode =>
      graphAccumulator
        .upsertEdge(parent.id, typeNode.id, edgeType, location)
      typeNode
    }
  }

  def extractType(_type: Type, parent: GraphNode, edgeType: String): Unit = _type match {
    case name @ Type.Name(value: String) =>
      extractTypeName(name, parent, edgeType)
    case Type.Select(qual: Term.Ref, name: Type.Name) =>
      extractTerm(qual, parent)
      extractTypeName(name, parent, edgeType)
    case Type.Project(qual: Type, name: Type.Name) =>
      extractType(qual, parent, edgeType)
      extractTypeName(name, parent, edgeType)
    case Type.Singleton(ref: Term.Ref) =>
      extractTerm(ref, parent) //TODO maybe Term.Ref should be tracked separately
    case Type.Apply(tpe: Type, args: List[Type]) =>
      extractType(tpe, parent, edgeType)
      args.foreach(extractType(_, parent, edgeType))
    case Type.ApplyInfix(lhs: Type, op: Name, rhs: Type) =>
      extractType(lhs, parent, edgeType)
      extractTypeName(op, parent, edgeType)
      extractType(rhs, parent, edgeType)
    case Type.Function(params: List[Type], res: Type) =>
      params.foreach(extractType(_, parent, edgeType))
      extractType(res, parent, edgeType)
    case Type.ImplicitFunction(params: List[Type], res: Type) =>
      params.foreach(extractType(_, parent, edgeType))
      extractType(res, parent, edgeType)
    case Type.Tuple(args: List[Type]) =>
      args.foreach(extractType(_, parent, edgeType))
    case Type.With(lhs: Type, rhs: Type) =>
      extractType(lhs, parent, edgeType)
      extractType(rhs, parent, edgeType)
    case Type.And(lhs: Type, rhs: Type) =>
      extractType(lhs, parent, edgeType)
      extractType(rhs, parent, edgeType)
    case Type.Or(lhs: Type, rhs: Type) =>
      extractType(lhs, parent, edgeType)
      extractType(rhs, parent, edgeType)
    case Type.Refine(tpe: Option[Type], stats: List[Stat]) =>
      tpe.foreach(extractType(_, parent, edgeType))
      stats.foreach(extractStat(_, parent))
    case Type.Existential(tpe: Type, stats: List[Stat]) =>
      extractType(tpe, parent, edgeType)
      stats.foreach(extractStat(_, parent))
    case Type.Annotate(tpe: Type, annots: List[Mod.Annot]) =>
      extractType(tpe, parent, edgeType)
    //TODO annots
    case Type.Lambda(tparams: List[Type.Param], tpe: Type) =>
      extractType(tpe, parent, edgeType)
      tparams.foreach(extractTypeParam(_, parent, edgeType))
    case Type.Method(paramss: List[List[Term.Param]], tpe: Type) =>
      extractType(tpe, parent, edgeType)
      paramss.flatten.foreach(extractTermParam(_, parent, edgeType))
    case Type.Placeholder(bounds: Type.Bounds) =>
      bounds.lo.foreach(extractType(_, parent, edgeType))
      bounds.hi.foreach(extractType(_, parent, edgeType))
    case Type.ByName(tpe: Type) =>
      extractType(tpe, parent, edgeType)
    case Type.Repeated(tpe: Type) =>
      extractType(tpe, parent, edgeType)
    case Type.Var(name: Name) =>
      extractTypeName(name, parent, edgeType)
    case unsupported => logger.debug(s"Unsupported type: ${unsupported.getClass()} ${posToLocation(unsupported.pos)}")
  }

  def posToLocation(pos: Position): Location =
    Location(uri, pos.startLine, pos.startColumn, pos.endLine, pos.endColumn)

  def posToLOC(pos: Position): Int =
    pos.endLine - pos.startLine + 1

  def getNodeWithLOC(symbol: String, pos: Position): GraphNode =
    graphAccumulator.addNodeProperties(graphAccumulator.getNode(symbol), Map("LOC" -> posToLOC(pos).toString))
}
