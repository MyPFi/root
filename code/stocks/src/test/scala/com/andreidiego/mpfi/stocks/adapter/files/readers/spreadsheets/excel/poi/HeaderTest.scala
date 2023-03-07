package com.andreidiego.mpfi.stocks.adapter.files.readers.spreadsheets.excel.poi

import org.scalatest.BeforeAndAfterAll
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.freespec.FixtureAnyFreeSpec

class HeaderTest extends FixtureAnyFreeSpec, BeforeAndAfterAll:

  import java.io.File
  import scala.language.deprecated.symbolLiterals
  import org.apache.poi.openxml4j.opc.OPCPackage
  import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
  import org.scalatest.Outcome
  import Header.HeaderError.IllegalArgument
  import HeaderTest.{*, given}

  override protected type FixtureParam = XSSFWorkbook

  private val TEST_SPREADSHEET = "Header.xlsx"
  private var testWorkbook: XSSFWorkbook = _
  
  override protected def beforeAll(): Unit = 
    testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest(testWorkbook))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  // TODO A Header is a line
  "A Header should" - {
    "be built from a POI Row." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

      "Header.from(poiHeaderRow)" should compile
    }
    "be successfully built when given a POI Row that contains" - {
      "only" - {
        "non-empty cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

          Header.from(poiHeaderRow).columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
        }
        "non-blank (non-empty and separators) cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("HeaderNonEmptyCellsAndSeparator").getRow(0)

          Header.from(poiHeaderRow).columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "")
        }
        "non-empty and string-formula cells." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("StringFormulaInHeader").getRow(0)

          Header.from(poiHeaderRow).columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Data PregãoPapel", "Nota", "Papel")
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
              val error = Header.from(null).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header: 'null'.")
              )
            }
            "empty (no cells)." in { poiWorkbook =>
              val poiHeaderRow = new XSSFWorkbook().createSheet("1").createRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. Header is empty.")
              )
            }
          }
          "has" - {
            "only separator (empty but not blank) cells." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("OnlySeparatorsInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. Header is empty.")
              )
            }
            "more than one contiguous separator." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("ContiguousSeparatorsInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. Multiple contiguous separators not allowed.")
              )
            }
            "a blank (empty but not a separator) cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("EmptyCellInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. An illegal blank cell was found in the header.")
              )
            }
            "a numeric cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("NumberInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. An illegal numeric cell was found in the header.")
              )
            }
            "a boolean cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("BooleanInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. An illegal boolean cell was found in the header.")
              )
            }
            "a date cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("DateInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. An illegal date cell was found in the header.")
              )
            }
            "a numeric formula cell." in { poiWorkbook =>
              val poiHeaderRow = poiWorkbook.getSheet("NumericFormulaInHeader").getRow(0)

              val error = Header.from(poiHeaderRow).error

              error should have(
                'class(classOf[IllegalArgument]),
                'message(s"Worksheet does not seem to have a valid header. An illegal numeric formula cell was found in the header.")
              )
            }
          }
        }
        "whose first cell is a separator." in { poiWorkbook =>
          val poiHeaderRow = poiWorkbook.getSheet("SeparatorFirstInHeader").getRow(0)

          val error = Header.from(poiHeaderRow).error

          error should have(
            'class(classOf[IllegalArgument]),
            'message(s"Worksheet does not seem to have a valid header. Separators not allowed at the beggining of the header.")
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

        Header.from(poiHeaderRow).columnNames should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "Qtde")
      }
    }
    "forbid manipulation of its internal columnNames." in { poiWorkbook ⇒
      val poiHeaderRow = poiWorkbook.getSheet("ValidTinyWorksheet").getRow(0)

      """Header.from(poiHeaderRow).columnNames = Seq("", "")""" shouldNot compile
    }
  }

object HeaderTest:

  import Header.ErrorsOr

  given Conversion[ErrorsOr[Header], Header] = _.toEither.value

  extension (errorsOrHeader: ErrorsOr[Header])

    private def error: Header.Error =
      errorsOrHeader.toEither.left.value.head