package com.virtuslab.iat.openapi

import com.virtuslab.iat.scala.TupleOps
import com.virtuslab.kubernetes.client.custom.B64Encoded
import com.virtuslab.kubernetes.client.openapi.api.EnumsSerializers
import com.virtuslab.kubernetes.client.openapi.core.Serializers
import org.json4s.{ DefaultFormats, Formats }

object json4s extends JValueProcessors with DefaultInterpreters with InterpreterOps with TupleOps {
  implicit val defaultFormats: Formats = DefaultFormats ++ EnumsSerializers.all ++
    Serializers.all + B64Encoded.json4sCustomSerializer
}
object interpreter extends DefaultInterpreters
object subinterpreter extends DefaultSubinterpreters
