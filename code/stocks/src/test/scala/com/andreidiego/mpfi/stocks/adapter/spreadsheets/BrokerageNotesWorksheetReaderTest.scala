package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.Outcome
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Inspectors.{forAll, forExactly}

import java.io.File

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
      val worksheet = Worksheet.from(poiWorkbook.getSheet("1")).get

      "BrokerageNotesWorksheetReader.from(worksheet)" should compile
    }
    "turn every" - {
      "'Group' into a 'BrokerageNote' when all 'Lines' in the 'Group' have the same 'TradingDate' and 'BrokerageNote'." in { poiWorkbook ⇒
        val worksheet = Worksheet.from(poiWorkbook.getSheet("2")).get
        assume(worksheet.groups.size == 4)

        val brokerageNotes = BrokerageNotesWorksheetReader.from(worksheet).brokerageNotes

        brokerageNotes should have size 4

        forAll(brokerageNotes)(_ shouldBe a[BrokerageNote])
      }
      "non-'SummaryLine' into an 'Operation'" in { poiWorkbook ⇒
        val worksheet = Worksheet.from(poiWorkbook.getSheet("2")).get
        assume(worksheet.nonSummaryLines.size == 7)

        val operations = BrokerageNotesWorksheetReader.from(worksheet).operations

        operations should have size 7

        forAll(operations)(_ shouldBe a[Operation])
      }
      "'SummaryLine' into a 'FinancialSummary'" in { poiWorkbook ⇒
        val worksheet = Worksheet.from(poiWorkbook.getSheet("3")).get
        assume(worksheet.summaryLines.size == 2)

        val financialSummaries = BrokerageNotesWorksheetReader.from(worksheet).financialSummaries

        financialSummaries should have size 2

        forAll(financialSummaries)(_ shouldBe a[FinancialSummary])
      }
      "red non-'SummaryLine' into a 'BuyingOperation'" in { poiWorkbook ⇒
        val worksheet = Worksheet.from(poiWorkbook.getSheet("4")).get
        assume(worksheet.redNonSummaryLines.size == 6)

        val operations = BrokerageNotesWorksheetReader.from(worksheet).operations

        operations should have size 11

        forExactly(6, operations)(_ shouldBe a[BuyingOperation])
      }
    }
  }

object BrokerageNotesWorksheetReaderTest:
  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"

  private val FORMULA = "FORMULA"
  private val RED = "255,0,0"

  extension (worksheet: Worksheet)

    private def nonSummaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(!_.isSummary))

    private def summaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(isSummary))

    private def redNonSummaryLines: Seq[Line] =
      nonSummaryLines.filter(allNonEmptyCellsRed)

  extension (brokerageNotesWorksheetReader: BrokerageNotesWorksheetReader)

    private def operations: Seq[Operation] =
      brokerageNotesWorksheetReader.brokerageNotes.flatMap(_.operations)

    private def financialSummaries: Seq[FinancialSummary] =
      brokerageNotesWorksheetReader.brokerageNotes.map(_.financialSummary)

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def allNonEmptyCellsRed: Boolean = nonEmptyCells.forall(redFont)

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty

    private def redFont: Boolean = cell.fontColor == RED