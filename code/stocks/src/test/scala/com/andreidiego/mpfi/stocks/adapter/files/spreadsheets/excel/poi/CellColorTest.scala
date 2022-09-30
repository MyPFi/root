package com.andreidiego.mpfi.stocks.adapter.files.spreadsheets.excel.poi

import Color.{Black, Blue, Crimson, White}
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec

import scala.language.deprecated.symbolLiterals

class CellColorTest extends AnyWordSpec :

  def haveDefaultsFor: AfterWord = afterWord("have defaults for")
  def allowConfigurationOf: AfterWord = afterWord("")

  "A CellColor" should haveDefaultsFor {
    "the foreground color" in {
      CellColor() should have ('foreground (Black))
    }
    "the background color" in {
      CellColor() should have ('background (White))
    }
  }
  it should {
    "allow configuration of the foreground color" in {
      CellColor(foreground = Crimson) should have ('foreground (Crimson))
    }
    "allow configuration of the background color" in {
      CellColor(background = Blue) should have ('background (Blue))
    }
    "equal another CellColor with the same configuration" in {
      CellColor() should equal (CellColor(foreground = Black, background = White))
    }
    "not equal another CellColor with a different configuration" in {
      CellColor() should not equal CellColor(foreground = Blue, background = White)
    }
  }
/*
    it should ignore {
      "all can correctly tell its font color." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedFontColors = Iterator(
          "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("FONT-COLOR") should equal(expectedFontColors.next)
        }
      }
      "all can correctly tell its font color even when it's set to automatic." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 187
        val AUTOMATIC = "0,0,0"

        val expectedFontColors = Iterator(
          AUTOMATIC, "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("FONT-COLOR") should equal(expectedFontColors.next)
        }
      }
      "all can correctly tell its background color." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 130

        val expectedBackgroundColors = Iterator(
          "", "", "", "", "", "", "251,228,213", "251,228,213", "251,228,213", "251,228,213", "251,228,213", ""
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("BACKGROUND-COLOR") should equal(expectedBackgroundColors.next)
        }
      }
    }
  */
  /*
    "group" should {
      "" that {
        "" when {
          "" ignore { excelExtractor =>
          }
        }
      }
    }
    "double-checker" should {
      "" that {
        "" when {
          "" ignore { excelExtractor =>
          }
        }
      }
    }*/