package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.Outcome
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.TryValues.*
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.Inspectors.forAll

import java.io.File
import scala.language.deprecated.symbolLiterals
import scala.util.Try

// TODO Replace Try + exceptions with Either
class WorksheetTest extends FixtureAnyFreeSpec :

  import WorksheetTest.*

  override protected type FixtureParam = XSSFWorkbook

  override protected def withFixture(test: OneArgTest): Outcome =
    val testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

    try withFixture(test.toNoArgTest(testWorkbook))
    finally testWorkbook.close()

  "A Worksheet should" - {
    "be built from a POI Worksheet" in { poiWorkbook ⇒
      val poiWorksheet = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

      "Worksheet.from(poiWorksheet)" should compile
    }
    "be successfully built when" - {
      "given a POI Worksheet" - {
        "whose header contains only" - {
          "non-empty cells." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

            headerFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
          }
          "non-blank (non-empty and separators) cells. (although some exceptions - described below - apply for separators)" in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("HeaderNonEmptyCellsAndSeparator")

            headerFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "")
          }
          "non-empty and string-formula cells." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("StringFormulaInHeader")

            headerFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq("Data Pregão", "Data PregãoPapel", "Nota", "Papel")
          }
        }
        "that contains" - {
          "one empty line between the header and its first non-empty regular line." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs VALID_TINY_WORKSHEET_CONTENTS
          }
          "no empty line between the header and its first non-empty regular line." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("NoEmptyLineAfterHeader")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, stringLine("2"))
          }
          "only the header and two non-empty regular lines." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("HeaderPlusTwoRegulars")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, stringLine("2"), stringLine("3"))
          }
          "only the header and a non-empty regular line." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("HeaderPlusOneRegular")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, stringLine("2"))
          }
          // TODO Two empty lines are only valid between the last group summary and the worksheet summary
          "two empty lines between its non-empty regular lines." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("TwoEmptiesBetweenRegulars")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, stringLine("2"), blankLine("3"), blankLine("4"), stringLine("5"))
          }
          "two empty lines are only valid between the last group summary and the worksheet summary" ignore { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("TwoEmptiesBetweenRegulars")

            headerFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq("Data Pregão", "Data PregãoPapel", "Nota", "Papel")
          }
          "one empty line between its non-empty regular lines." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("OneEmptyBetweenRegulars")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, stringLine("2"), blankLine("3"), stringLine("4"), stringLine("5"))
          }
          "no empty line between its non-empty regular lines." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs VALID_TINY_WORKSHEET_CONTENTS
          }
        }
        "whose lines contain" - {
          "strings." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs VALID_TINY_WORKSHEET_CONTENTS
          }
          "numbers." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithNumbers")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(
              HEADER,
              Seq(stringCell("A2"), integerCell("B2", "78174"), stringCell("C2"), integerCell("D2", "200"))
            )
          }
          "dates." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithDates")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER, STANDARD_LINE)
          }
          "currencies." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithCurrencies")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(HEADER_WITH_PRICE, STANDARD_LINE_WITH_PRICE)
          }
          "blanks." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithBlanks")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(
              HEADER_WITH_PRICE,
              Seq(dateCell("A2"), blankCell("B2"), stringCell("C2"), integerCell("D2", "200"), currencyCell("E2"))
            )
          }
          "string formulas." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithStringFormulas")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(
              HEADER_WITH_PRICE,
              Seq(dateCell("A2"), stringCell("B2"), ("C2", "VALE3VALE3", "STRING", "", "_xlfn.CONCAT(B2,B2)", "", "255,0,0", ""), integerCell("D2", "200"), currencyCell("E2"))
            )
          }
          "numeric formulas." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("LinesWithNumericFormulas")

            linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(
              HEADER_WITH_PRICE :+ ("F1", "Total", "STRING", "", "", "", "0,0,0", ""),
              STANDARD_LINE_WITH_PRICE :+ ("F2", "3068", "FORMULA", """"R$"\ #,##0.00;[Red]\-"R$"\ #,##0.00""", "D2*E2", "", "255,0,0", "")
            )
          }
        }
      }
    }
    "fail to be built when" - {
      "given a Header and a set of Lines directly instead of a POI Worksheet." in { poiWorkbook =>
        val poiHeaderRow = poiWorkbook.getSheet(VALID_TINY_WORKSHEET).getRow(0)

        """Worksheet(Header.from(poiHeaderRow).success.value, Seq())""" shouldNot compile
      }
      "given a POI Worksheet" - {
        "that" - {
          "is" - {
            "null." in { poiWorkbook =>
              val exception = Worksheet.from(null).failure.exception

              exception should have(
                'class(classOf[NullPointerException]),
                'message("""Cannot invoke "org.apache.poi.xssf.usermodel.XSSFSheet.getLastRowNum()" because "poiWorksheet" is null""")
              )
            }
            "empty." in { poiWorkbook =>
              val TEST_SHEET_NAME = "EmptySheet"
              val TEST_SHEET = poiWorkbook.getSheet(TEST_SHEET_NAME)

              val exception = Worksheet.from(TEST_SHEET).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"It looks like $TEST_SHEET_NAME is completely empty.")
              )
            }
          }
          "contains" - {
            "only the header." in { poiWorkbook =>
              val TEST_SHEET_NAME = "OnlyHeader"
              val TEST_SHEET = poiWorkbook.getSheet(TEST_SHEET_NAME)

              val exception = Worksheet.from(TEST_SHEET).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"$TEST_SHEET_NAME does not seem to have lines other than the header.")
              )
            }
            "more than one empty line between the header and its first non-empty regular line." in { poiWorkbook =>
              val TEST_SHEET_NAME = "IrregularEmptyLinesAtTheTop"
              val TEST_SHEET = poiWorkbook.getSheet(TEST_SHEET_NAME)

              val exception = Worksheet.from(TEST_SHEET).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Irregular empty line interval found right after the header of $TEST_SHEET_NAME. Only one empty line is allowed in this position.")
              )
            }
            "more than two empty lines between its non-empty regular lines." in { poiWorkbook =>
              val TEST_SHEET_NAME = "IrregularEmptyLineInterval"
              val TEST_SHEET = poiWorkbook.getSheet(TEST_SHEET_NAME)

              val exception = Worksheet.from(TEST_SHEET).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Irregular empty line interval (5:7) found between the regular lines of $TEST_SHEET_NAME. No more than two empty lines are allowed in this position.")
              )
            }
          }
        }
        "whose header" - {
          "is not in the first line." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("HeaderOutOfPlace")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalArgumentException]),
              'message("Header is empty.")
            )
          }
          "has only separator (empty but not blank) cells." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("OnlySeparatorsInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalArgumentException]),
              'message("Header is empty.")
            )
          }
          "has more than one contiguous separator." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("ContiguousSeparatorsInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalArgumentException]),
              'message("Multiple contiguous separators not allowed.")
            )
          }
          "'s first cell is a separator." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("SeparatorFirstInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalArgumentException]),
              'message("Separators not allowed at the beggining of the header.")
            )
          }
          "has a blank (empty but not a separator) cell." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("EmptyCellInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalArgumentException]),
              'message("An illegal blank cell was found in the header.")
            )
          }
          "has a numeric cell." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("NumberInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalStateException]),
              'message("Cannot get a STRING value from a NUMERIC cell")
            )
          }
          "has a boolean cell." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("BooleanInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalStateException]),
              'message("Cannot get a STRING value from a BOOLEAN cell")
            )
          }
          "has a date cell." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("DateInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalStateException]),
              'message("Cannot get a STRING value from a NUMERIC cell")
            )
          }
          "has a numeric formula cell." in { poiWorkbook =>
            val TEST_SHEET = poiWorkbook.getSheet("NumericFormulaInHeader")

            val exception = Worksheet.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"Worksheet does not seem to have a valid header.")
            )

            exception.getCause should have(
              'class(classOf[IllegalStateException]),
              'message("Cannot get a STRING value from a NUMERIC formula cell")
            )
          }
        }
      }
    }
    "filter out trailing empty lines." in { poiWorkbook =>
      val TEST_SHEET = poiWorkbook.getSheet("TrailingEmptyLines")

      linesFrom(Worksheet.from(TEST_SHEET)) should have size 10
    }
    "create group of lines containing those regular lines constrained between empty lines when there is" - {
      "no empty line right after the header." in { poiWorkbook =>
        val TEST_SHEET = poiWorkbook.getSheet("GroupsNoEmptyLineAfterHeader")

        groupsFrom(Worksheet.from(TEST_SHEET)) should have size 3
      }
      "an empty line right after the header." in { poiWorkbook =>
        val TEST_SHEET = poiWorkbook.getSheet("GroupsEmptyLineAfterHeader")

        groupsFrom(Worksheet.from(TEST_SHEET)) should have size 3
      }
    }
    "have at last one group." in { poiWorkbook =>
      val TEST_SHEET = poiWorkbook.getSheet("NoEmptyLineAfterHeader")

      groupsFrom(Worksheet.from(TEST_SHEET)) should have size 1
    }
    // TODO ???should the group in the worksheet below be allowed???
    "???should the group in the worksheet below be allowed???" ignore { poiWorkbook =>
      val TEST_SHEET = poiWorkbook.getSheet("BrokerageNotes.xlsx._BugInGroupFormation_")

      groupsFrom(Worksheet.from(TEST_SHEET)) should have size 1
    }
    "expose" - {
      "a name, which is the name of the underlying Excel worksheet." in { poiWorkbook =>
        val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

        nameFrom(Worksheet.from(TEST_SHEET)) should be(VALID_TINY_WORKSHEET)
      }
      "a header with cells sorted by their position in the worksheet." in { poiWorkbook =>
        val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

        headerFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
      }
      "a set of lines" - {
        "sorted by their position in the worksheet with cells within lines sorted in the same way." in { poiWorkbook =>
          val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

          linesFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs VALID_TINY_WORKSHEET_CONTENTS
        }
        "with no less cells than the header's size." in { poiWorkbook =>
          val TEST_SHEET = poiWorkbook.getSheet("LastTwoCellsEmpty")

          forAll(linesFrom(Worksheet.from(TEST_SHEET)).drop(2)) { line ⇒
            line should have size 4
            line.map(_._2) should contain theSameElementsInOrderAs Seq("05/11/2008", "78174", "", "")
          }
        }
      }
      "a set of groups sorted by their position in the worksheet with lines within groups and cells within lines sorted in the same way." in { poiWorkbook =>
        val TEST_SHEET = poiWorkbook.getSheet("GroupsNoEmptyLineAfterHeader")

        groupsFrom(Worksheet.from(TEST_SHEET)) should contain theSameElementsInOrderAs Seq(
          Seq(standardLine("2"), standardLine("3"), standardLine("4")),
          Seq(standardLine("6"), standardLine("7"), standardLine("8")),
          Seq(standardLine("10"))
        )
      }
    }
    "equal another Worksheet with the same configuration." in { poiWorkbook ⇒
      val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

      Worksheet.from(TEST_SHEET) should equal(Worksheet.from(TEST_SHEET))
    }
    "not equal another Worksheet with a different configuration." in { poiWorkbook ⇒
      val TEST_SHEET_1 = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)
      val TEST_SHEET_2 = poiWorkbook.getSheet("HeaderNonEmptyCellsAndSeparator")

      Worksheet.from(TEST_SHEET_1) should not equal Worksheet.from(TEST_SHEET_2)
    }
    "forbid manipulation of its internal header." in { poiWorkbook ⇒
      val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

      "val header: Header = Worksheet.from(TEST_SHEET).success.value.header" should compile
      "Worksheet.from(TEST_SHEET).success.value.header = header" shouldNot compile
    }
    "forbid manipulation of its internal lines." in { poiWorkbook ⇒
      val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

      "val lines: Seq[Line] = Worksheet.from(TEST_SHEET).success.value.lines" should compile
      "Worksheet.from(TEST_SHEET).success.value.lines = lines" shouldNot compile
    }
    "forbid manipulation of its internal groups." in { poiWorkbook ⇒
      val TEST_SHEET = poiWorkbook.getSheet(VALID_TINY_WORKSHEET)

      "val groups: Seq[Seq[Line]] = Worksheet.from(TEST_SHEET).success.value.groups" should compile
      "Worksheet.from(TEST_SHEET).success.value.groups = groups" shouldNot compile
    }
  }

object WorksheetTest:
  private type Cell = (String, String, String, String, String, String, String, String)
  private val TEST_SPREADSHEET = "Worksheet.xlsx"

  private val TRADING_DATE_HEADER = ("A1", "Data Pregão", "STRING", "", "", "", "0,0,0", "")
  private val BROKERAGE_NOTE_HEADER = ("B1", "Nota", "STRING", "", "", "", "0,0,0", "")
  private val TICKER_HEADER = ("C1", "Papel", "STRING", "", "", "", "0,0,0", "")
  private val QTY_HEADER = ("D1", "Qtde", "STRING", "", "", "", "0,0,0", "")
  private val PRICE_HEADER = ("E1", "Preço", "STRING", """"R$"\ #,##0.00;[Red]\-"R$"\ #,##0.00""", "", "", "0,0,0", "")
  private val HEADER = Seq(TRADING_DATE_HEADER, BROKERAGE_NOTE_HEADER, TICKER_HEADER, QTY_HEADER)
  private val HEADER_WITH_PRICE = HEADER :+ PRICE_HEADER

  private val STANDARD_LINE = standardLine("2")
  private val STANDARD_LINE_WITH_PRICE = STANDARD_LINE :+ currencyCell("E2")

  private val VALID_TINY_WORKSHEET = "ValidTinyWorksheet"
  private val VALID_TINY_WORKSHEET_CONTENTS = Seq(HEADER, blankLine("2"), stringLine("3"), stringLine("4"))

  private def standardLine(lineNumber: String): Seq[Cell] = Seq(dateCell(s"A$lineNumber"), integerCell(s"B$lineNumber", "78174"), stringCell(s"C$lineNumber"), integerCell(s"D$lineNumber", "200"))

  private def nameFrom(worksheet: Try[Worksheet]): String = worksheet.success.value.name

  private def headerFrom(worksheet: Try[Worksheet]): Seq[String] = worksheet.success.value.header.columnNames

  private def linesFrom(worksheet: Try[Worksheet]): Seq[Seq[Cell]] = worksheet.success.value.lines
    .map(_.cells)
    .map(cells ⇒ cells.map(cell ⇒ (cell.address, cell.value, cell.`type`, cell.mask, cell.formula, cell.note, cell.fontColor, cell.backgroundColor)))

  private def groupsFrom(worksheet: Try[Worksheet]): Seq[Seq[Seq[Cell]]] = worksheet.success.value.groups
    .map(group ⇒ group
      .map(line ⇒ line.cells
        .map(cell ⇒ (cell.address, cell.value, cell.`type`, cell.mask, cell.formula, cell.note, cell.fontColor, cell.backgroundColor))
      )
    )

  private def stringLine(lineNumber: String): Seq[Cell] =
    Seq(stringCell(s"A$lineNumber"), stringCell(s"B$lineNumber"), stringCell(s"C$lineNumber"), stringCell(s"D$lineNumber"))

  private def blankLine(lineNumber: String): Seq[Cell] =
    Seq(blankCell(s"A$lineNumber"), blankCell(s"B$lineNumber"), blankCell(s"C$lineNumber"), blankCell(s"D$lineNumber"))

  private def stringCell(address: String): Cell = (address, "VALE3", "STRING", "", "", "", "255,0,0", "")

  private def integerCell(address: String, number: String): Cell = (address, number, "INTEGER", "", "", "", "255,0,0", "")

  private def blankCell(address: String): Cell = (address, "", "STRING", "", "", "", "", "")

  private def dateCell(address: String): Cell = (address, "05/11/2008", "NUMERIC", "m/d/yy", "", "", "255,0,0", "")

  private def currencyCell(address: String): Cell = (address, "15,34", "NUMERIC", """"R$"\ #,##0.00;[Red]\-"R$"\ #,##0.00""", "", "", "255,0,0", "")