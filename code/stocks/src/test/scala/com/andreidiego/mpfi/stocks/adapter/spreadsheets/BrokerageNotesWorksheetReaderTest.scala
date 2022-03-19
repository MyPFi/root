package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.Worksheet
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
    "turn every 'Group' into a 'BrokerageNote' when all 'Operations' in the 'Group' have the same 'TradingDate' and 'BrokerageNote'." in { poiWorkbook ⇒
      val worksheet = Worksheet.from(poiWorkbook.getSheet("2")).get
      assume(worksheet.groups.size == 4)

      val brokerageNotes = BrokerageNotesWorksheetReader.from(worksheet).brokerageNotes

      brokerageNotes should have size 4

      forAll(brokerageNotes)(_ shouldBe a[BrokerageNote])
    }
  }

object BrokerageNotesWorksheetReaderTest:
  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"