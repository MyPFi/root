package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.FixtureAnyFreeSpec

import excel.poi.Worksheet

class InvestmentsTest extends FixtureAnyFreeSpec, BeforeAndAfterAll:

  import java.io.File
  import org.apache.poi.openxml4j.opc.OPCPackage
  import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
  import org.scalatest.Outcome
  import org.scalatest.matchers.should.Matchers.*
  import com.andreidiego.mpfi.stocks.adapter.services.*
  import BrokerageNotesWorksheetReader.ServiceDependencies
  import BrokerageNotesWorksheetReader.BrokerageNoteReaderError.*
  import InvestmentsTest.*

  override protected type FixtureParam = (XSSFWorkbook, ServiceDependencies)

  private var testWorkbook: XSSFWorkbook = _
  private val serviceDependencies: ServiceDependencies = (
    ProvisionalAverageStockPriceService, 
    ProvisionalSettlementFeeRateService, 
    ProvisionalTradingFeesRateService, 
    ProvisionalServiceTaxRateService, 
    ProvisionalIncomeTaxAtSourceRateService
  )
  
  override protected def beforeAll(): Unit = 
    testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest((testWorkbook, serviceDependencies)))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  "Print errors in 'Notas de Corretagem'" in { (poiWorkbook, serviceDependencies) =>
    val TEST_SHEET_NAME = "Notas de Corretagem"
    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

    BrokerageNotesWorksheetReader.from(TEST_SHEET)(serviceDependencies).errors.foreach(println)
  }

object InvestmentsTest:
  import org.scalatest.EitherValues.*
  import BrokerageNotesWorksheetReader.ErrorsOr

  private val TEST_SPREADSHEET = "Investimentos.xlsx"

  extension (errorsOrBrokerageNotesWorksheetReader: ErrorsOr[BrokerageNotesWorksheetReader])

    private def errors: Seq[BrokerageNotesWorksheetReader.Error] =
      errorsOrBrokerageNotesWorksheetReader.toEither.left.value.toNonEmptyList.toList

  extension (errorsOrWorksheet: ErrorsOr[Worksheet])

    private def get: Worksheet =
      errorsOrWorksheet.toEither.value
