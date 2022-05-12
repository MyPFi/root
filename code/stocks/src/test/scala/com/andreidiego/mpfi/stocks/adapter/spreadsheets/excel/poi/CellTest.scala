package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Outcome
import org.scalatest.TryValues.*

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        "a date, in all its formats:" - {
          "14." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_14)

            valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
          }
          "15." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_15)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_15)

            valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
          }
          "16." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_16)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_16)

            valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
          }
          "17." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_17)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_17)

            valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
          }
          "22." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_22)
            assert(poiCell.getCellStyle.getDataFormat == DATE_FORMAT_ID_22)

            valueOf(Cell.from(poiCell)) should be(DATE_VALUE)
          }
        }
        "a currency, in all its formats:" - {
          "5." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
          "6." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_6)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
          "7." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_7)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
          "8." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_8)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
          "42." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_42)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
          "44." in { poiRow =>
            val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_44)

            valueOf(Cell.from(poiCell)) should be(CURRENCY_VALUE)
          }
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
      "be able to tell when it's" - {
        "a currency." in { poiRow ⇒
          val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

          assert(Cell.from(poiCellWithCurrency).success.value.isCurrency)
        }
        "not a currency." in { poiRow ⇒
          val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

          assert(Cell.from(poiCellWithDouble).success.value.isNotCurrency)
        }
        "a date." in { poiRow ⇒
          val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

          assert(Cell.from(poiCellWithDate).success.value.isDate)
        }
        "not a date." in { poiRow ⇒
          val poiCellWithNoDate = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

          assert(Cell.from(poiCellWithNoDate).success.value.isNotDate)
        }
      }
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
          "'DOUBLE', when its underlying Excel value is" - {
            "a double" - {
              "itself." in { poiRow ⇒
                val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

                typeOf(Cell.from(poiCellWithDouble)) should be(DOUBLE)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

                typeOf(Cell.from(poiCellWithDoubleFormula)) should be(DOUBLE)
              }
            }
            "a double-shaped string" - {
              "itself, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                  typeOf(Cell.from(poiCellWithDoubleShapedString)) should be(DOUBLE)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                  typeOf(Cell.from(poiCellWithDoubleShapedStringWithComma)) should be(DOUBLE)
                }
              }
              "resulting from a formula, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                  typeOf(Cell.from(poiCellWithDoubleShapedStringFormula)) should be(DOUBLE)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                  typeOf(Cell.from(poiCellWithDoubleShapedStringWithCommaFormula)) should be(DOUBLE)
                }
              }
            }
          }
          "'CURRENCY'" - {
            "for the following underlying Excel format ids:" - {
              "5." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
              "6." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_6)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
              "7." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_7)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
              "8." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_8)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
              "42." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_42)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
              "44." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_44)

                typeOf(Cell.from(poiCell)) should be(CURRENCY)
              }
            }
            "when its underlying Excel value is a currency-shaped string" - {
              "itself, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                  typeOf(Cell.from(poiCellWithCurrencyShapedString)) should be(CURRENCY)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                  typeOf(Cell.from(poiCellWithCurrencyShapedStringWithComma)) should be(CURRENCY)
                }
              }
              "resulting from a formula, whether represented with a" - {
                "dot." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                  typeOf(Cell.from(poiCellWithCurrencyShapedStringFormula)) should be(CURRENCY)
                }
                "comma." in { poiRow ⇒
                  val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                  typeOf(Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula)) should be(CURRENCY)
                }
              }
            }
          }
          "'DATE'" - {
            "for the following underlying Excel format ids:" - {
              "14." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

                typeOf(Cell.from(poiCell)) should be(DATE)
              }
              "15." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_15)

                typeOf(Cell.from(poiCell)) should be(DATE)
              }
              "16." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_16)

                typeOf(Cell.from(poiCell)) should be(DATE)
              }
              "17." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_17)

                typeOf(Cell.from(poiCell)) should be(DATE)
              }
              "22." in { poiRow =>
                val poiCell = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_22)

                typeOf(Cell.from(poiCell)) should be(DATE)
              }
            }
            "when its underlying Excel value is a date-shaped string" - {
              "itself." in { poiRow ⇒
                val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

                typeOf(Cell.from(poiCellWithDateShapedString)) should be(DATE)
              }
              "resulting from a formula." in { poiRow ⇒
                val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

                typeOf(Cell.from(poiCellWithDateShapedStringFormula)) should be(DATE)
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
          val poiCellWithMask = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)
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
        "'Double', when its underlying Excel value is" - {
          "a double" - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              asDouble(Cell.from(poiCellWithDouble)) should be(Some(DOUBLE_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              asDouble(Cell.from(poiCellWithDoubleFormula)) should be(Some(DOUBLE_FORMULA_VALUE.toDouble))
            }
          }
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                asDouble(Cell.from(poiCellWithDoubleShapedString)) should be(Some(DOUBLE_SHAPED_STRING_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                asDouble(Cell.from(poiCellWithDoubleShapedStringWithComma)) should be(Some(DOUBLE_SHAPED_STRING_WITH_COMMA_VALUE.toDouble))
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                asDouble(Cell.from(poiCellWithDoubleShapedStringFormula)) should be(Some(DOUBLE_SHAPED_STRING_FORMULA_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                asDouble(Cell.from(poiCellWithDoubleShapedStringWithCommaFormula)) should be(Some(DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE.toDouble))
              }
            }
          }
          "an integer" - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              asDouble(Cell.from(poiCellWithInteger)) should be(Some(INTEGER_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              asDouble(Cell.from(poiCellWithIntegerFormula)) should be(Some(INTEGER_FORMULA_VALUE.toDouble))
            }
          }
          "an integer-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              asDouble(Cell.from(poiCellWithIntegerShapedString)) should be(Some(INTEGER_SHAPED_STRING_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              asDouble(Cell.from(poiCellWithIntegerShapedStringFormula)) should be(Some(INTEGER_SHAPED_STRING_FORMULA_VALUE.toDouble))
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              asDouble(Cell.from(poiCellWithCurrency)) should be(Some(CURRENCY_VALUE.toDouble))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              asDouble(Cell.from(poiCellWithCurrencyFormula)) should be(Some(CURRENCY_FORMULA_VALUE.toDouble))
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                asDouble(Cell.from(poiCellWithCurrencyShapedString)) should be(Some(CURRENCY_SHAPED_STRING_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                asDouble(Cell.from(poiCellWithCurrencyShapedStringWithComma)) should be(Some(CURRENCY_SHAPED_STRING_WITH_COMMA_VALUE.toDouble))
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                asDouble(Cell.from(poiCellWithCurrencyShapedStringFormula)) should be(Some(CURRENCY_SHAPED_STRING_FORMULA_VALUE.toDouble))
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                asDouble(Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula)) should be(Some(CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA_VALUE.toDouble))
              }
            }
          }
        }
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
        "'LocalDate', when its underlying Excel value is" - {
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              asLocalDate(Cell.from(poiCellWithDate)) should be(Some(DATE_VALUE.toLocalDate))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              asLocalDate(Cell.from(poiCellWithDateFormula)) should be(Some(DATE_FORMULA_VALUE.toLocalDate))
            }
          }
          "a date-shaped string" - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              asLocalDate(Cell.from(poiCellWithDateShapedString)) should be(Some(DATE_SHAPED_STRING_VALUE.toLocalDate))
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              asLocalDate(Cell.from(poiCellWithDateShapedStringFormula)) should be(Some(DATE_SHAPED_STRING_FORMULA_VALUE.toLocalDate))
            }
          }
        }
      }
      "not be converted to" - {
        "'Double', for instance, when its underlying Excel value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            asDouble(Cell.from(emptyPOICell)) should be(None)
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              asDouble(Cell.from(poiCellWithString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              asDouble(Cell.from(poiCellWithStringFormula)) should be(None)
            }
          }
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              asDouble(Cell.from(poiCellWithDate)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              asDouble(Cell.from(poiCellWithDateFormula)) should be(None)
            }
          }
          "a date-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              asDouble(Cell.from(poiCellWithDateShapedString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              asDouble(Cell.from(poiCellWithDateShapedStringFormula)) should be(None)
            }
          }
        }
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
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                asInt(Cell.from(poiCellWithDoubleShapedString)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                asInt(Cell.from(poiCellWithDoubleShapedStringWithComma)) should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                asInt(Cell.from(poiCellWithDoubleShapedStringFormula)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                asInt(Cell.from(poiCellWithDoubleShapedStringWithCommaFormula)) should be(None)
              }
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
          "a date" - {
            "itself." in { poiRow ⇒
              val poiCellWithDate = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMAT_ID_14)

              asInt(Cell.from(poiCellWithDate)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_FORMULA)

              asInt(Cell.from(poiCellWithDateFormula)) should be(None)
            }
          }
          "a date-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithDateShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING)

              asInt(Cell.from(poiCellWithDateShapedString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDateShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DATE_SHAPED_STRING_FORMULA)

              asInt(Cell.from(poiCellWithDateShapedStringFormula)) should be(None)
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              asInt(Cell.from(poiCellWithCurrency)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              asInt(Cell.from(poiCellWithCurrencyFormula)) should be(None)
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                asInt(Cell.from(poiCellWithCurrencyShapedString)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                asInt(Cell.from(poiCellWithCurrencyShapedStringWithComma)) should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                asInt(Cell.from(poiCellWithCurrencyShapedStringFormula)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                asInt(Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula)) should be(None)
              }
            }
          }
        }
        "'LocalDate', for instance, when its value is" - {
          "empty." in { poiRow ⇒
            val emptyPOICell = poiRow.getCell(INDEX_OF_CELL_WITH_BLANK)

            asLocalDate(Cell.from(emptyPOICell)) should be(None)
          }
          "a double." - {
            "itself." in { poiRow ⇒
              val poiCellWithDouble = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE)

              asLocalDate(Cell.from(poiCellWithDouble)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithDoubleFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_FORMULA)

              asLocalDate(Cell.from(poiCellWithDoubleFormula)) should be(None)
            }
          }
          "a double-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING)

                asLocalDate(Cell.from(poiCellWithDoubleShapedString)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA)

                asLocalDate(Cell.from(poiCellWithDoubleShapedStringWithComma)) should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithDoubleShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_FORMULA)

                asLocalDate(Cell.from(poiCellWithDoubleShapedStringFormula)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithDoubleShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_DOUBLE_SHAPED_STRING_WITH_COMMA_FORMULA)

                asLocalDate(Cell.from(poiCellWithDoubleShapedStringWithCommaFormula)) should be(None)
              }
            }
          }
          "an alphanumeric string." - {
            "itself." in { poiRow ⇒
              val poiCellWithString = poiRow.getCell(INDEX_OF_CELL_WITH_STRING)

              asLocalDate(Cell.from(poiCellWithString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_STRING_FORMULA)

              asLocalDate(Cell.from(poiCellWithStringFormula)) should be(None)
            }
          }
          "a integer." - {
            "itself." in { poiRow ⇒
              val poiCellWithInteger = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER)

              asLocalDate(Cell.from(poiCellWithInteger)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_FORMULA)

              asLocalDate(Cell.from(poiCellWithIntegerFormula)) should be(None)
            }
          }
          "a integer-shaped string." - {
            "itself." in { poiRow ⇒
              val poiCellWithIntegerShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING)

              asLocalDate(Cell.from(poiCellWithIntegerShapedString)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithIntegerShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_INTEGER_SHAPED_STRING_FORMULA)

              asLocalDate(Cell.from(poiCellWithIntegerShapedStringFormula)) should be(None)
            }
          }
          "a currency" - {
            "itself." in { poiRow ⇒
              val poiCellWithCurrency = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMAT_ID_5)

              asLocalDate(Cell.from(poiCellWithCurrency)) should be(None)
            }
            "resulting from a formula." in { poiRow ⇒
              val poiCellWithCurrencyFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_FORMULA)

              asLocalDate(Cell.from(poiCellWithCurrencyFormula)) should be(None)
            }
          }
          "a currency-shaped string" - {
            "itself, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedString = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING)

                asLocalDate(Cell.from(poiCellWithCurrencyShapedString)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithComma = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA)

                asLocalDate(Cell.from(poiCellWithCurrencyShapedStringWithComma)) should be(None)
              }
            }
            "resulting from a formula, whether represented with a" - {
              "dot." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_FORMULA)

                asLocalDate(Cell.from(poiCellWithCurrencyShapedStringFormula)) should be(None)
              }
              "comma." in { poiRow ⇒
                val poiCellWithCurrencyShapedStringWithCommaFormula = poiRow.getCell(INDEX_OF_CELL_WITH_CURRENCY_SHAPED_STRING_WITH_COMMA_FORMULA)

                asLocalDate(Cell.from(poiCellWithCurrencyShapedStringWithCommaFormula)) should be(None)
              }
            }
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
  private val DOUBLE = "DOUBLE"
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
  private val CURRENCY = "CURRENCY"
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
  private val DATE = "DATE"
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
  private val INDEX_OF_CELL_WITH_NOTE = 33
  private val NOTE = "Note"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_RED = 34
  private val FONT_COLOR_RED = "255,0,0"
  private val INDEX_OF_CELL_WITH_FONT_COLOR_AUTOMATIC = 35
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

  private def asDouble(cell: Try[Cell]): Option[Double] = cell.success.value.asDouble

  private def asLocalDate(cell: Try[Cell]): Option[LocalDate] = cell.success.value.asLocalDate

  extension (string: String)
    private def toLocalDate: LocalDate = LocalDate.parse(string, DateTimeFormatter.ofPattern("dd/MM/yyyy"))