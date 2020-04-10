package com.virtuslab.kubernetes.client.custom

import com.google.gson.Gson
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable.Specification

class IntOrStringTest extends Specification with JsonMatchers {
  "Unit specification for the IntOrString model. ".txt

  "An IntOrString can be constructed form an Int\n" >> {
    "where value is 0" >> { IntOrString(0).int mustEqual 0 }
    "where value is 1" >> { IntOrString(1).int mustEqual 1 }
    "where value is -1" >> { IntOrString(-1).int mustEqual -1 }
  }

  "An IntOrString can be constructed from a String" >> {
    "where value is empty" >> { IntOrString("").string mustEqual "" }
    "where value is test" >> { IntOrString("test").string mustEqual "test" }
  }

  val gson: Gson = JSON.gson

  "An IntOrString can be marshalled to JSON\n" >> {
    "where a value of '10' is used" >> { gson.toJson(IntOrString(10)) mustEqual ("10") }
    "where a value of 'ten' is used" >> { gson.toJson(IntOrString("ten")) mustEqual ("\"ten\"") }
  }

  "An IntOrString can be un-marshalled from JSON\n" >> {
    "where a value of '10' is used" >> { gson.fromJson("10", classOf[IntOrString]) mustEqual IntOrString(10) }
    "where a value of 'ten' is used" >> { gson.fromJson("ten", classOf[IntOrString]) mustEqual IntOrString("ten") }
  }
}
