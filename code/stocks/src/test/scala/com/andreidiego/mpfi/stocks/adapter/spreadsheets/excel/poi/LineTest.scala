package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.CellType.{BLANK, FORMULA, NUMERIC, STRING}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.Outcome
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Inspectors.forAll
import org.scalatest.TryValues.*

import java.io.File
import scala.language.deprecated.symbolLiterals
import scala.util.{Failure, Try}

// TODO Replace Try + exceptions with Either
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
    "be built from a POI Row" in { ignoredPOIWorksheet ⇒
      val poiRegularRow = new XSSFWorkbook().createSheet("1").createRow(0)

      "Line.from(poiRegularRow)" should compile
    }
    "be successfully built when given a POI Row that contains" - {
      "strings." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(STRING_VALUE)
      }
      "integers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(INTEGER_VALUE)
      }
      "floating point numbers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_FLOATING_POINT_NUMBER)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(FLOATING_POINT_VALUE)
      }
      "dates." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_DATE)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(DATE_VALUE)
      }
      "currencies." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_CURRENCY)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(CURRENCY_VALUE)
      }
      "blanks." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(BLANK_VALUE)
      }
      "separators." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_SEPARATOR)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(SEPARATOR_VALUE)
      }
      "string formulas." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(STRING_FORMULA_VALUE)
      }
      "numeric formulas that result in integers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(INTEGER_FORMULA_VALUE)
      }
      "numeric formulas that result in floating point numbers." in { poiWorksheet =>
        val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_FLOATING_POINT_FORMULA)

        valueOfFirstCellOf(Line.from(poiRegularRow)) should be(FLOATING_POINT_FORMULA_VALUE)
      }
      "multiple cells" - {
        "all blanks." in { poiWorksheet =>
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_BLANKS)

          valuesOf(cellsOf(Line.from(poiRegularRow))) should contain theSameElementsInOrderAs LINE_WITH_ONLY_BLANK_CELLS
        }
        "all separators." in { poiWorksheet =>
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_SEPARATORS)

          valuesOf(cellsOf(Line.from(poiRegularRow))) should contain theSameElementsInOrderAs LINE_WITH_ONLY_SEPARATOR_CELLS
        }
      }
    }
    "fail to be built when" - {
      "given a sequence of strings instead of a POI Row." in { poiWorksheet =>
        """Line(Seq(("Address", "Value", "Type", "Mask", "Formula", "Note", "FontColor", "BackgroundColor")))""" shouldNot compile
      }
      "given a POI Row" - {
        "that" - {
          "is" - {
            "null." in { poiWorksheet =>
              val exception = Line.from(null).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Invalid line found.")
              )

              exception.getCause should have(
                'class(classOf[NullPointerException]),
                'message("""Cannot invoke "org.apache.poi.xssf.usermodel.XSSFRow.getRowNum()" because "poiRow$1" is null""")
              )
            }
            "empty (no cells)." in { poiWorksheet =>
              val poiRegularRow = new XSSFWorkbook().createSheet("1").createRow(0)

              val exception = Line.from(poiRegularRow).failure.exception

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
    "equal another Line with the same configuration." in { poiWorksheet ⇒
      val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

      Line.from(poiRegularRow) should equal(Line.from(poiRegularRow))
    }
    "not equal another Line with a different configuration." in { poiWorksheet ⇒
      val poiStringRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
      val poiIntegerRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_INTEGER)

      Line.from(poiStringRow) should not equal Line.from(poiIntegerRow)
    }
    "always have cells which" - {
      "should always have" - {
        "an address" in { poiWorksheet ⇒
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
          val expectedAddresses = Seq("A12", "B12", "C12", "D12")

          addressesOf(cellsOf(Line.from(poiRegularRow))) should contain theSameElementsInOrderAs expectedAddresses
        }
        "a type" in { poiWorksheet ⇒
          val poiRegularRow = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
          val expectedTypes = Seq(POI_NUMERIC, POI_STRING, POI_BLANK, POI_FORMULA)

          typesOf(cellsOf(Line.from(poiRegularRow))) should contain theSameElementsInOrderAs expectedTypes
        }
      }
      "could sometimes have" - {
        "a value" in { poiWorksheet ⇒
          val poiRowWithValue = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
          val poiRowWithNoValue = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

          val value = valueOfFirstCellOf(Line.from(poiRowWithValue))
          val noValue = valueOfFirstCellOf(Line.from(poiRowWithNoValue))

          value should be(STRING_VALUE)
          noValue should be(empty)
        }
        "a mask" in { poiWorksheet ⇒
          val poiRowWithMask = poiWorksheet.getRow(INDEX_OF_LINE_WITH_DATE)
          val poiRowWithNoMask = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

          val mask = maskOfFirstCellOf(Line.from(poiRowWithMask))
          val noMask = maskOfFirstCellOf(Line.from(poiRowWithNoMask))

          mask should be(MASK)
          noMask should be(empty)
        }
        "a formula" in { poiWorksheet ⇒
          val poiRowWithFormula = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING_FORMULA)
          val poiRowWithNoFormula = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

          val formula = formulaOfFirstCellOf(Line.from(poiRowWithFormula))
          val noFormula = formulaOfFirstCellOf(Line.from(poiRowWithNoFormula))

          formula should be(STRING_FORMULA)
          noFormula should be(empty)
        }
        "a note" in { poiWorksheet ⇒
          val poiRowWithNote = poiWorksheet.getRow(INDEX_OF_LINE_WITH_NOTE)
          val poiRowWithNoNote = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

          val note = noteOfFirstCellOf(Line.from(poiRowWithNote))
          val noNote = noteOfFirstCellOf(Line.from(poiRowWithNoNote))

          note should be(NOTE)
          noNote should be(empty)
        }
        "color of" - {
          "font" in { poiWorksheet ⇒
            val poiRowWithFontColorRed = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)
            val poiRowWithFontColorAutomatic = poiWorksheet.getRow(INDEX_OF_LINE_WITH_MULTIPLE_CELLS)
            val poiRowWithNoFontColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_BLANK)

            val fontColor1 = fontColorOfFirstCellOf(Line.from(poiRowWithFontColorRed))
            val fontColor2 = fontColorOfFirstCellOf(Line.from(poiRowWithFontColorAutomatic))
            val noFontColor = fontColorOfFirstCellOf(Line.from(poiRowWithNoFontColor))

            fontColor1 should be(FONT_COLOR_RED)
            fontColor2 should be(FONT_COLOR_AUTOMATIC)
            noFontColor should be(empty)
          }
          "background" in { poiWorksheet ⇒
            val poiRowWithBackgroundColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_SEPARATOR)
            val poiRowWithNoBackgroundColor = poiWorksheet.getRow(INDEX_OF_LINE_WITH_STRING)

            val backgroundColor = backgroundColorOfFirstCellOf(Line.from(poiRowWithBackgroundColor))
            val noBackgroundColor = backgroundColorOfFirstCellOf(Line.from(poiRowWithNoBackgroundColor))

            backgroundColor should be(BACKGROUND_COLOR)
            noBackgroundColor should be(empty)
          }
        }
      }
    }
    "forbid manipulation of its internal cells." in { poiWorksheet ⇒
      val poiRegularRow = poiWorksheet.getRow(0)

      """Line.from(poiRegularRow).success.value.cells = Seq("", "")""" shouldNot compile
    }
  }

object LineTest:
  private type Cell = (String, String, String, String, String, String, String, String)

  private val TEST_SPREADSHEET = "Line.xlsx"
  private val VALID_TINY_WORKSHEET = "ValidTinyWorksheet"

  private val INDEX_OF_LINE_WITH_STRING = 0
  private val STRING_VALUE = "String"
  private val FONT_COLOR_RED = "255,0,0"
  private val FONT_COLOR_AUTOMATIC = "0,0,0"
  private val INDEX_OF_LINE_WITH_INTEGER = 1
  private val INTEGER_VALUE = "78174"
  private val INDEX_OF_LINE_WITH_FLOATING_POINT_NUMBER = 2
  private val FLOATING_POINT_VALUE = "2.6"
  private val INDEX_OF_LINE_WITH_DATE = 3
  private val DATE_VALUE = "05/11/2008"
  private val MASK = "m/d/yy"
  private val INDEX_OF_LINE_WITH_CURRENCY = 4
  private val CURRENCY_VALUE = "25.19"
  private val INDEX_OF_LINE_WITH_BLANK = 5
  private val BLANK_VALUE = ""
  private val INDEX_OF_LINE_WITH_SEPARATOR = 6
  private val SEPARATOR_VALUE = ""
  private val BACKGROUND_COLOR = "231,230,230"
  private val INDEX_OF_LINE_WITH_STRING_FORMULA = 7
  private val STRING_FORMULA = "_xlfn.CONCAT(A1,A1)"
  private val STRING_FORMULA_VALUE = "StringString"
  private val INDEX_OF_LINE_WITH_INTEGER_FORMULA = 8
  private val INTEGER_FORMULA_VALUE = "156348"
  private val INDEX_OF_LINE_WITH_FLOATING_POINT_FORMULA = 9
  private val FLOATING_POINT_FORMULA_VALUE = "5.2"
  private val INDEX_OF_LINE_WITH_NOTE = 10
  private val NOTE = "Uma nota de exemplo"
  private val INDEX_OF_LINE_WITH_MULTIPLE_CELLS = 11
  private val POI_STRING = STRING.toString
  private val POI_BLANK = BLANK.toString
  private val POI_NUMERIC = NUMERIC.toString
  private val POI_FORMULA = FORMULA.toString
  private val INDEX_OF_LINE_WITH_MULTIPLE_BLANKS = 12
  private val INDEX_OF_LINE_WITH_MULTIPLE_SEPARATORS = 13
  private val LINE_WITH_ONLY_BLANK_CELLS = Seq("", "", "", "")
  private val LINE_WITH_ONLY_SEPARATOR_CELLS = Seq("", "", "", "")

  private def cellsOf(line: Try[Line]): Seq[Cell] = line.success.value.cells

  private def valuesOf(cells: Seq[Cell]): Seq[String] = cells.map(_._2)

  private def addressesOf(cells: Seq[Cell]): Seq[String] = cells.map(_._1)

  private def typesOf(cells: Seq[Cell]): Seq[String] = cells.map(_._3)

  private def valueOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._2

  private def maskOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._4

  private def formulaOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._5

  private def noteOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._6

  private def fontColorOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._7

  private def backgroundColorOfFirstCellOf(line: Try[Line]): String = cellsOf(line).head._8