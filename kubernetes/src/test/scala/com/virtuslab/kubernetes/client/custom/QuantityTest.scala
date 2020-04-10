package com.virtuslab.kubernetes.client.custom

import org.json4s.jackson.Serialization
import org.json4s.{ Formats, NoTypeHints }
import org.specs2.mutable.Specification

// Adapted from skuber
class QuantityTest extends Specification {
  "This is a unit specification for the Quantity model. ".br

  "A quantity can be constructed from decimal SI values" >> {
    "where a value of 100m equates to 0.1" >> { Quantity("100m").amount mustEqual 0.1 }
    "where a value of 100k equates to 100000" >> { Quantity("100k").amount mustEqual 100000 }
    "where a value of 100M equates to 100000000" >> { Quantity("100M").amount mustEqual 100000000 }
    "where a value of 100G equates to 100E+9" >> { Quantity("100G").amount mustEqual 100e+9 }
    "where a value of 100T equates to 100E+12" >> { Quantity("100T").amount mustEqual 100e+12 }
    "where a value of 100P equates to 100E+15" >> { Quantity("100P").amount mustEqual 100e+15 }
    "where a value of 100E equates to 100E+18" >> { Quantity("100E").amount mustEqual 100e+18 }
  }

  "A quantity can be constructed from values with scientific E notation" >> {
    "where a value of 0.01E+5 equates to 1000" >> { Quantity("0.01E+5").amount mustEqual 1000 }
    "where a value of 10010.56E-3 equates to 10.01056" >> { Quantity("10010.56E-3").amount mustEqual 10.01056 }
    "where a value of 55.67e+6 equates to 55670000" >> { Quantity("55.67e+6").amount mustEqual 55670000 }
    "where a value of 5e+3 equates to 5000" >> { Quantity("5e+3").amount mustEqual 5000 }
    "where a value of 67700e-33 equates to 67.700" >> { Quantity("67700e-3").amount mustEqual 67.700 }
  }

  "A quantity can be constructed from binary SI values" >> {
    "where a value of 100Ki equates to 102400" >> { Quantity("100Ki").amount mustEqual 102400 }
    "where a value of 10Mi equates to 10485760" >> { Quantity("10Mi").amount mustEqual 10485760 }
    "where a value of 10Ti equates to 10 *(2 ^ 40) " >> { Quantity("10Ti").amount mustEqual 10 * Math.pow(2, 40) }
    "where a value of 10Pi equates to 10 *(2 ^ 50) " >> { Quantity("10Pi").amount mustEqual 10 * Math.pow(2, 50) }
    "where a value of 10Ei equates to 10 *(2 ^ 60) " >> {
      val multi = new java.math.BigInteger("10")
      val base = new java.math.BigInteger("2")
      val value = base.pow(60).multiply(multi)
      val decValue = scala.math.BigDecimal(new java.math.BigDecimal(value))
      System.err.println("....10Ei = " + decValue)
      Quantity("10Ei").amount mustEqual decValue
    }
  }

  "A quantity can be constructed for plain integer and decimal values with no suffixes" >> {
    "where a value of 10 is valid" >> { Quantity("10").amount mustEqual 10 }
    "where a value of -10 is valid" >> { Quantity("-10").amount mustEqual -10 }
    "where a value of 10.55 is valid" >> { Quantity("10.55").amount mustEqual 10.55 }
    "where a value of -10.55 is valid" >> { Quantity("-10.55").amount mustEqual -10.55 }
  }

  "A quantity will reject bad values" >> {
    "where constructing from a value of 10Zi results in an exception" >> {
      def badVal: BigDecimal = Quantity("10Zi").amount
      badVal must throwAn[Exception]
    }
    "where constructing from an empty value results in an exception" >> {
      def emptyVal: BigDecimal = Quantity("").amount
      emptyVal must throwAn[Exception]
    }
  }

  import org.json4s.jackson.Serialization._
  implicit val formats: Formats = Serialization.formats(NoTypeHints) + Quantity.Serializer

  "A quantity can be marshalled to JSON" >> {
    "where a value of 100Ki is used" >> { write(Quantity("100Ki")) mustEqual ("\"100Ki\"") }
    "where a value of 10 is used" >> { write(Quantity("10")) mustEqual ("\"10\"") }
  }

  "A quantity can be un-marshalled from JSON" >> {
    "where a value of 100Ki is used" >> { read[Quantity]("\"100Ki\"") mustEqual Quantity("100Ki") }
  }
}
