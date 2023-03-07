package com.andreidiego.mpfi.stocks.adapter.files.readers.spreadsheets.excel.poi

import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.EitherValues.*
import org.scalatest.BeforeAndAfterAll

class CellTest extends FixtureAnyFreeSpec, BeforeAndAfterAll:

  import java.io.File
  import org.apache.poi.openxml4j.opc.OPCPackage
  import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook, XSSFWorkbookFactory}
  import scala.language.deprecated.symbolLiterals
  import org.scalatest.Outcome
  import org.scalatest.matchers.should.Matchers.*
  import CellType.{valueOf as _, *}
  import Cell.CellError.IllegalArgument
  import CellTest.{*, given}

  override protected type FixtureParam = XSSFRow

  private var testWorkbook: XSSFWorkbook = _
  
  override protected def beforeAll(): Unit = 
    testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest(testWorkbook.getSheet(CELL_WORKSHEET).getRow(0)))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  "A Cell" - {
    "should" - {
      "be built from a POI Cell" in { _ ⇒
        val poiCell = new XSSFWorkbook().createSheet("1").createRow(0).createCell(0)

        "Cell.from(poiCell)" should compile
      }
      "be successfully built when given a POI Cell that contains" - {
        "a string." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          Cell.from(poiCell).value should be(STRING_VALUE)
        }
        "an integer." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

          Cell.from(poiCell).value should be(INTEGER_VALUE)
        }
        "a double." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

          Cell.from(poiCell).value should be(DOUBLE_VALUE)
        }
        "a date, in all its formats:" - {
          "14." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_14)

            Cell.from(poiCell).value should be(DATE_VALUE)
          }
          "15." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_15)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_15)

            Cell.from(poiCell).value should be(DATE_VALUE)
          }
          "16." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_16)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_16)

            Cell.from(poiCell).value should be(DATE_VALUE)
          }
          "17." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_17)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_17)

            Cell.from(poiCell).value should be(DATE_VALUE)
          }
          "22." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_22)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_22)

            Cell.from(poiCell).value should be(DATE_VALUE)
          }
        }
        "a currency, in all its formats:" - {
          "5." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
          "6." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_6)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
          "7." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_7)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
          "8." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_8)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
          "42." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_42)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
          "44." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_44)

            Cell.from(poiCell).value should be(CURRENCY_VALUE)
          }
        }
        "blank." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

          Cell.from(poiCell).value should be(BLANK_VALUE)
        }
        "a separator." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

          Cell.from(poiCell).value should be(SEPARATOR_VALUE)
        }
        "a string formula." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

          Cell.from(poiCell).value should be(STRING_FORMULA_VALUE)
        }
        "a numeric formula that results in an integer." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

          Cell.from(poiCell).value should be(INTEGER_FORMULA_VALUE)
        }
        "a numeric formula that results in a double." in { poiRow =>
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

          Cell.from(poiCell).value should be(DOUBLE_FORMULA_VALUE)
        }
        "an error." in { poiRow =>
          val poiCellWithErrors = poiRow.getCell(INDEX_OF_CELL_WITH_ERROR)

          Cell.from(poiCellWithErrors).value should be(ERROR_VALUE)
        }
        "a negative date." in { poiRow ⇒
          val poiCellWithNegativeDate = poiRow.getCell(INDEX_OF_CELL_WITH_NEGATIVE_DATE)

          Cell.from(poiCellWithNegativeDate).value should be(NEGATIVE_DATE_VALUE)
        }
        "something that looks like a date but contains characters other than numbers and the '/' symbol." in { poiRow ⇒
          val poiCellWithDateWithExtraneousCharacters = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_WITH_EXTRANEOUS_CHARACTERS)

          Cell.from(poiCellWithDateWithExtraneousCharacters).value should be(DATE_WITH_EXTRANEOUS_CHARACTERS_VALUE)
        }
        "an invalid date." in { poiRow ⇒
          val poiCellWithInvalidDate = poiRow.getCell(INDEX_OF_CELL_WITH_INVALID_DATE)

          Cell.from(poiCellWithInvalidDate).value should be(INVALID_DATE_VALUE)
        }
      }
      "fail to be built when given" - {
        "string arguments instead of a POI Cell." in { _ =>
          """Cell("Address", "Value", "Type", "Mask", "Formula", "Note", "FontColor", "BackgroundColor")""" shouldNot compile
        }
        "a POI Cell that is 'null'." in { _ =>
          val error = Cell.from(null).error

          error should have(
            'class(classOf[IllegalArgument]),
            'message(s"Invalid cell found: null")
          )
        }
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
      "forbid manipulation of its internal" - {
        "address." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).address = "address"""" shouldNot compile
        }
        "value." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).value = "value"""" shouldNot compile
        }
        "type." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).`type` = CellType.INTEGER""" shouldNot compile
        }
        "mask." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).mask = "mask"""" shouldNot compile
        }
        "formula." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).formula = "formula"""" shouldNot compile
        }
        "note." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).note = "note"""" shouldNot compile
        }
        "fontColor." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).fontColor = "fontColor"""" shouldNot compile
        }
        "backgroundColor." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          """Cell.from(poiCell).backgroundColor = "backgroundColor"""" shouldNot compile
        }
      }
      "be able to tell when it's" - {
        "empty." in { poiRow ⇒
          val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

          Cell.from(emptyPOICell).toEither.value shouldBe empty
        }
        "not empty." in { poiRow ⇒
          val nonEmptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          assert(Cell.from(nonEmptyPOICell).isNotEmpty)
        }
        "a currency." in { poiRow ⇒
          val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

          assert(Cell.from(poiCellWithCurrency).isCurrency)
        }
        "not a currency." in { poiRow ⇒
          val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

          assert(Cell.from(poiCellWithDouble).isNotCurrency)
        }
        "a date." in { poiRow ⇒
          val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

          assert(Cell.from(poiCellWithDate).isDate)
        }
        "not a date." in { poiRow ⇒
          val poiCellWithNoDate = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          assert(Cell.from(poiCellWithNoDate).isNotDate)
        }
        "a formula." in { poiRow ⇒
          val poiCellWithFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

          assert(Cell.from(poiCellWithFormula).isFormula)
        }
        "not a formula." in { poiRow ⇒
          val poiCellWithNoFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          assert(Cell.from(poiCellWithNoFormula).isNotFormula)
        }
      }
      "always have" - {
        "an address." in { poiRow ⇒
          val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          Cell.from(poiCell).address should be(ADDRESS_OF_CELL_WITH_STRING)
        }
        "type" - {
          "'STRING', when its underlying Excel value is" - {
            "empty, because it's" - {
              "an empty cell." in { poiRow ⇒
                val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

                Cell.from(emptyPOICell).`type` should be(STRING)
              }
              "a separator." in { poiRow ⇒
                val separatorPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

                Cell.from(separatorPOICell).`type` should be(STRING)
              }
            }
            "an alphanumeric string" - {
              "itself." in { poiRow ⇒
                val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

                Cell.from(poiCellWithString).`type` should be(STRING)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

                Cell.from(poiCellWithStringFormula).`type` should be(STRING)
              }
            }
            "something that looks like a date but contains characters other than numbers and the '/' symbol." in { poiRow ⇒
              val poiCellWithDateWithExtraneousCharacters = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_WITH_EXTRANEOUS_CHARACTERS)

              Cell.from(poiCellWithDateWithExtraneousCharacters).`type` should be(STRING)
            }
            "an invalid date." in { poiRow ⇒
              val poiCellWithInvalidDate = poiRow.getCell(INDEX_OF_CELL_WITH_INVALID_DATE)

              Cell.from(poiCellWithInvalidDate).`type` should be(STRING)
            }
          }
          "'INTEGER', when its underlying Excel value is" - {
            "an integer" - {
              "itself." in { poiRow ⇒
                val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

                Cell.from(poiCellWithInteger).`type` should be(INTEGER)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

                Cell.from(poiCellWithIntegerFormula).`type` should be(INTEGER)
              }
            }
            "an integer-shaped string" - {
              "itself." in { poiRow ⇒
                val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

                Cell.from(poiCellWithIntegerShapedString).`type` should be(INTEGER)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithIntegerShapedStringFormula).`type` should be(INTEGER)
              }
            }
            "a negative date." in { poiRow ⇒
              val poiCellWithNegativeDate = poiRow.getCell(INDEX_OF_CELL_WITH_NEGATIVE_DATE)

              Cell.from(poiCellWithNegativeDate).`type` should be(INTEGER)
            }
          }
          "'DOUBLE', when its underlying Excel value is" - {
            "a double" - {
              "itself." in { poiRow ⇒
                val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

                Cell.from(poiCellWithDouble).`type` should be(DOUBLE)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

                Cell.from(poiCellWithDoubleFormula).`type` should be(DOUBLE)
              }
            }
            "a double-shaped string" - {
              "itself, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                  Cell.from(poiCellWithDoubleShapedString).`type` should be(DOUBLE)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                  Cell.from(poiCellWithDoubleShapedStringWithComma).`type` should be(DOUBLE)
                }
              }
              "resulting from a formula, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                  Cell.from(poiCellWithDoubleShapedStringFormula).`type` should be(DOUBLE)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                  Cell.from(poiCellWithDoubleShapedStringWithCommaFormula).`type` should be(DOUBLE)
                }
              }
            }
          }
          "'CURRENCY'" - {
            "for the following underlying Excel format ids:" - {
              "5." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
              "6." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_6)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
              "7." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_7)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
              "8." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_8)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
              "42." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_42)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
              "44." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_44)

                Cell.from(poiCell).`type` should be(CURRENCY)
              }
            }
            "when its underlying Excel value is a currency-shaped string" - {
              "itself, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                  Cell.from(poiCellWithCurrencyShapedString).`type` should be(CURRENCY)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                  Cell.from(poiCellWithCurrencyShapedStringWithComma).`type` should be(CURRENCY)
                }
              }
              "resulting from a formula, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                  Cell.from(poiCellWithCurrencyShapedStringFormula).`type` should be(CURRENCY)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                  Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula).`type` should be(CURRENCY)
                }
              }
            }
          }
          "'DATE'" - {
            "for the following underlying Excel format ids:" - {
              "14." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

                Cell.from(poiCell).`type` should be(DATE)
              }
              "15." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_15)

                Cell.from(poiCell).`type` should be(DATE)
              }
              "16." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_16)

                Cell.from(poiCell).`type` should be(DATE)
              }
              "17." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_17)

                Cell.from(poiCell).`type` should be(DATE)
              }
              "22." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_22)

                Cell.from(poiCell).`type` should be(DATE)
              }
            }
            "when its underlying Excel value is a date-shaped string" - {
              "itself." in { poiRow ⇒
                val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

                Cell.from(poiCellWithDateShapedString).`type` should be(DATE)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithDateShapedStringFormula).`type` should be(DATE)
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

          val value = Cell.from(poiCellWithValue).value
          val noValue = Cell.from(poiCellWithNoValue).value

          value should be(STRING_VALUE)
          noValue should be(empty)
        }
        "a mask." in { poiRow ⇒
          val poiCellWithMask = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)
          val poiCellWithNoMask = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val mask = Cell.from(poiCellWithMask).mask
          val noMask = Cell.from(poiCellWithNoMask).mask

          mask should be(MASK)
          noMask should be(empty)
        }
        "a formula." in { poiRow ⇒
          val poiCellWithFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)
          val poiCellWithNoFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val formula = Cell.from(poiCellWithFormula).formula
          val noFormula = Cell.from(poiCellWithNoFormula).formula

          formula should be(STRING_FORMULA)
          noFormula should be(empty)
        }
        "a note." in { poiRow ⇒
          val poiCellWithNote = poiRow.getCell(INDEX_OF_CELL_WITH_NOTE)
          val poiCellWithNoNote = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          val note = Cell.from(poiCellWithNote).note
          val noNote = Cell.from(poiCellWithNoNote).note

          note should be(NOTE)
          noNote should be(empty)
        }
        "color of" - {
          "font." in { poiRow ⇒
            val poiCellWithFontColorRed = poiRow.getCell(INDEX_OF_CELL_WITH_FONT_COLOR_RED)
            val poiCellWithFontColorAutomatic = poiRow.getCell(INDEX_OF_CELL_WITH_FONT_COLOR_AUTOMATIC)
            val poiCellWithNoFontColor = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)

            val fontColor1 = Cell.from(poiCellWithFontColorRed).fontColor
            val fontColor2 = Cell.from(poiCellWithFontColorAutomatic).fontColor
            val noFontColor = Cell.from(poiCellWithNoFontColor).fontColor

            fontColor1 should be(FONT_COLOR_RED)
            fontColor2 should be(FONT_COLOR_AUTOMATIC)
            noFontColor should be(empty)
          }
          "background." in { poiRow ⇒
            val poiCellWithBackgroundColor = poiRow.getCell(INDEX_OF_CELL_WITH_SEPARATOR)
            val poiCellWithNoBackgroundColor = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

            val backgroundColor = Cell.from(poiCellWithBackgroundColor).backgroundColor
            val noBackgroundColor = Cell.from(poiCellWithNoBackgroundColor).backgroundColor

            backgroundColor should be(BACKGROUND_COLOR)
            noBackgroundColor should be(empty)
          }
        }
      }
      "be converted to" - {
        "'Double', when its underlying Excel value is" - {
          "a double" - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              Cell.from(poiCellWithDouble).asDouble should be(Some(DOUBLE_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              Cell.from(poiCellWithDoubleFormula).asDouble should be(Some(DOUBLE_FORMULA_VALUE.toDouble))
            }
          }
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                Cell.from(poiCellWithDoubleShapedString).asDouble should be(Some(DOUBLE_SHAPED_STRING_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithDoubleShapedStringWithComma).asDouble should be(Some(DOUBLE_SHAPED_STRING_WITH_COMMA_VALUE.toDouble))
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringFormula).asDouble should be(Some(DOUBLE_SHAPED_STRING_FORMULA_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringWithCommaFormula).asDouble should be(Some(DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE.toDouble))
              }
            }
          }
          "an integer" - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              Cell.from(poiCellWithInteger).asDouble should be(Some(INTEGER_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              Cell.from(poiCellWithIntegerFormula).asDouble should be(Some(INTEGER_FORMULA_VALUE.toDouble))
            }
          }
          "an integer-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              Cell.from(poiCellWithIntegerShapedString).asDouble should be(Some(INTEGER_SHAPED_STRING_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithIntegerShapedStringFormula).asDouble should be(Some(INTEGER_SHAPED_STRING_FORMULA_VALUE.toDouble))
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              Cell.from(poiCellWithCurrency).asDouble should be(Some(CURRENCY_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              Cell.from(poiCellWithCurrencyFormula).asDouble should be(Some(CURRENCY_FORMULA_VALUE.toDouble))
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                Cell.from(poiCellWithCurrencyShapedString).asDouble should be(Some(CURRENCY_SHAPED_STRING_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithCurrencyShapedStringWithComma).asDouble should be(Some(CURRENCY_SHAPED_STRING_WITH_COMMA_VALUE.toDouble))
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringFormula).asDouble should be(Some(CURRENCY_SHAPED_STRING_FORMULA_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula).asDouble should be(Some(CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE.toDouble))
              }
            }
          }
        }
        "'Int', when its underlying Excel value is" - {
          "an integer" - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              Cell.from(poiCellWithInteger).asInt should be(Some(INTEGER_VALUE.toInt))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              Cell.from(poiCellWithIntegerFormula).asInt should be(Some(INTEGER_FORMULA_VALUE.toInt))
            }
          }
          "an integer-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              Cell.from(poiCellWithIntegerShapedString).asInt should be(Some(INTEGER_SHAPED_STRING_VALUE.toInt))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithIntegerShapedStringFormula).asInt should be(Some(INTEGER_SHAPED_STRING_FORMULA_VALUE.toInt))
            }
          }
        }
        "'LocalDate', when its underlying Excel value is" - {
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              Cell.from(poiCellWithDate).asLocalDate should be(Some(DATE_VALUE.toLocalDate))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              Cell.from(poiCellWithDateFormula).asLocalDate should be(Some(DATE_FORMULA_VALUE.toLocalDate))
            }
          }
          "a date-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              Cell.from(poiCellWithDateShapedString).asLocalDate should be(Some(DATE_SHAPED_STRING_VALUE.toLocalDate))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithDateShapedStringFormula).asLocalDate should be(Some(DATE_SHAPED_STRING_FORMULA_VALUE.toLocalDate))
            }
          }
        }
      }
      "not be converted to" - {
        "'Double', for instance, when its underlying Excel value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            Cell.from(emptyPOICell).asDouble should be(None)
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              Cell.from(poiCellWithString).asDouble should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              Cell.from(poiCellWithStringFormula).asDouble should be(None)
            }
          }
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              Cell.from(poiCellWithDate).asDouble should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              Cell.from(poiCellWithDateFormula).asDouble should be(None)
            }
          }
          "a date-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              Cell.from(poiCellWithDateShapedString).asDouble should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithDateShapedStringFormula).asDouble should be(None)
            }
          }
        }
        "'Int', for instance, when its underlying Excel value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            Cell.from(emptyPOICell).asInt should be(None)
          }
          "a double." - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              Cell.from(poiCellWithDouble).asInt should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              Cell.from(poiCellWithDoubleFormula).asInt should be(None)
            }
          }
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                Cell.from(poiCellWithDoubleShapedString).asInt should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithDoubleShapedStringWithComma).asInt should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringFormula).asInt should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringWithCommaFormula).asInt should be(None)
              }
            }
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              Cell.from(poiCellWithString).asInt should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              Cell.from(poiCellWithStringFormula).asInt should be(None)
            }
          }
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              Cell.from(poiCellWithDate).asInt should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              Cell.from(poiCellWithDateFormula).asInt should be(None)
            }
          }
          "a date-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              Cell.from(poiCellWithDateShapedString).asInt should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithDateShapedStringFormula).asInt should be(None)
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              Cell.from(poiCellWithCurrency).asInt should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              Cell.from(poiCellWithCurrencyFormula).asInt should be(None)
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                Cell.from(poiCellWithCurrencyShapedString).asInt should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithCurrencyShapedStringWithComma).asInt should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringFormula).asInt should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula).asInt should be(None)
              }
            }
          }
        }
        "'LocalDate', for instance, when its value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            Cell.from(emptyPOICell).asLocalDate should be(None)
          }
          "a double." - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              Cell.from(poiCellWithDouble).asLocalDate should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              Cell.from(poiCellWithDoubleFormula).asLocalDate should be(None)
            }
          }
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                Cell.from(poiCellWithDoubleShapedString).asLocalDate should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithDoubleShapedStringWithComma).asLocalDate should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringFormula).asLocalDate should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithDoubleShapedStringWithCommaFormula).asLocalDate should be(None)
              }
            }
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              Cell.from(poiCellWithString).asLocalDate should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              Cell.from(poiCellWithStringFormula).asLocalDate should be(None)
            }
          }
          "a integer." - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              Cell.from(poiCellWithInteger).asLocalDate should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              Cell.from(poiCellWithIntegerFormula).asLocalDate should be(None)
            }
          }
          "a integer-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              Cell.from(poiCellWithIntegerShapedString).asLocalDate should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              Cell.from(poiCellWithIntegerShapedStringFormula).asLocalDate should be(None)
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              Cell.from(poiCellWithCurrency).asLocalDate should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              Cell.from(poiCellWithCurrencyFormula).asLocalDate should be(None)
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                Cell.from(poiCellWithCurrencyShapedString).asLocalDate should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                Cell.from(poiCellWithCurrencyShapedStringWithComma).asLocalDate should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringFormula).asLocalDate should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula).asLocalDate should be(None)
              }
            }
          }
        }
      }
    }
  }

object CellTest:

  import java.time.LocalDate
  import java.time.format.DateTimeFormatter
  import org.apache.poi.ss.usermodel.FormulaError.NAME
  import Cell.ErrorsOr

  private val TEST_SPREADSHEET = "Cell.xlsx"
  private val CELL_WORKSHEET = "CellWorksheet"

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
  private val DOUBLE_SHAPED_STRING_VALUE = "1.1"
  private val INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA = 11
  private val DOUBLE_SHAPED_STRING_FORMULA_VALUE = "1.1"
  private val INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA = 12
  private val DOUBLE_SHAPED_STRING_WITH_COMMA_VALUE = "1.1"
  private val INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA = 13
  private val DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE = "1.1"
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5 = 14
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_6 = 15
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_7 = 16
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_8 = 17
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_42 = 18
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_44 = 19
  private val CURRENCY_VALUE = "1.0"
  private val INDEX_OF_CELL_WITH_CURRENCY_FORMULA = 20
  private val CURRENCY_FORMULA_VALUE = "2.0"
  private val INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING = 21
  private val CURRENCY_SHAPED_STRING_VALUE = "1.0"
  private val INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA = 22
  private val CURRENCY_SHAPED_STRING_FORMULA_VALUE = "1.0"
  private val INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA = 23
  private val CURRENCY_SHAPED_STRING_WITH_COMMA_VALUE = "1.0"
  private val INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA = 24
  private val CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE = "1.0"
  private val INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14 = 25
  private val DATE_FORMAT_ID_14 = 14
  private val INDEX_OF_CELL_WITH_DATE_FORMAT_ID_15 = 26
  private val DATE_FORMAT_ID_15 = 15
  private val INDEX_OF_CELL_WITH_DATE_FORMAT_ID_16 = 27
  private val DATE_FORMAT_ID_16 = 16
  private val INDEX_OF_CELL_WITH_DATE_FORMAT_ID_17 = 28
  private val DATE_FORMAT_ID_17 = 17
  private val INDEX_OF_CELL_WITH_DATE_FORMAT_ID_22 = 29
  private val DATE_FORMAT_ID_22 = 22
  private val DATE_VALUE = "05/11/2008"
  private val MASK = "m/d/yy"
  private val INDEX_OF_CELL_WITH_DATE_FORMULA = 30
  private val DATE_FORMULA_VALUE = "06/11/2008"
  private val INDEX_OF_CELL_WITH_DATE_SHAPED_STRING = 31
  private val DATE_SHAPED_STRING_VALUE = "05/11/2008"
  private val INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA = 32
  private val DATE_SHAPED_STRING_FORMULA_VALUE = "05/11/2008"
  private val INDEX_OF_CELL_WITH_NEGATIVE_DATE = 33
  private val NEGATIVE_DATE_VALUE = "-39757"
  private val INDEX_OF_CELL_WITH_DATE_WITH_EXTRANEOUS_CHARACTERS = 34
  private val DATE_WITH_EXTRANEOUS_CHARACTERS_VALUE = "O5/11/2008"
  private val INDEX_OF_CELL_WITH_INVALID_DATE = 35
  private val INVALID_DATE_VALUE = "05/13/2008"
  private val INDEX_OF_CELL_WITH_NOTE = 36
  private val NOTE = "Note"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_RED = 37
  private val FONT_COLOR_RED = "255,0,0"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_AUTOMATIC = 38
  private val FONT_COLOR_AUTOMATIC = "0,0,0"
  private val INDEX_OF_CELL_WITH_ERROR = 39
  private val ERROR_VALUE = NAME.getString

  given Conversion[ErrorsOr[Cell], Cell] = _.toEither.value

  extension (errorsOrCell: ErrorsOr[Cell])
    private def error: Cell.Error = errorsOrCell.toEither.left.value.head

  extension (string: String)
    private def toLocalDate: LocalDate = LocalDate.parse(string, DateTimeFormatter.ofPattern("dd/MM/yyyy"))