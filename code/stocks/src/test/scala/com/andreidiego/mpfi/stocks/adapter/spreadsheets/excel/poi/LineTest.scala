package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.CellType.{BLANK, FORMULA, NUMERIC, STRING}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.Outcome
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.TryValues.*

import java.io.File
import scala.language.deprecated.symbolLiterals
import scala.util.Try

// TODO Replace Try + exceptions with Validated
class LineTest extends FixtureAnyFreeSpec :

  import LineTest.*

  override protected type FixtureParam = XSSFSheet

  override protected def withFixture(test: OneArgTest): Outcome =
    val testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

    try withFixture(test.toNoArgTest(testWorkbook.getSheet(VALID_TINY_WORKSHEET)))
    finally testWorkbook.close()

  "A Line should" - {
    "be built from a POI Row and a size." in { _ ⇒
      val poiRegularRow = new XSSFWorkbook().createSheet("1").createRow(0)

      "Line.from(poiRegularRow, 1)" should compile
    }
    "be successfully built when given a POI Row that contains" - {
      "strings." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_STRING)) should be(STRING_VALUE)
      }
      "integers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_INTEGER)) should be(INTEGER_VALUE)
      }
      "floating point numbers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_FLOATING_POINT_NUMBER)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_FLOATING_POINT_NUMBER)) should be(FLOATING_POINT_VALUE)
      }
      "dates." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_DATE)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_DATE)) should be(DATE_VALUE)
      }
      "currencies." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_CURRENCY)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_CURRENCY)) should be(CURRENCY_VALUE)
      }
      "blanks." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_BLANK)) should be(BLANK_VALUE)
      }
      "separators." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_SEPARATOR)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_SEPARATOR)) should be(SEPARATOR_VALUE)
      }
      "string formulas." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_STRING_FORMULA)) should be(STRING_FORMULA_VALUE)
      }
      "numeric formulas that result in integers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_INTEGER_FORMULA)) should be(INTEGER_FORMULA_VALUE)
      }
      "numeric formulas that result in floating point numbers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_FLOATING_POINT_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_FLOATING_POINT_FORMULA)) should be(FLOATING_POINT_FORMULA_VALUE)
      }
      "a POI Cell that is null." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_NULL)
        assume(poiRegularRow.getCell(0) == null)

        valueOfFirstCellOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_NULL)) should be(BLANK_VALUE)
      }
      "multiple cells" - {
        "all blanks." in { poiWorksheet =>
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_BLANKS)

          valuesOf(cellsOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_MULTIPLE_BLANKS))) should contain theSameElementsInOrderAs LINE_WITH_ONLY_BLANK_CELLS
        }
        "all separators." in { poiWorksheet =>
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_SEPARATORS)

          valuesOf(cellsOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_MULTIPLE_SEPARATORS))) should contain theSameElementsInOrderAs LINE_WITH_ONLY_SEPARATOR_CELLS
        }
      }
    }
    "fail to be built when" - {
      "given a sequence of strings instead of a POI Row." in { _ =>
        """Line(Seq(("Address", "Value", "Type", "Mask", "Formula", "Note", "FontColor", "BackgroundColor")))""" shouldNot compile
      }
      "given a POI Row" - {
        "that" - {
          "is" - {
            "null." in { _ =>
              val exception = Line.from(null, ZERO).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Invalid line found.")
              )

              exception.getCause should have(
                'class(classOf[NullPointerException]),
                'message("""Cannot invoke "org.apache.poi.xssf.usermodel.XSSFRow.getRowNum()" because "poiRow$1" is null""")
              )
            }
            "empty (no cells)." in { _ =>
              val poiRegularRow = new XSSFWorkbook().createSheet("1").createRow(0)

              val exception = Line.from(poiRegularRow, ZERO).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Invalid line found.")
              )

              exception.getCause should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Line ${poiRegularRow.getRowNum} does not seem to have any cells.")
              )
            }
          }
        }
      }
    }
    "always have" - {
      "as many cells as its 'size'." - {
        "Not more." in { poiWorksheet ⇒
          val poiRowWithTrailingEmptyCell = poiWorksheet.getRow(INDEX_OF_LINE_WITH_TRAILING_EMPTY_CELL)
          val sizeThatExcludesTrailingEmptyCell = SIZE_OF_LINE_WITH_TRAILING_EMPTY_CELL - 1

          cellsOf(Line.from(poiRowWithTrailingEmptyCell, sizeThatExcludesTrailingEmptyCell)) should have size sizeThatExcludesTrailingEmptyCell
        }
        "Not less." in { poiWorksheet ⇒
          val poiRowWithOneCell = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
          val SIZE_OF_LINE_WITH_STRING_PLUS_ONE = SIZE_OF_LINE_WITH_STRING + 1
          val newLineCells = cellsOf(Line.from(poiRowWithOneCell, SIZE_OF_LINE_WITH_STRING_PLUS_ONE))

          newLineCells should have size SIZE_OF_LINE_WITH_STRING_PLUS_ONE
          newLineCells.last.`type` should be(POI_BLANK)
        }
      }
      "cells which" - {
        "should always have" - {
          "an address" in { poiWorksheet ⇒
            val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
            val expectedAddresses = Seq("A14", "B14", "C14", "D14")

            addressesOf(cellsOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_MULTIPLE_CELLS))) should contain theSameElementsInOrderAs expectedAddresses
          }
          "a type" in { poiWorksheet ⇒
            val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
            val expectedTypes = Seq(POI_NUMERIC, POI_STRING, POI_BLANK, POI_FORMULA)

            typesOf(cellsOf(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_MULTIPLE_CELLS))) should contain theSameElementsInOrderAs expectedTypes
          }
        }
        "could sometimes have" - {
          "a value" in { poiWorksheet ⇒
            val poiRowWithValue = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
            val poiRowWithNoValue = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

            val value = valueOfFirstCellOf(Line.from(poiRowWithValue, SIZE_OF_LINE_WITH_STRING))
            val noValue = valueOfFirstCellOf(Line.from(poiRowWithNoValue, SIZE_OF_LINE_WITH_BLANK))

            value should be(STRING_VALUE)
            noValue should be(empty)
          }
          "a mask" in { poiWorksheet ⇒
            val poiRowWithMask = poiWorksheet.getRow(INDEX_OF_LINE_WITH_DATE)
            val poiRowWithNoMask = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

            val mask = maskOfFirstCellOf(Line.from(poiRowWithMask, SIZE_OF_LINE_WITH_DATE))
            val noMask = maskOfFirstCellOf(Line.from(poiRowWithNoMask, SIZE_OF_LINE_WITH_STRING))

            mask should be(MASK)
            noMask should be(empty)
          }
          "a formula" in { poiWorksheet ⇒
            val poiRowWithFormula = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING_FORMULA)
            val poiRowWithNoFormula = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

            val formula = formulaOfFirstCellOf(Line.from(poiRowWithFormula, SIZE_OF_LINE_WITH_STRING_FORMULA))
            val noFormula = formulaOfFirstCellOf(Line.from(poiRowWithNoFormula, SIZE_OF_LINE_WITH_STRING))

            formula should be(STRING_FORMULA)
            noFormula should be(empty)
          }
          "a note" in { poiWorksheet ⇒
            val poiRowWithNote = poiWorksheet.getRow(INDEX_OF_LINE_WITH_NOTE)
            val poiRowWithNoNote = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

            val note = noteOfFirstCellOf(Line.from(poiRowWithNote, SIZE_OF_LINE_WITH_NOTE))
            val noNote = noteOfFirstCellOf(Line.from(poiRowWithNoNote, SIZE_OF_LINE_WITH_STRING))

            note should be(NOTE)
            noNote should be(empty)
          }
          "color of" - {
            "font" in { poiWorksheet ⇒
              val poiRowWithFontColorRed = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
              val poiRowWithFontColorAutomatic = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
              val poiRowWithNoFontColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

              val fontColor1 = fontColorOfFirstCellOf(Line.from(poiRowWithFontColorRed, SIZE_OF_LINE_WITH_STRING))
              val fontColor2 = fontColorOfFirstCellOf(Line.from(poiRowWithFontColorAutomatic, SIZE_OF_LINE_WITH_MULTIPLE_CELLS))
              val noFontColor = fontColorOfFirstCellOf(Line.from(poiRowWithNoFontColor, SIZE_OF_LINE_WITH_BLANK))

              fontColor1 should be(FONT_COLOR_RED)
              fontColor2 should be(FONT_COLOR_AUTOMATIC)
              noFontColor should be(empty)
            }
            "background" in { poiWorksheet ⇒
              val poiRowWithBackgroundColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_SEPARATOR)
              val poiRowWithNoBackgroundColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

              val backgroundColor = backgroundColorOfFirstCellOf(Line.from(poiRowWithBackgroundColor, SIZE_OF_LINE_WITH_SEPARATOR))
              val noBackgroundColor = backgroundColorOfFirstCellOf(Line.from(poiRowWithNoBackgroundColor, SIZE_OF_LINE_WITH_STRING))

              backgroundColor should be(BACKGROUND_COLOR)
              noBackgroundColor should be(empty)
            }
          }
        }
      }
    }
    "equal another Line with the same configuration." in { poiWorksheet ⇒
      val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

      Line.from(poiRegularRow, SIZE_OF_LINE_WITH_STRING) should equal(Line.from(poiRegularRow, SIZE_OF_LINE_WITH_STRING))
    }
    "not equal another Line with a different configuration." in { poiWorksheet ⇒
      val poiStringRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
      val poiIntegerRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER)

      Line.from(poiStringRow, SIZE_OF_LINE_WITH_STRING) should not equal Line.from(poiIntegerRow, SIZE_OF_LINE_WITH_INTEGER)
    }
    "forbid manipulation of its internal cells." in { poiWorksheet ⇒
      val poiRegularRow = poiWorksheet.getRow(0)

      """Line.from(poiRegularRow).success.value.cells = Seq("", "")""" shouldNot compile
    }
  }

object LineTest:
  private val TEST_SPREADSHEET = "Line.xlsx"
  private val VALID_TINY_WORKSHEET = "ValidTinyWorksheet"

  private val ZERO = 0
  private val INDEX_OF_LINE_WITH_STRING = 0
  private val SIZE_OF_LINE_WITH_STRING = 1
  private val STRING_VALUE = "String"
  private val FONT_COLOR_RED = "255,0,0"
  private val FONT_COLOR_AUTOMATIC = "0,0,0"
  private val INDEX_OF_LINE_WITH_INTEGER = 1
  private val SIZE_OF_LINE_WITH_INTEGER = 1
  private val INTEGER_VALUE = "78174"
  private val INDEX_OF_LINE_WITH_FLOATING_POINT_NUMBER = 2
  private val SIZE_OF_LINE_WITH_FLOATING_POINT_NUMBER = 1
  private val FLOATING_POINT_VALUE = "2.6"
  private val INDEX_OF_LINE_WITH_DATE = 3
  private val SIZE_OF_LINE_WITH_DATE = 1
  private val DATE_VALUE = "05/11/2008"
  private val MASK = "m/d/yy"
  private val INDEX_OF_LINE_WITH_CURRENCY = 4
  private val SIZE_OF_LINE_WITH_CURRENCY = 1
  private val CURRENCY_VALUE = "25,19"
  private val INDEX_OF_LINE_WITH_BLANK = 5
  private val SIZE_OF_LINE_WITH_BLANK = 1
  private val BLANK_VALUE = ""
  private val INDEX_OF_LINE_WITH_SEPARATOR = 6
  private val SIZE_OF_LINE_WITH_SEPARATOR = 1
  private val SEPARATOR_VALUE = ""
  private val BACKGROUND_COLOR = "231,230,230"
  private val INDEX_OF_LINE_WITH_STRING_FORMULA = 7
  private val SIZE_OF_LINE_WITH_STRING_FORMULA = 1
  private val STRING_FORMULA = "_xlfn.CONCAT(A1,A1)"
  private val STRING_FORMULA_VALUE = "StringString"
  private val INDEX_OF_LINE_WITH_INTEGER_FORMULA = 8
  private val SIZE_OF_LINE_WITH_INTEGER_FORMULA = 1
  private val INTEGER_FORMULA_VALUE = "156348"
  private val INDEX_OF_LINE_WITH_FLOATING_POINT_FORMULA = 9
  private val SIZE_OF_LINE_WITH_FLOATING_POINT_FORMULA = 1
  private val FLOATING_POINT_FORMULA_VALUE = "5.2"
  private val INDEX_OF_LINE_WITH_NULL = 10
  private val SIZE_OF_LINE_WITH_NULL = 2
  private val INDEX_OF_LINE_WITH_MULTIPLE_BLANKS = 11
  private val SIZE_OF_LINE_WITH_MULTIPLE_BLANKS = 4
  private val LINE_WITH_ONLY_BLANK_CELLS = Seq("", "", "", "")
  private val INDEX_OF_LINE_WITH_MULTIPLE_SEPARATORS = 12
  private val SIZE_OF_LINE_WITH_MULTIPLE_SEPARATORS = 4
  private val LINE_WITH_ONLY_SEPARATOR_CELLS = Seq("", "", "", "")
  private val INDEX_OF_LINE_WITH_MULTIPLE_CELLS = 13
  private val SIZE_OF_LINE_WITH_MULTIPLE_CELLS = 4
  private val POI_STRING = STRING.toString
  private val POI_BLANK = BLANK.toString
  private val POI_NUMERIC = NUMERIC.toString
  private val POI_FORMULA = FORMULA.toString
  private val INDEX_OF_LINE_WITH_NOTE = 14
  private val SIZE_OF_LINE_WITH_NOTE = 1
  private val NOTE = "Uma nota de exemplo"
  private val INDEX_OF_LINE_WITH_TRAILING_EMPTY_CELL = 15
  private val SIZE_OF_LINE_WITH_TRAILING_EMPTY_CELL = 3

  private def cellsOf(line: Try[Line]): Seq[Cell] = line.success.value.cells

  private def valuesOf(cells: Seq[Cell]): Seq[String] = cells.map(_.value)

  private def addressesOf(cells: Seq[Cell]): Seq[String] = cells.map(_.address)

  private def typesOf(cells: Seq[Cell]): Seq[String] = cells.map(_.`type`)

  private def valueOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.value

  private def maskOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.mask

  private def formulaOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.formula

  private def noteOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.note

  private def fontColorOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.fontColor

  private def backgroundColorOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head.backgroundColor