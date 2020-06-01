package com.virtuslab.semanticgraphs.scalac_plugin.graphgenerator.analyzerv2

object EdgeType {
  val DECLARATION = "DECLARATION"
  val RETURN_TYPE = "RETURN_TYPE"
  val EXTEND = "EXTEND"
  val METHOD_PARAMETER = "METHOD_PARAMETER"
  val VALUE_TYPE = "VALUE_TYPE"
  val CALL = "CALL"
  val TYPE_PARAMETER = "TYPE_PARAMETER"

  val AGGREGATE = "AGGREGATE"
  val TYPE_UPPER_BOUND = "TYPE_UPPER_BOUND"
}