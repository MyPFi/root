package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.Outcome
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Inspectors.{forAll, forExactly}
import org.scalatest.TryValues.*

import java.io.File
import scala.language.deprecated.symbolLiterals
import scala.util.Try

class BrokerageNotesWorksheetReaderTest extends FixtureAnyFreeSpec :

  import BrokerageNotesWorksheetReaderTest.*

  override protected type FixtureParam = XSSFWorkbook

  override protected def withFixture(test: OneArgTest): Outcome =
    val testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

    try withFixture(test.toNoArgTest(testWorkbook))
    finally testWorkbook.close()

  "A BrokerageNotesWorksheetReader should" - {
    "be built from a Worksheet" in { poiWorkbook ⇒
      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("1")).get

      "BrokerageNotesWorksheetReader.from(TEST_SHEET)" should compile
    }
    "fail to be built when" - {
      "given a 'Worksheet' whose" - {
        "'Groups' contain 'Lines' with different" - {
          "'TradingDate's." in { poiWorkbook ⇒
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupWithDifferentTradingDates")).get
            assume(TEST_SHEET.groups.size == 4)

            val exception = BrokerageNotesWorksheetReader.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              // Todo Replace the 'NoteNumber' below by the 'GroupIndex' after it has been added to the 'Group' class
              'message(s"An invalid 'Group' ('1662') was found on 'Worksheet' ${TEST_SHEET.name}. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '06/11/2008' in 'A3' is different from '05/11/2008' in 'A2'.")
            )
          }
          "'NoteNumbers's." in { poiWorkbook ⇒
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupWithDifferentNoteNumbers")).get
            assume(TEST_SHEET.groups.size == 4)

            val exception = BrokerageNotesWorksheetReader.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              'message(s"An invalid 'Group' ('1663') was found on 'Worksheet' ${TEST_SHEET.name}. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '1663' in 'B3' is different from '1662' in 'B2'.")
            )
          }
        }
        "'Line's contain 'Cell's" - {
          "with different font-colors." in { poiWorkbook ⇒
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("LineWithDifferentFontColors")).get

            val exception = BrokerageNotesWorksheetReader.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              // TODO Replace the information about the 'Line' below by the lineNumber after it has been introcuced in the 'Line' class
              'message(s"An invalid 'Line' ('05/11/2008 - 1662 - PETR4 - 200') was found on 'Worksheet' ${TEST_SHEET.name}. 'FontColor' should be the same for all 'Cell's in a 'Line' in order to being able to turn it into an 'Operation' but, '112,173,71' in 'B3' is different from '255,0,0' in 'A3'.")
            )
          }
          "whose font-colors are neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("LineWithBlackFontColor")).get

            val exception = BrokerageNotesWorksheetReader.from(TEST_SHEET).failure.exception

            exception should have(
              'class(classOf[IllegalArgumentException]),
              // TODO Replace the information about the 'Line' below by the lineNumber after it has been introcuced in the 'Line' class
              'message(s"An invalid 'Line' ('05/11/2008 - 1662 - GGBR4 - 100') was found on 'Worksheet' ${TEST_SHEET.name}. 'Line's should have font-color either red (255,0,0) or blue (68,114,196) in order to being able to turn them into 'Operation's but this 'Line' has font-color '0,0,0'.")
            )
          }
          "whose values are supposed to have been calculated from other 'Cell's but, do not pass the recalculation test, namely:" - {
            "For 'Operation's:" - {
              "'Volume', which should equal 'Qty' * 'Price'." in { poiWorkbook ⇒
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("VolumeDoesNotMatchQtyTimesPrice")).get

                val exception = BrokerageNotesWorksheetReader.from(TEST_SHEET).failure.exception

                exception should have(
                  'class(classOf[IllegalArgumentException]),
                  // TODO Replace the information about the 'Line' below by the lineNumber after it has been introcuced in the 'Line' class
                  'message(s"An invalid calculated 'Cell' ('F2:Volume') was found on 'Worksheet' ${TEST_SHEET.name}. It was supposed to contain '7030.0', which is equal to 'D2:Qty * E2:Price (200 * 35.15)' but, it actually contained '7030.01'.")
                )
              }
            }
          }
        }
      }
    }
    "turn every" - {
      "'Group' into a 'BrokerageNote' when all 'Lines' in the 'Group' have the same 'TradingDate' and 'BrokerageNote'." in { poiWorkbook ⇒
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSameTradingDate&Note")).get
        assume(TEST_SHEET.groups.size == 4)

        val brokerageNotes = BrokerageNotesWorksheetReader.from(TEST_SHEET).brokerageNotes

        brokerageNotes should have size 4

        forAll(brokerageNotes)(_ shouldBe a[BrokerageNote])
      }
      "non-'SummaryLine' into an 'Operation'." in { poiWorkbook ⇒
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSameTradingDate&Note")).get
        assume(TEST_SHEET.nonSummaryLines.size == 7)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 7

        forAll(operations)(_ shouldBe a[Operation])
      }
      "'SummaryLine' into a 'FinancialSummary'." in { poiWorkbook ⇒
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSummary")).get
        assume(TEST_SHEET.summaryLines.size == 2)

        val financialSummaries = BrokerageNotesWorksheetReader.from(TEST_SHEET).financialSummaries

        financialSummaries should have size 2

        forAll(financialSummaries)(_ shouldBe a[FinancialSummary])
      }
      "red non-'SummaryLine' into a 'BuyingOperation'." in { poiWorkbook ⇒
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("BuyingAndSellingOperations")).get
        assume(TEST_SHEET.redNonSummaryLines.size == 6)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 11

        forExactly(6, operations)(_ shouldBe a[BuyingOperation])
      }
      "blue non-'SummaryLine' into a 'SellingOperation'." in { poiWorkbook ⇒
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("BuyingAndSellingOperations")).get
        assume(TEST_SHEET.blueNonSummaryLines.size == 5)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 11

        forExactly(5, operations)(_ shouldBe a[SellingOperation])
      }
    }
    "generate a 'FinancialSummary', for 'Groups' of one 'Line', whose fields would replicate the corresponding ones from the one 'Line' in the 'Group'." in { poiWorkbook ⇒
      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SingleLineGroups")).get
      assume(TEST_SHEET.groups.size == 3)
      assume(TEST_SHEET.groups.forall(_.size == 1))

      val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations
      val financialSummaries = BrokerageNotesWorksheetReader.from(TEST_SHEET).financialSummaries

      financialSummaries should have size 3

      forAll(financialSummaries.zip(operations)) { financialSummaryAndOperation ⇒
        val financialSummary = financialSummaryAndOperation._1
        val operation = financialSummaryAndOperation._2

        financialSummary.volume should equal(operation.volume)
        financialSummary.settlementFee should equal(operation.settlementFee)
        financialSummary.negotiationFees should equal(operation.negotiationFees)
        financialSummary.brokerage should equal(operation.brokerage)
        financialSummary.serviceTax should equal(operation.serviceTax)
        financialSummary.incomeTaxAtSource should equal(operation.incomeTaxAtSource)
        financialSummary.total should equal(operation.total)
      }
    }
  }

object BrokerageNotesWorksheetReaderTest:
  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"

  private val FORMULA = "FORMULA"
  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

  extension (worksheet: Worksheet)

    private def nonSummaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(!_.isSummary))

    private def summaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(isSummary))

    private def redNonSummaryLines: Seq[Line] =
      nonSummaryLines.filter(allNonEmptyCellsRed)

    private def blueNonSummaryLines: Seq[Line] =
      nonSummaryLines.filter(allNonEmptyCellsBlue)

    // TODO This property should be added to the actual 'Worksheet' class
    private def name: String = "???Placeholder until we add the name field to the Worksheet class???"

  extension (brokerageNotesWorksheetReaderTry: Try[BrokerageNotesWorksheetReader])

    private def brokerageNotes: Seq[BrokerageNote] =
      brokerageNotesWorksheetReaderTry.success.value.brokerageNotes

    private def operations: Seq[Operation] =
      brokerageNotes.flatMap(_.operations)

    private def financialSummaries: Seq[FinancialSummary] =
      brokerageNotes.map(_.financialSummary)

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def allNonEmptyCellsRed: Boolean = nonEmptyCells.forall(redFont)

    private def allNonEmptyCellsBlue: Boolean = nonEmptyCells.forall(blueFont)

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty

    private def redFont: Boolean = cell.fontColor == RED

    private def blueFont: Boolean = cell.fontColor == BLUE