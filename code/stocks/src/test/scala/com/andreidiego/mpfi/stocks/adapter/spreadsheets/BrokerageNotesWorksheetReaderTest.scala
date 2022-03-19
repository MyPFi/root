package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Line, Worksheet}
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.Outcome
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.Inspectors.forAll

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
    }
  }

object BrokerageNotesWorksheetReaderTest:
  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"

  private val FORMULA = "FORMULA"

  extension (worksheet: Worksheet)

    private def nonSummaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(!_.cells.filter(_.value.nonEmpty).forall(_.`type` == FORMULA)))

  extension (brokerageNotesWorksheetReader: BrokerageNotesWorksheetReader)

    private def operations: Seq[Operation] =
      brokerageNotesWorksheetReader.brokerageNotes.flatMap(_.operations)