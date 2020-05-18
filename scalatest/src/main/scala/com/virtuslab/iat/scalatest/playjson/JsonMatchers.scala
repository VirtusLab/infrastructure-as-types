package com.virtuslab.iat.scalatest.playjson

import diffson.jsonpatch._
import org.scalatest.matchers.{ MatchResult, Matcher }
import play.api.libs.json.Json.parse
import play.api.libs.json._

import scala.util.Try

trait JsonMatchers {

  def matchJson(right: JsValue): Matcher[JsValue] =
    Matcher[JsValue] { left =>
      MatchResult(
        matches = left == right,
        rawFailureMessage = "Json did not match" + diffMessage(left, right),
        rawNegatedFailureMessage = "Json should not have matched" + diffMessage(left, right)
      )
    }

  def matchJson(right: String): Matcher[JsValue] =
    Matcher[JsValue] { left =>
      Try(parse(right)).toOption match {
        case Some(rightJson) => matchJson(rightJson)(left)
        case _               => cantParseResult(Json.stringify(left), right.trim)
      }
    }

  def matchJsonString(right: String): Matcher[String] =
    Matcher[String] { left =>
      (Try(parse(left)).toOption, Try(parse(right)).toOption) match {
        case (Some(leftJson), Some(rightJson)) => matchJson(rightJson)(leftJson)
        case _                                 => cantParseResult(left, right)
      }
    }

  private def diffMessage(left: JsValue, right: JsValue): String = {
    import diffson._
    import diffson.lcs._
    import diffson.playJson._
    import diffson.jsonpatch.lcsdiff.remembering._

    implicit val lcs: Patience[JsValue] = new Patience[JsValue]

    import diffson.playJson.DiffsonProtocol._
    implicit val format: OFormat[JsonPatch[JsValue]] = Json.format[JsonPatch[JsValue]]

    val p: JsonPatch[JsValue] = diff(right, left)
    val operations = p.ops.map {
      case Add(path, value)  => s"+ ${Json.stringify(Json.toJson(path))}: ${Json.stringify(value)}\n"
      case Remove(path, old) => s"- ${Json.stringify(Json.toJson(path))}: ${old.fold("")(Json.stringify)}\n"
      case Replace(path, value, old) =>
        s"r ${Json.stringify(Json.toJson(path))}: ${Json.stringify(value)} -> ${old.fold("")(Json.stringify)}\n"
      case Move(from, path)  => s"m ${Json.stringify(Json.toJson(from))} -> ${Json.stringify(Json.toJson(path))}"
      case Copy(from, path)  => s"c ${Json.stringify(Json.toJson(from))} -> ${Json.stringify(Json.toJson(path))}"
      case Test(path, value) => s"t ${Json.stringify(Json.toJson(path))}: ${Json.stringify(value)}\n"
      case o                 => throw new UnsupportedOperationException(s"Unrecognized operation: $o")
    }

    val result = new StringBuilder(s""", expected:
        |${Json.prettyPrint(right)}
        |actual:
        |${Json.prettyPrint(left)}
        |
        |Diff (+ added, - removed, r replaced, m moved, c copied):
        |""".stripMargin)
    operations.foreach(result ++= _)
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
                  |{
                  |  "value" : 1,
                  |  "array" : [ 1, 2, 3 ],
                  |  "field" : "Hello"
                  |}
                  |actual:
                  |{
                  |  "value" : 1,
                  |  "field" : "Hello"
                  |}
                  |
                  |Diff (+ added, - removed, r replaced, m moved, c copied):
                  |- "/array": [1,2,3]
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
                  |{
                  |  "value" : 1,
                  |  "array" : [ 1, 2, 3 ],
                  |  "field" : "Hello"
                  |}
                  |actual:
                  |{
                  |  "value" : 1,
                  |  "array" : [ 1, 2, 3 ],
                  |  "field" : "Hello",
                  |  "new" : "should not be here"
                  |}
                  |
                  |Diff (+ added, - removed, r replaced, m moved, c copied):
                  |+ "/new": "should not be here"
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
                  |{
                  |  "value" : 1,
                  |  "array" : [ 1, 2, 3 ],
                  |  "field" : "Hello"
                  |}
                  |actual:
                  |{
                  |  "value" : 2,
                  |  "array" : [ 1, 2, 3 ],
                  |  "field" : "Bye!"
                  |}
                  |
                  |Diff (+ added, - removed, r replaced, m moved, c copied):
                  |r "/field": "Bye!" -> "Hello"
                  |r "/value": 2 -> 1
                  |""".stripMargin)
      }
    }
  }
}
