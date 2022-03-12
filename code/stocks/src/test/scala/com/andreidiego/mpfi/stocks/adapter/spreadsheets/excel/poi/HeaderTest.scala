package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Outcome
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.TryValues.*

import java.io.File
import scala.language.deprecated.symbolLiterals
import scala.util.Failure

class HeaderTest extends FixtureAnyFreeSpec :
  override protected type FixtureParam = XSSFWorkbook

  private val TEST_SPREADSHEET = "Header.xlsx"

  override protected def withFixture(test: OneArgTest): Outcome =
    val testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

    try withFixture(test.toNoArgTest(testWorkbook))
    finally testWorkbook.close()

  // TODO A Header is a line
  // TODO Replace Try + exceptions with Either
  "A Header should" - {
    "be built from a POI Row." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

      "Header.from(poiHeaderRow)" should compile
    }
    "be successfully built when given a POI Row that contains" - {
      "only" - {
        "non-empty cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

          Header.from(poiHeaderRow).success.value.columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
        }
        "non-blank (non-empty and separators) cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("HeaderNonEmptyCellsAndSeparator").getRow(0)

          Header.from(poiHeaderRow).success.value.columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "")
        }
        "non-empty and string-formula cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("StringFormulaInHeader").getRow(0)

          Header.from(poiHeaderRow).success.value.columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Data PregãoPapel", "Nota", "Papel")
        }
      }
    }
    "fail to be built when" - {
      "given a sequence of strings instead of a POI Row." in { poiWorkbook =>
        """Header(Seq("Column 1", "Column 2"))""" shouldNot compile
      }
      "given a POI Row" - {
        "that" - {
          "is" - {
            "null." in { poiWorkbook =>
              val exception = Header.from(null).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[NullPointerException]),
                'message("""Cannot invoke "org.apache.poi.xssf.usermodel.XSSFRow.getLastCellNum()" because "poiHeaderRow" is null""")
              )
            }
            "empty (no cells)." in { poiWorkbook =>
              val poiHeaderRow = new XSSFWorkbook().createSheet("1").createRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalArgumentException]),
                'message("Header is empty.")
              )
            }
          }
          "has" - {
            "only separator (empty but not blank) cells." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("OnlySeparatorsInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalArgumentException]),
                'message("Header is empty.")
              )
            }
            "more than one contiguous separator." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("ContiguousSeparatorsInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalArgumentException]),
                'message("Multiple contiguous separators not allowed.")
              )
            }
            "a blank (empty but not a separator) cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("EmptyCellInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalArgumentException]),
                'message("An illegal blank cell was found in the header.")
              )
            }
            "a numeric cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("NumberInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalStateException]),
                'message("Cannot get a STRING value from a NUMERIC cell")
              )
            }
            "a boolean cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("BooleanInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalStateException]),
                'message("Cannot get a STRING value from a BOOLEAN cell")
              )
            }
            "a date cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("DateInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

              exception should have(
                'class(classOf[IllegalArgumentException]),
                'message(s"Worksheet does not seem to have a valid header.")
              )

              exception.getCause should have(
                'class(classOf[IllegalStateException]),
                'message("Cannot get a STRING value from a NUMERIC cell")
              )
            }
            "a numeric formula cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("NumericFormulaInHeader").getRow(0)

              val exception = Header.from(poiHeaderRow).failure.exception

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
        "whose first cell is a separator." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("SeparatorFirstInHeader").getRow(0)

          val exception = Header.from(poiHeaderRow).failure.exception

          exception should have(
            'class(classOf[IllegalArgumentException]),
            'message(s"Worksheet does not seem to have a valid header.")
          )

          exception.getCause should have(
            'class(classOf[IllegalArgumentException]),
            'message("Separators not allowed at the beggining of the header.")
          )
        }
      }
    }
    "equal another Header with the same configuration." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

      Header.from(poiHeaderRow) should equal(Header.from(poiHeaderRow))
    }
    "not equal another Header with a different configuration." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)
      val poiRegularRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(2)

      Header.from(poiHeaderRow) should not equal Header.from(poiRegularRow)
    }
    "always have" - {
      "columnNames." in { poiWorkbook =>
        val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

        Header.from(poiHeaderRow).success.value.columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
      }
    }
    "forbid manipulation of its internal columnNames." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

      """Header.from(poiHeaderRow).success.value.columnNames = Seq("", "")""" shouldNot compile
    }
  }