package com.virtuslab.semanticgraphs.scalac_plugin

import java.nio.file.Path

import com.virtuslab.semanticgraphs.scalac_plugin.proto.ProtobufExporter

import scala.meta.internal.semanticdb.scalac.SemanticdbOps
import scala.tools.nsc.Global
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.trees.TreeExtractor
import com.virtuslab.semanticgraphs.proto.model.graphnode.SemanticGraphFile
import java.io.File
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2.GraphAccumulator
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2.SemanticdbGraphExtractor
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2.SemanticdbHelper
import com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2.AstGraphExtractor

class SemanticGraphsGenerator(override val global: Global, projectRoot: Path) extends SemanticdbOps {

  def generateGraph(source: global.CompilationUnit): Unit = {

    val textDocument = source.toTextDocument
    val file = projectRoot.resolve(textDocument.uri).toString
    if (file.endsWith("GuestBook.scala")){
      val graphAccumulator = new GraphAccumulator(file)
      val helper = new SemanticdbHelper(textDocument, file)
      //new SemanticdbGraphExtractor(graphAccumulator, helper).createInitialGraphBasedOnSemanticDB()

      val tree = TreeExtractor.extractTree(file)
      val semanticGraphFile = new AstGraphExtractor(graphAccumulator, helper).augmentTheInitialGraph(tree)
      dumpFile(projectRoot, semanticGraphFile, file)
    }
  }

  def dumpFile(projectRoot: Path, semanticGraphFile: SemanticGraphFile, uri: String) = {
    val fileUri = s"${projectRoot.toAbsolutePath}/.semanticgraphs/${semanticGraphFile.uri}.semanticgraphdb"
    import java.io.FileOutputStream
    val file = new File(fileUri)
    file.getParentFile.mkdirs()
    file.createNewFile()
    val outputStream = new FileOutputStream(file, false)
    outputStream.write(semanticGraphFile.toByteArray)
    outputStream.close()
  }

}
