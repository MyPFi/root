package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage

import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Outcome
import org.scalatest.TryValues.*

import java.io.File
import language.deprecated.symbolLiterals
import scala.util.Try

// TODO Replace Try + exceptions with Validated
class CellTest extends FixtureAnyFreeSpec :

  import CellTest.*

  override protected type FixtureParam = XSSFRow

  override protected def withFixture(test: OneArgTest): Outcome =
    val testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(new File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

    try withFixture(test.toNoArgTest(testWorkbook.getSheet(CELL_WORKSHEET).getRow(0)))
    finally testWorkbook.close()

  "A Cell" - {
    "should" - {
      "be built from a POI Cell" in { _ ⇒
        val poiCell = new XSSFWorkbook().createSheet("1").createRow(0).createCell(0)

        "Cell.from(poiCell)" should compile
      }
      "be successfully built when given a POI Cell that contains" - {
        "a string." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          valueOf(Cell.from(poiCell)) should be(STRING_VALUE)
        }
        "an integer." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

          valueOf(Cell.from(poiCell)) should be(INTEGER_VALUE)
        }
        "a double." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

          valueOf(Cell.from(poiCell)) should be(DOUBLE_VALUE)
        }
        "a date." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE)

          valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
        }
        "currency." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY)

          valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
        }
        "blank." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

          valueOf(Cell.from(poiCell)) should be(BLANK_VALUE)
        }
        "a separator." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

          valueOf(Cell.from(poiCell)) should be(SEPARATOR_VALUE)
        }
        "a string formula." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

          valueOf(Cell.from(poiCell)) should be(STRING_FORMULA_VALUE)
        }
        "a numeric formula that results in an integer." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

          valueOf(Cell.from(poiCell)) should be(INTEGER_FORMULA_VALUE)
        }
        "a numeric formula that results in a double." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

          valueOf(Cell.from(poiCell)) should be(DOUBLE_FORMULA_VALUE)
        }
      }
      "fail to be built when given" - {
        "string arguments instead of a POI Cell." in { _ =>
          """Cell("Address", "Value", "Type", "Mask", "Formula", "Note", "FontColor", "BackgroundColor")""" shouldNot compile
        }
        "a POI Cell that is null." in { _ =>
          val exception = Cell.from(null).failure.exception

          exception should have(
            'class(classOf[IllegalArgumentException]),
            'message(s"Invalid cell found: null")
          )
        }
      }
      "be empty if its value is empty." in { poiRow ⇒
        val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

        Cell.from(emptyPOICell).success.value shouldBe empty
      }
      "not be empty if its value is not empty." in { poiRow ⇒
        val nonEmptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        assert(Cell.from(nonEmptyPOICell).success.value.isNotEmpty)
      }
      "equal another Cell with the same configuration." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        Cell.from(poiCell) should equal(Cell.from(poiCell))
      }
      "not equal another Cell with a different configuration." in { poiRow ⇒
        val poiCell1 = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)
        val poiCell2 = poiRow.getCell(INDEX_OF_CELL_WITH_FONT_COLOR_RED)

        Cell.from(poiCell1) should not equal Cell.from(poiCell2)
      }
      "forbid manipulation of its internal address." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.address = "address"""" shouldNot compile
      }
      "forbid manipulation of its internal value." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.value = "value"""" shouldNot compile
      }
      "forbid manipulation of its internal type." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.`type` = "type"""" shouldNot compile
      }
      "forbid manipulation of its internal mask." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.mask = "mask"""" shouldNot compile
      }
      "forbid manipulation of its internal formula." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.formula = "formula"""" shouldNot compile
      }
      "forbid manipulation of its internal note." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.note = "note"""" shouldNot compile
      }
      "forbid manipulation of its internal fontColor." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.fontColor = "fontColor"""" shouldNot compile
      }
      "forbid manipulation of its internal backgroundColor." in { poiRow ⇒
        val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

        """Cell.from(poiCell).success.value.backgroundColor = "backgroundColor"""" shouldNot compile
      }
      //    TODO Currency with no decimals, 1 decimal and 2 decimals
      "always have" - {
        "an address." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          addressOf(Cell.from(poiCell)) should be(ADDRESS_OF_CELL_WITH_STRING)
        }
        "type" - {
          "'STRING', when its underlying Excel value is" - {
            "empty, because it's" - {
              "an empty cell." in { poiRow ⇒
                val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

                typeOf(Cell.from(emptyPOICell)) should be(STRING)
              }
              "a separator." in { poiRow ⇒
                val separatorPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

                typeOf(Cell.from(separatorPOICell)) should be(STRING)
              }
            }
            "an alphanumeric string" - {
              "itself." in { poiRow ⇒
                val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

                typeOf(Cell.from(poiCellWithString)) should be(STRING)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

                typeOf(Cell.from(poiCellWithStringFormula)) should be(STRING)
              }
            }
          }
          "'INTEGER', when its underlying Excel value is" - {
            "an integer" - {
              "itself." in { poiRow ⇒
                val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

                typeOf(Cell.from(poiCellWithInteger)) should be(INTEGER)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

                typeOf(Cell.from(poiCellWithIntegerFormula)) should be(INTEGER)
              }
            }
            "an integer-shaped string" - {
              "itself." in { poiRow ⇒
                val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

                typeOf(Cell.from(poiCellWithIntegerShapedString)) should be(INTEGER)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

                typeOf(Cell.from(poiCellWithIntegerShapedStringFormula)) should be(INTEGER)
              }
            }
          }
        }
      }
    }
    "could sometimes" - {
      "have" - {
        "a value." in { poiRow ⇒
          val poiCellWithValue = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)
          val poiCellWithNoValue = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

          val value = valueOf(Cell.from(poiCellWithValue))
          val noValue = valueOf(Cell.from(poiCellWithNoValue))

          value should be(STRING_VALUE)
          noValue should be(empty)
        }
        "a mask." in { poiRow ⇒
          val poiCellWithMask = poiRow.getCell(INDEX_OF_CELL_WITH_DATE)
          val poiCellWithNoMask = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val mask = maskOf(Cell.from(poiCellWithMask))
          val noMask = maskOf(Cell.from(poiCellWithNoMask))

          mask should be(MASK)
          noMask should be(empty)
        }
        "a formula." in { poiRow ⇒
          val poiCellWithFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)
          val poiCellWithNoFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val formula = formulaOf(Cell.from(poiCellWithFormula))
          val noFormula = formulaOf(Cell.from(poiCellWithNoFormula))

          formula should be(STRING_FORMULA)
          noFormula should be(empty)
        }
        "a note." in { poiRow ⇒
          val poiCellWithNote = poiRow.getCell(INDEX_OF_CELL_WITH_NOTE)
          val poiCellWithNoNote = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val note = noteOf(Cell.from(poiCellWithNote))
          val noNote = noteOf(Cell.from(poiCellWithNoNote))

          note should be(NOTE)
          noNote should be(empty)
        }
        "color of" - {
          "font." in { poiRow ⇒
            val poiCellWithFontColorRed = poiRow.getCell(INDEX_OF_CELL_WITH_FONT_COLOR_RED)
            val poiCellWithFontColorAutomatic = poiRow.getCell(INDEX_OF_CELL_WITH_FONT_COLOR_AUTOMATIC)
            val poiCellWithNoFontColor = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

            val fontColor1 = fontColorOf(Cell.from(poiCellWithFontColorRed))
            val fontColor2 = fontColorOf(Cell.from(poiCellWithFontColorAutomatic))
            val noFontColor = fontColorOf(Cell.from(poiCellWithNoFontColor))

            fontColor1 should be(FONT_COLOR_RED)
            fontColor2 should be(FONT_COLOR_AUTOMATIC)
            noFontColor should be(empty)
          }
          "background." in { poiRow ⇒
            val poiCellWithBackgroundColor = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)
            val poiCellWithNoBackgroundColor = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

            val backgroundColor = backgroundColorOf(Cell.from(poiCellWithBackgroundColor))
            val noBackgroundColor = backgroundColorOf(Cell.from(poiCellWithNoBackgroundColor))

            backgroundColor should be(BACKGROUND_COLOR)
            noBackgroundColor should be(empty)
          }
        }
      }
      "be converted to" - {
        "'Int', when its underlying Excel value is" - {
          "an integer" - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              asInt(Cell.from(poiCellWithInteger)) should be(Some(INTEGER_VALUE.toInt))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              asInt(Cell.from(poiCellWithIntegerFormula)) should be(Some(INTEGER_FORMULA_VALUE.toInt))
            }
          }
          "an integer-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              asInt(Cell.from(poiCellWithIntegerShapedString)) should be(Some(INTEGER_SHAPED_STRING_VALUE.toInt))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              asInt(Cell.from(poiCellWithIntegerShapedStringFormula)) should be(Some(INTEGER_SHAPED_STRING_FORMULA_VALUE.toInt))
            }
          }
        }
      }
      "not be converted to" - {
        "'Int', for instance, when its underlying Excel value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            asInt(Cell.from(emptyPOICell)) should be(None)
          }
          "a double." - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              asInt(Cell.from(poiCellWithDouble)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              asInt(Cell.from(poiCellWithDoubleFormula)) should be(None)
            }
          }
          "a double-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

              asInt(Cell.from(poiCellWithDoubleShapedString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

              asInt(Cell.from(poiCellWithDoubleShapedStringFormula)) should be(None)
            }
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              asInt(Cell.from(poiCellWithString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              asInt(Cell.from(poiCellWithStringFormula)) should be(None)
            }
          }
          "a date." in { poiRow ⇒
            val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE)

            asInt(Cell.from(poiCellWithDate)) should be(None)
          }
          "a currency." in { poiRow ⇒
            val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY)

            asInt(Cell.from(poiCellWithCurrency)) should be(None)
          }
        }
      }
    }
  }

object CellTest:
  private val TEST_SPREADSHEET = "Cell.xlsx"
  private val CELL_WORKSHEET = "CellWorksheet"

  private val STRING = "STRING"
  private val INDEX_OF_CELL_WITH_BLANK = 0
  private val BLANK_VALUE = ""
  private val INDEX_OF_CELL_WITH_SEPARATOR = 1
  private val SEPARATOR_VALUE = ""
  private val BACKGROUND_COLOR = "231,230,230"
  private val INDEX_OF_CELL_WITH_STRING = 2
  private val STRING_VALUE = "String"
  private val ADDRESS_OF_CELL_WITH_STRING = "C1"
  private val INDEX_OF_CELL_WITH_STRING_FORMULA = 3
  private val STRING_FORMULA = "_xlfn.CONCAT(C1,C1)"
  private val STRING_FORMULA_VALUE = "StringString"
  private val INTEGER = "INTEGER"
  private val INDEX_OF_CELL_WITH_INTEGER = 4
  private val INTEGER_VALUE = "1"
  private val INDEX_OF_CELL_WITH_INTEGER_FORMULA = 5
  private val INTEGER_FORMULA_VALUE = "2"
  private val INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING = 6
  private val INTEGER_SHAPED_STRING_VALUE = "1"
  private val INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA = 7
  private val INTEGER_SHAPED_STRING_FORMULA_VALUE = "11"
  private val INDEX_OF_CELL_WITH_DOUBLE = 8
  private val DOUBLE_VALUE = "1.1"
  private val INDEX_OF_CELL_WITH_DOUBLE_FORMULA = 9
  private val DOUBLE_FORMULA_VALUE = "2.2"
  private val INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING = 10
  private val INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA = 11
  private val INDEX_OF_CELL_WITH_DATE = 12
  private val DATE_VALUE = "05/11/2008"
  private val MASK = "m/d/yy"
  private val INDEX_OF_CELL_WITH_CURRENCY = 13
  private val CURRENCY_VALUE = "1,00"
  private val INDEX_OF_CELL_WITH_NOTE = 14
  private val NOTE = "Note"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_RED = 15
  private val FONT_COLOR_RED = "255,0,0"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_AUTOMATIC = 16
  private val FONT_COLOR_AUTOMATIC = "0,0,0"

  private def valueOf(cell: Try[Cell]): String = cell.success.value.value

  private def addressOf(cell: Try[Cell]): String = cell.success.value.address

  private def typeOf(cell: Try[Cell]): String = cell.success.value.`type`

  private def maskOf(cell: Try[Cell]): String = cell.success.value.mask

  private def formulaOf(cell: Try[Cell]): String = cell.success.value.formula

  private def noteOf(cell: Try[Cell]): String = cell.success.value.note

  private def fontColorOf(cell: Try[Cell]): String = cell.success.value.fontColor

  private def backgroundColorOf(cell: Try[Cell]): String = cell.success.value.backgroundColor

  private def asInt(cell: Try[Cell]): Option[Int] = cell.success.value.asInt