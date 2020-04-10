package com.virtuslab.kubernetes.client.custom

import org.json4s.{ Formats, NoTypeHints }
import org.json4s.jackson.Serialization
import org.specs2.mutable.Specification

class IntOrStringTest extends Specification {
  "Unit specification for the IntOrString model. ".br

  "An IntOrString can be constructed form an Int" >> {
    "where value is 0" >> { IntOrString(0).int mustEqual 0 }
    "where value is 1" >> { IntOrString(1).int mustEqual 1 }
    "where value is -1" >> { IntOrString(-1).int mustEqual -1 }
  }

  "An IntOrString can be constructed from a String" >> {
    "where value is empty" >> { IntOrString("").string mustEqual "" }
    "where value is test" >> { IntOrString("test").string mustEqual "test" }
  }

  import org.json4s.jackson.Serialization._
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + IntOrString.Serializer

  "An IntOrString can be marshalled to JSON" >> {
    "where a value of '10' is used" >> { write(IntOrString(10)) mustEqual "10" }
    "where a value of 'ten' is used" >> { write(IntOrString("ten")) mustEqual "\"ten\"" }
  }

  "An IntOrString can be un-marshalled from JSON" >> {
    "where a value of '10' is used" >> { read[IntOrString]("10") mustEqual IntOrString(10) }
    "where a value of 'ten' is used" >> { read[IntOrString]("\"ten\"") mustEqual IntOrString("ten") }
  }
}
