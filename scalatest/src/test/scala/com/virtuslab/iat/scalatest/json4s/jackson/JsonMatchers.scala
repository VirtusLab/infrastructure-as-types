package com.virtuslab.iat.scalatest.json4s.jackson

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.json4s.{ Diff, Formats, JValue }
import org.scalatest.matchers._

import scala.util.Try

trait JsonMatchers {

  def matchJson(right: JValue)(implicit formats: Formats): Matcher[JValue] =
    Matcher[JValue] { left =>
      MatchResult(
        matches = left == right,
        rawFailureMessage = "Json did not match" + diffMessage(left, right),
        rawNegatedFailureMessage = "Json should not have matched" + diffMessage(left, right)
      )
    }

  def matchJson(right: String)(implicit formats: Formats): Matcher[JValue] =
    Matcher[JValue] { left =>
      Try(parse(right)).toOption match {
        case Some(rightJson) => matchJson(rightJson)(formats)(left)
        case _               => cantParseResult(write(left), right)
      }
    }

  def matchJsonString(right: String)(implicit formats: Formats): Matcher[String] =
    Matcher[String] { left =>
      (Try(parse(left)).toOption, Try(parse(right)).toOption) match {
        case (Some(leftJson), Some(rightJson)) => matchJson(rightJson)(formats)(leftJson)
        case _                                 => cantParseResult(left, right)
      }
    }

  private def diffMessage(left: JValue, right: JValue)(implicit formats: Formats): String = {
    val Diff(changed, added, deleted) = right.diff(left)
    val result = new StringBuilder(s""", expected:
        |${write(right)}
        |actual:
        |${write(left)}
        |
        |Diff (~ changed, - deleted, + added):
        |""".stripMargin)
    if (changed.toOption.isDefined)
      result ++= s"~ ${write(changed)}\n"
    if (deleted.toOption.isDefined)
      result ++= s"- ${write(deleted)}\n"
    if (added.toOption.isDefined)
      result ++= s"+ ${write(added)}\n"
    result.toString
  }

  private def cantParseResult(left: String, right: String) = MatchResult(
    matches = false,
    rawFailureMessage = "Could not parse json {0} did not equal {1}",
    rawNegatedFailureMessage = "Json should not have matched {0} {1}",
    args = IndexedSeq(left.trim, right.trim)
  )
}

object JsonMatchers extends JsonMatchers

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class JsonSpec extends AnyFreeSpec with Matchers with JsonMatchers {

  implicit val formats: Formats = org.json4s.DefaultFormats

  val json: String =
    """
      |{
      |  "value": 1,
      |  "array": [1,2,3],
      |  "field": "Hello"
      |}
    """.stripMargin

  "Json matcher" - {
    "should not throw exception when string contains expected json" in {
      // given:
      val actual = json
      // when:
      actual.should(matchJsonString(json))
    }
    "should print absent part of expected json" in {
      // given:
      val actual =
        """
          |{
          |  "value": 1,
          |  "field": "Hello"
          |}
        """.stripMargin
      // when:
      try {
        actual.should(matchJsonString(json))
      } catch {
        case e: Throwable =>
          e.getMessage should
            be("""|Json did not match, expected:
                  |{"value":1,"array":[1,2,3],"field":"Hello"}
                  |actual:
                  |{"value":1,"field":"Hello"}
                  |
                  |Diff (~ changed, - deleted, + added):
                  |- {"array":[1,2,3]}
                  |""".stripMargin)
      }
    }
    "should print added part of json" in {
      // given:
      val actual =
        """
          |{
          |  "value": 1,
          |  "array": [1,2,3],
          |  "field": "Hello",
          |  "new": "should not be here"
          |}
        """.stripMargin
      // when:
      try {
        actual.should(matchJsonString(json))
      } catch {
        case e: Throwable =>
          e.getMessage should
            be("""|Json did not match, expected:
                  |{"value":1,"array":[1,2,3],"field":"Hello"}
                  |actual:
                  |{"value":1,"array":[1,2,3],"field":"Hello","new":"should not be here"}
                  |
                  |Diff (~ changed, - deleted, + added):
                  |+ {"new":"should not be here"}
                  |""".stripMargin)
      }
    }
    "should print different part of json" in {
      // given:
      val actual =
        """
          |{
          |  "value": 2,
          |  "array": [1,2,3],
          |  "field": "Bye!"
          |}
        """.stripMargin
      // when:
      try {
        actual.should(matchJsonString(json))
      } catch {
        case e: Throwable =>
          e.getMessage should
            be("""|Json did not match, expected:
                  |{"value":1,"array":[1,2,3],"field":"Hello"}
                  |actual:
                  |{"value":2,"array":[1,2,3],"field":"Bye!"}
                  |
                  |Diff (~ changed, - deleted, + added):
                  |~ {"value":2,"field":"Bye!"}
                  |""".stripMargin)
      }
    }
  }
}
