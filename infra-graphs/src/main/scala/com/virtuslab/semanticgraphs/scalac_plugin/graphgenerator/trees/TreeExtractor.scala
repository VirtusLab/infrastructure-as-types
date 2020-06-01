package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.trees

import java.nio.file.FileSystems

import scala.meta._
import scala.meta.internal.semanticdb.TextDocument

object TreeExtractor {

  def extractTree(file: String): Source = {
    val path        = FileSystems.getDefault.getPath(file)
    val bytes       = java.nio.file.Files.readAllBytes(path)
    val stringFile  = new String(bytes, "UTF-8")
    val input       = Input.VirtualFile(path.toString, stringFile)
    val exampleTree = input.parse[Source].get

    exampleTree
  }
}
