package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.Worksheet
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
import org.scalatest.Outcome
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.matchers.should.Matchers.*

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
  }

object BrokerageNotesWorksheetReaderTest:
  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"