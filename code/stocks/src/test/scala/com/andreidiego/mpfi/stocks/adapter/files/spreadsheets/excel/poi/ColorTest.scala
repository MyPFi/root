package com.andreidiego.mpfi.stocks.adapter.files.spreadsheets.excel.poi

import Color.{Black, Blue, Crimson, White}
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import scala.Console.{BLUE, GREEN, RED}
import scala.language.deprecated.symbolLiterals

class ColorTest extends AnyWordSpec :

  def beCreatedFrom: AfterWord = afterWord("be created from")

  "A Color" can beCreatedFrom {
    "a hex RGB code" in {
      Color("#CD5C5C") should not be None
    }
    "a decimal RGB code" in {
      val RED = 205.toByte
      val GREEN = 92.toByte
      val BLUE = 92.toByte
      Color(RED, GREEN, BLUE) should not be None
    }
    "one of its several named constants" in {
      Color.Crimson should not be None
    }
  }
  it should {
    "not accept an hex RGB code that is not defined by one of its named constants" in {
      Color("#BD2B3F") should be(None)
    }
    "not accept a decimal RGB code that is not defined by one of its named constants" in {
      val RED = 251.toByte
      val GREEN = 228.toByte
      val BLUE = 213.toByte
      Color(RED, GREEN, BLUE) should be(None)
    }
    "provide its decimal RGB representation" in {
      Color.Crimson.dec should be(Array(220, 20, 60).map(_.toByte))
    }
    "provide its hex RGB representation" in {
      Color.Crimson.hex should be("#DC143C")
    }
    "define the 141 named colors supported by modern browsers" in {
      Color.values should have size 141
    }
    "have no duplicate hex value (other than the two pair of aliases Aqua/Cyan and Fuchsia/Magenta)among its named colors" in {
      Color.values.map(_.hex).distinct.size should equal(Color.values.size - 2)
    }
    "have no duplicate dec value (other than the two pair of aliases Aqua/Cyan and Fuchsia/Magenta) among its named colors" in {
      Color.values.map(_.dec.toSeq).distinct.size should equal(Color.values.size - 2)
    }
    "equal another Color with the same configuration" in {
      val RED = Crimson.dec(0)
      val GREEN = Crimson.dec(1)
      val BLUE = Crimson.dec(2)
      Color(RED, GREEN, BLUE) should equal (Color(RED, GREEN, BLUE))
    }
    "not equal another Color with a different configuration" in {
      val RED = Crimson.dec(0)
      val GREEN = Crimson.dec(1)
      val BLUE = Crimson.dec(2)
      Color(RED, GREEN, BLUE) should not equal Color(GREEN, RED, BLUE)
    }
  }