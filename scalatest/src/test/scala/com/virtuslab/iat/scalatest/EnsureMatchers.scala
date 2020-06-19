package com.virtuslab.iat.scalatest

import com.virtuslab.iat.scalatest.EnsureMatchers.zipper
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.should.Matchers

trait EnsureMatchers { self: AnyFlatSpecLike with Matchers =>

  trait Ensure[M, A] {
    def resources: Map[M, A]

    def ignore(p: M => Boolean): Ensure[M, A] = {
      resources.filter(e => p(e._1)).foreach {
        case (meta, _) => info(s"ignoring $meta")
      }
      Ensure(resources.filterNot(e => p(e._1)))
    }

    def contain(cases: (M, Matcher[A])*): Unit = contain(cases.toMap)

    def contain(cases: Map[M, Matcher[A]]): Unit = {
      zipper(resources, cases) {
        case (_, Some(actual), Some(expected)) => actual.should(expected)
        case (meta, Some(_), None)             => fail(s"unexpected $meta result (got the resource, but no test case)")
        case (meta, None, Some(_))             => fail(s"unexpected $meta test case (got the test case but no resource)")
        case (meta, None, None)                => fail(s"unexpected $meta -> None/None, this should never happen")
      }
    }
  }

  object Ensure {
    def apply[M, A](rs: Iterable[(M, A)]): Ensure[M, A] = new Ensure[M, A] {
      def resources: Map[M, A] = rs.toMap
    }
  }
}

object EnsureMatchers {
  def zipper[A, B, C, D](map1: Map[A, B], map2: Map[A, C])(f: (A, Option[B], Option[C]) => D): Map[A, D] =
    (for (key <- map1.keys ++ map2.keys) yield key -> f(key, map1.get(key), map2.get(key))).toMap
}
