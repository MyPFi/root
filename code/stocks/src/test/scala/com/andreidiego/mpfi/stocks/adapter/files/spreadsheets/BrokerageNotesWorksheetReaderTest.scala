package com.andreidiego.mpfi.stocks.adapter.files.spreadsheets

import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.FixtureAnyFreeSpec

import excel.poi.Worksheet

class BrokerageNotesWorksheetReaderTest extends FixtureAnyFreeSpec, BeforeAndAfterAll:

  import java.io.File
  import org.apache.poi.openxml4j.opc.OPCPackage
  import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
  import scala.language.deprecated.symbolLiterals
  import org.scalatest.Outcome
  import org.scalatest.Inspectors.{forAll, forExactly}
  import org.scalatest.matchers.should.Matchers.*
  import BrokerageNotesWorksheetMessages.*
  import BrokerageNotesWorksheetReader.ServiceDependencies
  import BrokerageNotesWorksheetReader.BrokerageNoteReaderError.*
  import com.andreidiego.mpfi.stocks.adapter.services.*
  import com.andreidiego.mpfi.stocks.adapter.services.TradingPeriod.*
  import BrokerageNotesWorksheetTestMessages.*
  import BrokerageNotesWorksheetReaderTest.*

  override protected type FixtureParam = XSSFWorkbook

  private var testWorkbook: XSSFWorkbook = _
  private given serviceDependencies: ServiceDependencies = (
    DummyAverageStockPriceService, 
    DummySettlementFeeRateService, 
    DummyTradingFeesRateService, 
    DummyServiceTaxRateService, 
    DummyIncomeTaxAtSourceRateService
  )
  
  override protected def beforeAll(): Unit = 
    testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest(testWorkbook))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  "A BrokerageNotesWorksheetReader should" - {
    "be built from a Worksheet" in { poiWorkbook =>
      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NotUsed")).get

      "BrokerageNotesWorksheetReader.from(TEST_SHEET)" should compile
    }
    "fail to be built when" - {
      "given a 'Worksheet'" - {
        "containing invalid 'BrokerageNote's, that is, 'BrokerageNote's that" - {
          "contain 'Operation's which are either:" - {
            "Invalid, due to" - { 
              "invalid 'Attribute's, like:" - {
                "'TradingDate'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingDateMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(
                      RequiredValueMissing(tradingDateMissing(2))
                    )
                  }
                  "if negative." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingDateNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(
                      UnexpectedContentType(unexpectedContentTypeInTradingDate("-39757", 2))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers and the  '/' symbol)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingDateExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(
                      UnexpectedContentType(unexpectedContentTypeInTradingDate("3O/12/2009", 2))
                    )
                  }
                  "when containing an invalid date." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingDateInvalidDate"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(
                      UnexpectedContentType(unexpectedContentTypeInTradingDate("30/13/2009", 2))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "TradingDateBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTradingDate("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingDateRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTradingDate(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingDateBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTradingDate(2))
                        )
                      }
                    }
                  }
                }
                "'NoteNumber'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "NoteNumberMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(noteNumberMissing(2))
                    )
                  }
                  "if negative." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "NoteNumberNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativeNoteNumber("-1662", 2))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "NoteNumberExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(unexpectedContentTypeInNoteNumber("I662", 2))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "NoteNumberBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInNoteNumber("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "NoteNumberRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInNoteNumber(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "NoteNumberBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInNoteNumber(2))
                        )
                      }
                    }
                  }
                }
                "'Ticker'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TickerMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(tickerMissing(2))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "TickerBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTicker("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TickerRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTicker(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TickerBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTicker(2))
                        )
                      }
                    }
                  }
                }
                "'Qty'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "QtyMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(qtyMissing(2))
                    )
                  }
                  "if negative." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "QtyNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativeQty("-100", 2))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "QtyExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInQty("l00", 2)))
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "QtyBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInQty("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "QtyRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInQty(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "QtyBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInQty(2))
                        )
                      }
                    }
                  }
                }
                "'Price'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "PriceMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(priceMissing(2))
                    )
                  }
                  "if negative." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "PriceNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativePrice("-15.34", 2))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "PriceExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(priceNotCurrency("R$ l5,34", 2)),
                      UnexpectedContentType(priceNotDouble("R$ l5,34", 2))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "PriceBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInPrice("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "PriceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInPrice(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "PriceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInPrice(2))
                        )
                      }
                    }
                  }
                }
                "'Volume'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "VolumeMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(volumeMissing(2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "VolumeExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(volumeNotCurrency("R$ l534,00", 2)),
                      UnexpectedContentType(volumeNotDouble("R$ l534,00", 2))
                    )
                  }
                  "if different than 'Qty' * 'Price'." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "VolumeDoesNotMatchQtyTimesPrice"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolume("7030.01", 2)("7030.00", "200", "35.15"))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "VolumeBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInVolume("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "VolumeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInVolume(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "VolumeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInVolume(2))
                        )
                      }
                    }
                  }
                }
                "'SettlementFee'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "SettlementFeeMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(settlementFeeMissing(2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "SettlementFeeExtraneousChars"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(settlementFeeNotCurrency("R$ O,42", 2)),
                      UnexpectedContentType(settlementFeeNotDouble("R$ O,42", 2))
                    )
                  }
                  "if different than 'Volume' * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' + a tolerance of:" - {
                    "+'0.01', when 'OperationalMode' is" - {
                      "'Normal'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "SettlementFeeAboveTolerance"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                        errors should contain allOf(
                          UnexpectedContentValue(unexpectedSettlementFee("3.05", 2)("3.03", "11000.00", "0.0275%")),
                          UnexpectedContentValue(unexpectedSettlementFee("3.05", 2)("2.20", "11000.00", "0.0200%"))
                        )
                      }
                      "'DayTrade', be it" - {
                        "highlighted." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HDTSettlementFeeAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedSettlementFee("2.22", 2)("2.20", "11000.00", "0.0200%"))
                          )
                        }
                        "or not." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCDTSettlementFeeAboveToleranc"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedSettlementFee("2.22", 2)("3.03", "11000.00", "0.0275%")),
                            UnexpectedContentValue(unexpectedSettlementFee("2.22", 2)("2.20", "11000.00", "0.0200%"))
                          )
                        }
                      }
                    }
                    "-'0.01', when 'OperationalMode' is" - {
                      "'Normal'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "SettlementFeeBelowTolerance"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                        errors should contain allOf(
                          UnexpectedContentValue(unexpectedSettlementFee("3.01", 2)("3.03", "11000.00", "0.0275%")),
                          UnexpectedContentValue(unexpectedSettlementFee("3.01", 2)("2.20", "11000.00", "0.0200%"))
                        )
                      }
                      "'DayTrade', be it" - {
                        "highlighted." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HDTSettlementFeeBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedSettlementFee("2.18", 2)("2.20", "11000.00", "0.0200%"))
                          )
                        }
                        "or not." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCDTSettlementFeeBelowToleranc"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedSettlementFee("2.18", 2)("3.03", "11000.00", "0.0275%")),
                            UnexpectedContentValue(unexpectedSettlementFee("2.18", 2)("2.20", "11000.00", "0.0200%"))
                          )
                        }
                      }
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "SettlementFeeBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInSettlementFee("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "SettlementFeeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInSettlementFee(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "SettlementFeeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInSettlementFee(2))
                        )
                      }
                    }
                  }
                }
                "'TradingFees'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingFeesMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(tradingFeesMissing(2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TradingFeesExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(tradingFeesNotCurrency("R$ O,44", 2)),
                      UnexpectedContentType(tradingFeesNotDouble("R$ O,44", 2))
                    )
                  }
                  "if different than 'Volume' * 'TradingFeesRate' at 'TradingDateTime' + a tolerance of:" - {
                    "+'0.01', when 'TradingTime' falls within" - {
                      "'PreOpening', be it" - {
                        "highlighted." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HPreOpTradingFeesAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "containing a note." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NPreOTradingFeesAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "with no visual clue whatsoever." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCPOTradingFeesAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedTradingFees("0.79", 2)("3.14", "11000.00", "0.0285%")),
                            UnexpectedContentValue(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                      }
                      "'Trading'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingFeesAboveTolerance"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentValue]),
                          'message(unexpectedTradingFees("3.16", 2)("3.14", "11000.00", "0.0285%"))
                        )
                      }
                      "'ClosingCall', be it" - {
                        "highlighted." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HCCallTradingFeesAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "containing a note." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NCCTradingFeesAboveTolererance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "with no visual clue whatsoever." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCCCTradingFeesAboveTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedTradingFees("0.79", 2)("3.14", "11000.00", "0.0285%")),
                            UnexpectedContentValue(unexpectedTradingFees("0.79", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                      }
                    }
                    "-'0.01', when 'TradingTime' falls within" - {
                      "'PreOpening', be it" - {
                        "highlighted." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HPreOpTradingFeesBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "containing a note." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NPreOTradingFeesBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "with no visual clue whatsoever." in { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCPOTradingFeesBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedTradingFees("0.75", 2)("3.14", "11000.00", "0.0285%")),
                            UnexpectedContentValue(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                      }
                      "'Trading'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingFeesBelowTolerance"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentValue]),
                          'message(unexpectedTradingFees("3.12", 2)("3.14", "11000.00", "0.0285%"))
                        )
                      }
                      "'ClosingCall', be it" - {
                        "highlighted." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "HCCallTradingFeesBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "containing a note." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NCCTradingFeesBelowTolererance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                          error should have(
                            'class(classOf[UnexpectedContentValue]),
                            'message(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                        "with no visual clue whatsoever." ignore { poiWorkbook =>
                          given TEST_SHEET_NAME: String = "NVCCCTradingFeesBelowTolerance"
                          val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                          val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                          errors should contain allOf(
                            UnexpectedContentValue(unexpectedTradingFees("0.75", 2)("3.14", "11000.00", "0.0285%")),
                            UnexpectedContentValue(unexpectedTradingFees("0.75", 2)("0.77", "11000.00", "0.0070%"))
                          )
                        }
                      }
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "TradingFeesBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTradingFees("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingFeesRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTradingFees(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TradingFeesBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTradingFees(2))
                        )
                      }
                    }
                  }
                }
                "'Brokerage'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "BrokerageMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(brokerageMissing(2)))
                  }
                  "if negative." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "BrokerageNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentValue(unexpectedNegativeBrokerage("-15.99", 2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "BrokerageExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(brokerageNotCurrency("R$ l5,99", 2)),
                      UnexpectedContentType(brokerageNotDouble("R$ l5,99", 2))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "BrokerageBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInBrokerage("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "BrokerageRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInBrokerage(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "BrokerageBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInBrokerage(2))
                        )
                      }
                    }
                  }
                }
                "'ServiceTax'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "ServiceTaxMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(serviceTaxMissing(2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "ServiceTaxExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(serviceTaxNotCurrency("R$ O,8O", 2)),
                      UnexpectedContentType(serviceTaxNotDouble("R$ O,8O", 2))
                    )
                  }
                  "if different than 'Brokerage' * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' + a tolerance of:" - {
                    "+'0.01'." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "ServiceTaxAboveTolerance"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedServiceTax("0.12", 2)("0.10", "1.99", "5.0%"))
                      )
                    }
                    "-'0.01'." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "ServiceTaxBelowTolerance"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedServiceTax("0.08", 2)("0.10", "1.99", "5.0%"))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "ServiceTaxBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInServiceTax("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "ServiceTaxRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInServiceTax(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "ServiceTaxBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInServiceTax(2))
                        )
                      }
                    }
                  }
                }
                "'IncomeTaxAtSource'" - {
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "IncomeTaxAtSourceExtraneousChar"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(incomeTaxAtSourceNotCurrency("R$ O,OO", 2)),
                      UnexpectedContentType(incomeTaxAtSourceNotDouble("R$ O,OO", 2))
                    )
                  }
                  "if different than" - {
                    "for 'SellingOperations', (('Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax') - ('AverageStockPrice' for the 'Ticker' * 'Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' when 'OperationalMode' is 'Normal'" in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "InvalidIncomeTaxAtSource"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedIncomeTaxAtSourceForSellings("0.19", 2)("0.09", "1801.75", "0.0050%"))
                      )
                    }
                    "for 'BuyingOperations, if not empty, zero." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "NonZeroIncomeTaxAtSourceBuying"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedIncomeTaxAtSourceForBuyings("0.01", 2))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "IncomeTaxAtSourceBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInIncomeTaxAtSource("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "IncomeTaxAtSourceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInIncomeTaxAtSource(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "IncomeTaxAtSourceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInIncomeTaxAtSource(2))
                        )
                      }
                    }
                  }
                }
                "'Total'" - {
                  "when missing." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TotalMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(totalMissing(2)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "TotalExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain allOf(
                      UnexpectedContentType(totalNotCurrency("R$ l551,32", 2)),
                      UnexpectedContentType(totalNotDouble("R$ l551,32", 2))
                    )
                  }
                  "if different than" - {
                    "for 'SellingOperations', 'Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax'." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "InvalidTotalForSelling"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTotalForSellings("7009.24", 2)("7009.27"))
                      )
                    }
                    "for 'BuyingOperations', 'Volume' + 'SettlementFee' + 'TradingFees' + 'Brokerage' + 'ServiceTax'." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "InvalidTotalForBuying"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTotalForBuyings("11008.22", 2)("11008.25"))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (91,155,213)." in { poiWorkbook =>
                      given TEST_SHEET_NAME: String = "TotalBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTotal("0,0,0", 2))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TotalRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTotal(2))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook =>
                        given TEST_SHEET_NAME: String = "TotalBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTotal(2))
                        )
                      }
                    }
                  }
                }
              }
              "the impossibility of determining its type when it has exactly half of it's" - { 
                "'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "NoEmptyAttributeHalfOfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2))
                  )
                }
                "non-empty 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "NonEmptyAttribsHalfOfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2))
                  )
                }
                "valid-colored 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "ValidColoredAttribHalfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2))
                  )
                }
                "non-empty valid-colored 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "NEValidColorAttrHalfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2))
                  )
                }
              }
            }
            "Not harmonic with each other, that is, contain different" - {
              "'TradingDate's." in { poiWorkbook =>
                given TEST_SHEET_NAME: String = "GroupWithDifferentTradingDates"
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  // TODO Replace the 'NoteNumber' below by the 'GroupIndex' after it has been added to the 'Group' class
                  'message(conflictingTradingDate("A3", "1662", "31/12/2009")("30/12/2009", "A2"))
                )
              }
              "'NoteNumbers's." in { poiWorkbook =>
                given TEST_SHEET_NAME: String = "GroupWithDifferentNoteNumbers"
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  'message(conflictingNoteNumber("B3", "1663", "1663")("1662", "B2"))
                )
              }
            }
          }
          "having more than one 'Operation'" - { 
            "don't have a 'Summary (last 'Operation' will be taken as the 'Summary')'." in { poiWorkbook =>
              given TEST_SHEET_NAME: String = "MultiLineGroupWithNoSummary"
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

              val expectedErrors = Seq(
                UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("11600.00", 3)("11000.00", 2, 2)),
                UnexpectedContentValue(unexpectedSettlementFeeSummary("3.19", 3)("3.03", 2, 2)),
                UnexpectedContentValue(unexpectedTradingFeesSummary("3.31", 3)("3.14", 2, 2)),
                UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("11608.59", 3)("11008.25", 2, 2))
              )

              val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

              errors should have size 4
              errors should contain theSameElementsAs expectedErrors
            }
            "have an invalid 'Summary', in which" - {
              "'VolumeSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "VolumeSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(volumeSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "VolumeSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(volumeSummaryNotCurrency("-R$ 9.322,OO", 5)),
                    UnexpectedContentType(volumeSummaryNotDouble("-R$ 9.322,OO", 5))
                  )
                }
                "is different than the sum of the 'Volume's of all" - {
                  "'Operation's, for homogenoues groups (comprised exclusively of 'Operation's of the same type)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "InvalidVolumeSummaryHomogGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolumeSummaryForHomogeneousGroups("-2110.00", 4)("16810.00", 2, 3))
                    )
                  }
                  "'SellingOperation's minus the sum of the 'Volume's of all 'BuyingOperation's for mixed groups (comprised of 'Operation's from different types)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "InvalidVolumeSummaryMixedGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolumeSummaryForHeterogeneousGroups("16810.00", 4)("-2110.00", 2, 3))
                    )
                  }
                }
              }
              "'SettlementFeeSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "SettlementFeeSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(settlementFeeSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "SettlementFeeSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(settlementFeeSummaryNotCurrency("R$ 2,S6", 5)),
                    UnexpectedContentType(settlementFeeSummaryNotDouble("R$ 2,S6", 5))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "InvalidSettlementFeeSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedSettlementFeeSummary("6.25", 4)("6.22", 2, 3))
                  )
                }
              }
              "'TradingFeesSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "TradingFeesSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(tradingFeesSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "TradingFeesSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(tradingFeesSummaryNotCurrency("R$ O,65", 5)),
                    UnexpectedContentType(tradingFeesSummaryNotDouble("R$ O,65", 5))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "InvalidTradingFeesSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedTradingFeesSummary("6.41", 4)("6.44", 2, 3))
                  )
                }
              }
              "'BrokerageSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "BrokerageSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(brokerageSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "BrokerageSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(brokerageSummaryNotCurrency("R$ 4T,97", 5)),
                    UnexpectedContentType(brokerageSummaryNotDouble("R$ 4T,97", 5))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "InvalidBrokerageSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedBrokerageSummary("3.95", 4)("3.98", 2, 3))
                  )
                }
              }
              "'ServiceTaxSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "ServiceTaxSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(serviceTaxSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "ServiceTaxSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(serviceTaxSummaryNotCurrency("R$ 2,4O", 5)),
                    UnexpectedContentType(serviceTaxSummaryNotDouble("R$ 2,4O", 5))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "InvalidServiceTaxSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedServiceTaxSummary("0.23", 4)("0.20", 2, 3))
                  )
                }
              }
              "'IncomeTaxAtSourceSummary'" - {
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "IncomeTaxAtSourceSummExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(incomeTaxAtSourceSummaryNotCurrency("R$ O,OO", 5)),
                    UnexpectedContentType(incomeTaxAtSourceSummaryNotDouble("R$ O,OO", 5))
                  )
                }
                // TODO There are a few of special cases when it comes to IncomeTaxAtSourceSummary: It could be either empty or zero for Buyings and, empty, zero, or have a greater than zero value for Sellings
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "InvalidIncomeTaxAtSourceSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedIncomeTaxAtSourceSummary("0.05", 5)("0.08", 2, 4))
                  )
                }
              }
              "'TotalSummary'" - {
                "is missing." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "TotalSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(totalSummaryMissing(5))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook =>
                  given TEST_SHEET_NAME: String = "TotalSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  errors should contain allOf(
                    UnexpectedContentType(totalSummaryNotCurrency("-R$ 9.37S,S9", 5)),
                    UnexpectedContentType(totalSummaryNotDouble("-R$ 9.37S,S9", 5))
                  )
                }
                "is different than the sum of the 'Total's of all" - {
                  "'Operation's, for homogenoues groups (comprised exclusively of 'Operation's of the same type)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "InvalidTotalSummaryHomogGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedTotalSummaryForHomogeneousGroups("-2111.18", 4)("16824.64", 2, 3))
                    )
                  }
                  "'SellingOperation's minus the sum of the 'Total's of all 'BuyingOperation's, for mixed groups (comprised of 'Operation's from different types)." in { poiWorkbook =>
                    given TEST_SHEET_NAME: String = "InvalidTotalSummaryMixedGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedTotalSummaryForHeterogeneousGroups("16824.64", 4)("-2111.18", 2, 3))
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    "accumulate errors" in { poiWorkbook =>
      given TEST_SHEET_NAME: String = "MultipleErrors"
      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

      val expectedErrors = Seq(
        // Line 2
        RequiredValueMissing(tradingDateMissing(2)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedSettlementFee("-0.42", 2)("-0.12", "-1534.00", "0.0079%")),
          UnexpectedContentValue(unexpectedSettlementFee("-0.42", 2)("-0.10", "-1534.00", "0.0063%")),
          UnexpectedContentValue(unexpectedTradingFees("-0.44", 2)("-0.41", "-1534.00", "0.0270%")),
          UnexpectedContentValue(unexpectedTradingFees("-0.44", 2)("-0.31", "-1534.00", "0.0205%")),
        // -CONSEQUENTIAL
        UnexpectedContentType(unexpectedContentTypeInNoteNumber("II62", 2)),
        UnexpectedContentValue(unexpectedNegativeQty("-100", 2)),
        UnexpectedContentColor(unexpectedColorForBuyingInVolume(2)),
        UnexpectedContentValue(unexpectedServiceTax("1.10", 2)("0.80", "15.99", "5.0%")),
        UnexpectedContentValue(unexpectedTotalForBuyings("-1517.47", 2)("-1517.77")),

        // Line 4
        UnexpectedContentType(unexpectedContentTypeInTradingDate("40177", 4)),
        RequiredValueMissing(noteNumberMissing(4)),
        UnexpectedContentType(unexpectedContentTypeInQty("S00", 4)),
        // +CONSEQUENTIAL
          UnexpectedContentType(volumeNotCurrency("#VALUE!", 4)),
          UnexpectedContentType(volumeNotDouble("#VALUE!", 4)),
          UnexpectedContentType(settlementFeeNotCurrency("#VALUE!", 4)),
          UnexpectedContentType(settlementFeeNotDouble("#VALUE!", 4)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 4)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 4)),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 4)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 4)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 4)("16.79")),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForBuyingInBrokerage(4)),

        // Line 6
        UnexpectedContentType(unexpectedContentTypeInTradingDate("3O/12/2009", 6)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedSettlementFee("0.76", 6)("0.22", "2750.00", "0.0079%")),
          UnexpectedContentValue(unexpectedSettlementFee("0.76", 6)("0.17", "2750.00", "0.0063%")),
          UnexpectedContentValue(unexpectedTradingFees("1.08", 6)("0.74", "2750.00", "0.0270%")),
          UnexpectedContentValue(unexpectedTradingFees("1.08", 6)("0.56", "2750.00", "0.0205%")),
        // -CONSEQUENTIAL
        UnexpectedContentValue(unexpectedNegativeNoteNumber("-1662", 6)),
        RequiredValueMissing(tickerMissing(6)),
        UnexpectedContentColor(unexpectedColorForBuyingInQty(6)),
        UnexpectedContentColor(unexpectedColorForBuyingInTradingFees(6)),
        UnexpectedContentType(brokerageNotCurrency("R$ l5,99", 6)),
        UnexpectedContentType(brokerageNotDouble("R$ l5,99", 6)),
        // +CONSEQUENTIAL
          UnexpectedContentType(totalNotCurrency("#VALUE!", 6)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 6)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 6)("2751.84")),
        // -CONSEQUENTIAL
        UnexpectedContentType(serviceTaxNotCurrency("R$ O,8O", 6)),
        UnexpectedContentType(serviceTaxNotDouble("R$ O,8O", 6)),

        // Line 8
        UnexpectedContentType(unexpectedContentTypeInTradingDate("-40177", 8)),
        UnexpectedContentColor(unexpectedColorForBuyingInTicker(8)),
        RequiredValueMissing(qtyMissing(8)),
        UnexpectedContentValue(unexpectedNegativeBrokerage("-15.99", 8)),
        UnexpectedContentColor(unexpectedColorForBuyingInTotal(8)),

        // Line 10
        UnexpectedContentColor(unexpectedColorForBuyingInNoteNumber(10)),
        RequiredValueMissing(priceMissing(10)),
        UnexpectedContentColor(unexpectedColorForBuyingInSettlementFee(10)),
        UnexpectedContentType(tradingFeesNotCurrency("R$ O,OO", 10)),
        UnexpectedContentType(tradingFeesNotDouble("R$ O,OO", 10)),
        // TODO The following is not exactly CONSEQUENTIAL since it's not a formula. It is going to give reason to a Warning when warnings are implemented.
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForBuyings("16.90", 10)("16.79")),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForBuyings("0.01", 10)),

        // Line 11
        UnexpectedContentValue(conflictingTradingDate("A11", "1345", "31/12/2009")("30/12/2009", "A10")),
        UnexpectedContentValue(conflictingNoteNumber("B11", "1345", "1345")("1344", "B10")),
        UnexpectedContentColor(unexpectedColorForBuyingInPrice(11)),
        RequiredValueMissing(volumeMissing(11)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolume("0.00", 11)("769.00", "100", "7.69")),
        UnexpectedContentColor(unexpectedColorForBuyingInServiceTax(11)),
        UnexpectedContentColor(unexpectedColorForBuyingInIncomeTaxAtSource(11)),

        // Line 12
        UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.30", 12)("0.00", 10, 11)),
        UnexpectedContentValue(unexpectedSettlementFeeSummary("0.30", 12)("0.00", 10, 11)),
        UnexpectedContentValue(unexpectedTradingFeesSummary("0.30", 12)("0.00", 10, 11)),
        UnexpectedContentValue(unexpectedBrokerageSummary("32.28", 12)("31.98", 10, 11)),
        UnexpectedContentValue(unexpectedServiceTaxSummary("1.90", 12)("1.60", 10, 11)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.31", 12)("0.01", 10, 11)),
        UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("33.99", 12)("33.69", 10, 11)),

        // Line 14
        UnexpectedContentColor(unexpectedColorForBuyingInTradingDate(14)),
        UnexpectedContentType(priceNotCurrency("R$ l5,34", 14)),
        UnexpectedContentType(priceNotDouble("R$ l5,34", 14)),
        // +CONSEQUENTIAL
          UnexpectedContentType(volumeNotCurrency("#VALUE!", 14)),
          UnexpectedContentType(volumeNotDouble("#VALUE!", 14)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 14)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 14)),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 14)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 14)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 14)("16.79")),
        // -CONSEQUENTIAL
        RequiredValueMissing(settlementFeeMissing(14)),

        // Line 16
        UnexpectedContentColor(unexpectedColorForSellingInTradingDate(16)),
        UnexpectedContentColor(unexpectedColorForSellingInQty(16)),
        UnexpectedContentValue(unexpectedNegativePrice("-31.5", 16)),
        UnexpectedContentValue(unexpectedVolume("3150.00", 16)("-3150.00", "100", "-31.50")),
        UnexpectedContentValue(unexpectedSettlementFee("1.17", 16)("0.87", "3150.00", "0.0275%")),
        UnexpectedContentValue(unexpectedSettlementFee("1.17", 16)("0.63", "3150.00", "0.0200%")),
        UnexpectedContentColor(unexpectedColorForSellingInSettlementFee(16)),
        RequiredValueMissing(tradingFeesMissing(16)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedTradingFees("0.00", 16)("0.90", "3150.00", "0.0285%")),
          UnexpectedContentValue(unexpectedTradingFees("0.00", 16)("0.22", "3150.00", "0.0070%")),
        // -CONSEQUENTIAL
        UnexpectedContentValue(unexpectedTotalForSellings("3132.34", 16)("3132.04")),

        // Line 17
        UnexpectedContentColor(unexpectedColorForSellingInNoteNumber(17)),
        UnexpectedContentColor(unexpectedColorForSellingInPrice(17)),
        UnexpectedContentType(settlementFeeNotDouble("R$ O,87", 17)),
        UnexpectedContentType(settlementFeeNotCurrency("R$ O,87", 17)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedSettlementFee("0.00", 17)("0.87", "3150.00", "0.0275%")),
          UnexpectedContentValue(unexpectedSettlementFee("0.00", 17)("0.63", "3150.00", "0.0200%")),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 17)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 17)),
          UnexpectedContentValue(unexpectedTotalForSellings("0.00", 17)("3149.10")),
          UnexpectedContentType(settlementFeeSummaryNotCurrency("#VALUE!", 20)),
          UnexpectedContentType(settlementFeeSummaryNotDouble("#VALUE!", 20)),
          UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 20)("2.60", 16, 19)),
          UnexpectedContentType(totalSummaryNotCurrency("#VALUE!", 20)),
          UnexpectedContentType(totalSummaryNotDouble("#VALUE!", 20)),
          UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 20)("3132.34", 16, 19)),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForSellingInTradingFees(17)),
        RequiredValueMissing(brokerageMissing(17)),
        UnexpectedContentType(incomeTaxAtSourceNotCurrency("R$ O,OO", 17)),
        UnexpectedContentType(incomeTaxAtSourceNotDouble("R$ O,OO", 17)),
        UnexpectedContentColor(unexpectedColorForSellingInTotal(17)),

        // Line 18
        UnexpectedContentColor(unexpectedColorForSellingInTicker(18)),
        UnexpectedContentColor(unexpectedColorForSellingInVolume(18)),
        UnexpectedContentType(volumeNotCurrency("R$ 1770,OO", 18)),
        UnexpectedContentType(volumeNotDouble("R$ 1770,OO", 18)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedVolume("0.00", 18)("1770.00", "100", "17.70")),
          UnexpectedContentType(settlementFeeNotDouble("#VALUE!", 18)),
          UnexpectedContentType(settlementFeeNotCurrency("#VALUE!", 18)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 18)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 18)),
          UnexpectedContentType(tradingFeesSummaryNotCurrency("#VALUE!", 20)),
          UnexpectedContentType(tradingFeesSummaryNotDouble("#VALUE!", 20)),
          UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 20)("2.39", 16, 19)),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForSellingInBrokerage(18)),
        RequiredValueMissing(serviceTaxMissing(18)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTax("0.00", 18)("0.80", "15.99", "5.0%")),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForSellings("-0.50", 18)("-0.00", "-15.99", "0.0050%")),
        UnexpectedContentType(totalNotCurrency("R$ l.753,43", 18)),
        UnexpectedContentType(totalNotDouble("R$ l.753,43", 18)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForSellings("0.00", 18)("-15.99")),

        // Line 19
        UnexpectedContentColor(unexpectedColorForSellingInServiceTax(19)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForSellings("0.05", 19)("0.26", "5200.29", "0.0050%")),
        UnexpectedContentColor(unexpectedColorForSellingInIncomeTaxAtSource(19)),
        RequiredValueMissing(totalMissing(19)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForSellings("0.00", 19)("5200.29")),

        // Line 22
        UnexpectedContentColor(invalidColorInTradingDate("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInNoteNumber("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInTicker("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInQty("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInPrice("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInVolume("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInSettlementFee("0,0,0", 22)),
        /* CONSEQUENTIAL */ UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(22)),
        UnexpectedContentColor(invalidColorInTradingFees("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInBrokerage("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInServiceTax("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInIncomeTaxAtSource("0,0,0", 22)),
        UnexpectedContentColor(invalidColorInTotal("0,0,0", 22)),

        // Line 26
        UnexpectedContentType(unexpectedContentTypeInTradingDate("30/13/2009", 26)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(conflictingTradingDate("A27", "903", "30/12/2009")("30/13/2009", "A26")),
          UnexpectedContentValue(unexpectedSettlementFee("0.69", 26)("0.20", "2494.00", "0.0079%")),
          UnexpectedContentValue(unexpectedSettlementFee("0.69", 26)("0.16", "2494.00", "0.0063%")),
          UnexpectedContentValue(unexpectedTradingFees("0.71", 26)("0.67", "2494.00", "0.0270%")),
          UnexpectedContentValue(unexpectedTradingFees("0.71", 26)("0.51", "2494.00", "0.0205%")),
        // -CONSEQUENTIAL

        // Line 27
        UnexpectedContentValue(unexpectedServiceTax("-0.80", 27)("0.80", "15.99", "5.0%")),

        // Line 29
        UnexpectedContentValue(unexpectedVolumeSummaryForHeterogeneousGroups("12934.00", 29)("-2494.00", 26, 28)),
        UnexpectedContentValue(unexpectedTotalSummaryForHeterogeneousGroups("12950.59", 29)("-2550.01", 26, 28)),

        // Line 33
        UnexpectedContentType(volumeSummaryNotCurrency("R$ 4.793,OO", 33)),
        UnexpectedContentType(volumeSummaryNotDouble("R$ 4.793,OO", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.00", 33)("4793.00", 31, 32)),
        UnexpectedContentType(settlementFeeSummaryNotCurrency("R$ l,32", 33)),
        UnexpectedContentType(settlementFeeSummaryNotDouble("R$ l,32", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 33)("1.32", 31, 32)),
        UnexpectedContentType(tradingFeesSummaryNotCurrency("R$ l,37", 33)),
        UnexpectedContentType(tradingFeesSummaryNotDouble("R$ l,37", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 33)("1.37", 31, 32)),
        UnexpectedContentType(brokerageSummaryNotCurrency("R$ 3l,98", 33)),
        UnexpectedContentType(brokerageSummaryNotDouble("R$ 3l,98", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedBrokerageSummary("0.00", 33)("31.98", 31, 32)),
        UnexpectedContentType(serviceTaxSummaryNotCurrency("R$ 1,6O", 33)),
        UnexpectedContentType(serviceTaxSummaryNotDouble("R$ 1,6O", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTaxSummary("0.00", 33)("1.60", 31, 32)),
        UnexpectedContentType(incomeTaxAtSourceSummaryNotCurrency("R$ O,OO", 33)),
        UnexpectedContentType(incomeTaxAtSourceSummaryNotDouble("R$ O,OO", 33)),
        UnexpectedContentType(totalSummaryNotCurrency("R$ 4.828,2E", 33)),
        UnexpectedContentType(totalSummaryNotDouble("R$ 4.828,2E", 33)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 33)("4829.26", 31, 32)),

        // Line 39
        UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(39)),
        /* NECESSARY */ RequiredValueMissing(noteNumberMissing(39)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedVolumeSummaryForHeterogeneousGroups("2726.00", 41)("7714.00", 39, 40)),
          UnexpectedContentValue(unexpectedTotalSummaryForHeterogeneousGroups("2688.10", 41)("7712.47", 39, 40)),
        // -CONSEQUENTIAL

        // Line 40
        UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(40)),
        // +NECESSARY 
          RequiredValueMissing(noteNumberMissing(40)),
          UnexpectedContentColor(invalidColorInVolume("0,0,0", 40)),
          UnexpectedContentColor(invalidColorInSettlementFee("0,0,0", 40)),
        // -NECESSARY 
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForBuyings("5200.29", 40)("5239.71")),

        // Line 44
        UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("2908.00", 44)("4290.00", 43, 43)),
        UnexpectedContentValue(unexpectedSettlementFeeSummary("0.80", 44)("0.86", 43, 43)),
        UnexpectedContentValue(unexpectedTradingFeesSummary("0.83", 44)("1.22", 43, 43)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.14", 44)("0.21", 43, 43)),
        UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("2889.58", 44)("4271.13", 43, 43)),

        // Line 46
        UnexpectedContentValue(unexpectedSettlementFee("1.18", 46)("0.86", "4290.00", "0.0200%")),

        // Line 48
        RequiredValueMissing(volumeSummaryMissing(48)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.00", 48)("7198.00", 46, 47)),
        RequiredValueMissing(settlementFeeSummaryMissing(48)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 48)("1.98", 46, 47)),
        RequiredValueMissing(tradingFeesSummaryMissing(48)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 48)("2.05", 46, 47)),
        RequiredValueMissing(brokerageSummaryMissing(48)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedBrokerageSummary("0.00", 48)("31.98", 46, 47)),
        RequiredValueMissing(serviceTaxSummaryMissing(48)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTaxSummary("0.00", 48)("1.60", 46, 47)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.00", 48)("0.35", 46, 47)),
        
        // Line 52
        RequiredValueMissing(totalSummaryMissing(52)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 52)("7160.71", 50, 51))
        
        // TODO Add Cell, Line, and, Worksheet errors once we fix the error accumulation strategy of those classes
      )

      val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

      errors should have size 201
      errors should contain theSameElementsAs expectedErrors
    }
    "be successfully built when given a" -{
      "'SettlementFee'" - {
        "that matches 'Volume' * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate'" - {
          "exactly, when 'OperationalMode' is" - {
            "'Normal'." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeExactMatch")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
            }
            "'DayTrade', be it" -{ 
              "highlighted." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HighlightedDTradeSettlementFee")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "or not." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoVisualCueDTradeSettlementFee")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
            }
          }
          "with a tolerance of:" - {
            "+'0.01', when 'OperationalMode' is" - {
              "'Normal'." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeWithinTolerance+")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "'DayTrade', be it" -{ 
                "highlighted." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HDTSettlementFeeWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "or not." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCDTSettlementFeeWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
            }
            "-'0.01', when 'OperationalMode' is" - {
              "'Normal'." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeWithinTolerance-")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "'DayTrade', be it" -{ 
                "highlighted." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HDTSettlementFeeWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "or not." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCDTSettlementFeeWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
            }
          }
        }
      }
      "'TradingFees'" - {
        "that matches 'Volume' * 'TradingFeesRate' at 'TradingDateTime'" - {
          "exactly, when 'TradingTime' falls within" - {
            "'Trading'." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("TradingFeesExactMatch")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
            }
            "'PreOpening', be it" -{ 
              "highlighted." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HighlightPreOpeningTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "containing a note." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NotePreOpeningTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "with no visual clue whatsoever." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoVisualCuePOpeningTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
            }
            "'ClosingCall', be it" -{ 
              "highlighted." ignore { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HighlightClosingCallTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "containing a note." ignore { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoteClosingCallTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "with no visual clue whatsoever." ignore { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoVisualCueCCallTradingFees")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
            }
          }
          "with a tolerance of:" - {
            "+'0.01', when 'TradingTime' falls within" - {
              "'Trading'." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("TradingFeesWithinTolerance+")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "'PreOpening', be it" -{ 
                "highlighted." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HPreOpenTradingFeesWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "containing a note." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NPreOpenTradingFeesWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "with no visual clue whatsoever." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCPOTradingFeesWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
              "'ClosingCall', be it" -{ 
                "highlighted." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HCCallTradingFeesWTolererance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "containing a note." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NCCTradingFeesWTolererance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "with no visual clue whatsoever." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCCCTradingFeesWTolerance+")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
            }
            "-'0.01', when 'TradingTime' falls within" - {
              "'Trading'." in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("TradingFeesWithinTolerance-")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
              }
              "'PreOpening', be it" -{ 
                "highlighted." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HPreOpenTradingFeesWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "containing a note." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NPreOpenTradingFeesWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "with no visual clue whatsoever." in { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCPOTradingFeesWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
              "'ClosingCall', be it" -{ 
                "highlighted." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HCCallTradingFeesWTolererance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "containing a note." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NCCTradingFeesWTolererance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
                "with no visual clue whatsoever." ignore { poiWorkbook =>
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCCCTradingFeesWTolerance-")).get

                  assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
                }
              }
            }
          }
        }
      }
      "'ServiceTax'" - {
        "that matches 'Brokerage' * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity'" - {
          "exactly." in { poiWorkbook =>
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("ServiceTaxExactMatch")).get

            assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
          }
          "with a tolerance of:" - {
            "+'0.01'." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("ServiceTaxWithinTolerance+")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
            }
            "-'0.01'." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("ServiceTaxWithinTolerance-")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).isValid)
            }
          }
        }
      }
    }
    "mark the 'Operation' as" - { 
      "'DayTrade' when its 'SettlementFee' is verified to be correct when using the 'DayTrade's rate" - {
        "for 'Operation's whose 'SettlementFee' is highlighted with an orange('252, 228, 214') background-color, be it" - { 
          "an exact match." in { poiWorkbook =>
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HighlightedDTradeSettlementFee")).get

            assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
          }
          "an approximate match with a tolerance of" - { 
            "+'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HDTSettlementFeeWTolerance+")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
            }
            "-'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HDTSettlementFeeWTolerance-")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
            }
          }
        }
        "after failing to do so using the 'Normal' rate for 'Operation's with no visual cue whatsoever, be it" - { 
          "an exact match." in { poiWorkbook =>
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoVisualCueDTradeSettlementFee")).get

            assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
          }
          "an approximate match with a tolerance of" - { 
            "+'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCDTSettlementFeeWTolerance+")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
            }
            "-'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCDTSettlementFeeWTolerance-")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isDayTrade)
            }
          }
        }
      }
      "carried out at market 'PreOpening' when its 'TradingFees' is verified to be correct when using the 'PreOpening's rate" - {
        "for 'Operation's whose 'TradingFees'" - { 
          "is highlighted with an orange('252, 228, 214') background-color, be it" - { 
            "an exact match." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HighlightPreOpeningTradingFees")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
            }
            "an approximate match with a tolerance of" - { 
              "+'0.01'" in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HPreOpenTradingFeesWTolerance+")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
              }
              "-'0.01'" in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("HPreOpenTradingFeesWTolerance-")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
              }
            }
          }
          "contains a note, be it" - { 
            "an exact match." in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NotePreOpeningTradingFees")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
            }
            "an approximate match with a tolerance of" - { 
              "+'0.01'" in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NPreOpenTradingFeesWTolerance+")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
              }
              "-'0.01'" in { poiWorkbook =>
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NPreOpenTradingFeesWTolerance-")).get

                assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
              }
            }
          }
        }
        "after failing to do so using the 'Trading' rate for 'Operation's with no visual cue whatsoever, be it" - { 
          "an exact match." in { poiWorkbook =>
            val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NoVisualCuePOpeningTradingFees")).get

            assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
          }
          "an approximate match with a tolerance of" - { 
            "+'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCPOTradingFeesWTolerance+")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
            }
            "-'0.01'" in { poiWorkbook =>
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NVCPOTradingFeesWTolerance-")).get

              assert(BrokerageNotesWorksheetReader.from(TEST_SHEET).operations.head.isCarriedOutAt(PRE_OPENING))
            }
          }
        }
      }
    }
    "turn every" - {
      "'Group' into a 'BrokerageNote' when all 'Lines' in the 'Group' have the same 'TradingDate' and 'BrokerageNote'." in { poiWorkbook =>
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSameTradingDate&Note")).get
        assume(TEST_SHEET.groups.size == 4)

        val brokerageNotes = BrokerageNotesWorksheetReader.from(TEST_SHEET).brokerageNotes

        brokerageNotes should have size 4

        forAll(brokerageNotes)(_ shouldBe a[BrokerageNote])
      }
      "non-'SummaryLine' into an 'Operation'." in { poiWorkbook =>
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSameTradingDate&Note")).get
        assume(TEST_SHEET.nonSummaryLines.size == 7)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 7

        forAll(operations)(_ shouldBe a[Operation])
      }
      "'SummaryLine' into a 'FinancialSummary'." in { poiWorkbook =>
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupsWithSummary")).get
        assume(TEST_SHEET.summaryLines.size == 2)

        val financialSummaries = BrokerageNotesWorksheetReader.from(TEST_SHEET).financialSummaries

        financialSummaries should have size 2

        forAll(financialSummaries)(_ shouldBe a[FinancialSummary])
      }
      "red non-'SummaryLine' into a 'BuyingOperation'." in { poiWorkbook =>
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("BuyingAndSellingOperations")).get
        assume(TEST_SHEET.redNonSummaryLines.size == 6)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 11

        forExactly(6, operations)(_ shouldBe a[BuyingOperation])
      }
      "blue non-'SummaryLine' into a 'SellingOperation'." in { poiWorkbook =>
        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("BuyingAndSellingOperations")).get
        assume(TEST_SHEET.blueNonSummaryLines.size == 5)

        val operations = BrokerageNotesWorksheetReader.from(TEST_SHEET).operations

        operations should have size 11

        forExactly(5, operations)(_ shouldBe a[SellingOperation])
      }
    }
    "generate a 'FinancialSummary', for 'Groups' of one 'Line', whose fields would replicate the corresponding ones from the one 'Line' in the 'Group'." in { poiWorkbook =>
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
        financialSummary.tradingFees should equal(operation.tradingFees)
        financialSummary.brokerage should equal(operation.brokerage)
        financialSummary.serviceTax should equal(operation.serviceTax)
        financialSummary.incomeTaxAtSource should equal(operation.incomeTaxAtSource)
        financialSummary.total should equal(operation.total)
      }
    }
  }

object BrokerageNotesWorksheetReaderTest:

  import org.scalatest.EitherValues.*
  import excel.poi.{Cell, Line}
  import BrokerageNotesWorksheetReader.ErrorsOr

  private val TEST_SPREADSHEET = "BrokerageNotes.xlsx"

  private val RED = "255,0,0"
  private val BLUE = "91,155,213"

  extension (errorsOrBrokerageNotesWorksheetReader: ErrorsOr[BrokerageNotesWorksheetReader])

    private def error: BrokerageNotesWorksheetReader.Error =
      errors.head
      
    private def errors: Seq[BrokerageNotesWorksheetReader.Error] =
      errorsOrBrokerageNotesWorksheetReader.toEither.left.value.toNonEmptyList.toList
        
    private def brokerageNotes: Seq[BrokerageNote] =
      errorsOrBrokerageNotesWorksheetReader.toEither.value.brokerageNotes

    private def operations: Seq[Operation] =
      brokerageNotes.flatMap(_.operations)

    private def financialSummaries: Seq[FinancialSummary] =
      brokerageNotes.map(_.financialSummary)

  extension (errorsOrWorksheet: ErrorsOr[Worksheet])

    private def get: Worksheet =
      errorsOrWorksheet.toEither.value

  extension (worksheet: Worksheet)

    private def nonSummaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(!_.isSummary))

    private def summaryLines: Seq[Line] =
      worksheet.groups.flatMap(_.filter(isSummary))

    private def redNonSummaryLines: Seq[Line] =
      nonSummaryLines.filter(allNonEmptyCellsRed)

    private def blueNonSummaryLines: Seq[Line] =
      nonSummaryLines.filter(allNonEmptyCellsBlue)

  extension (line: Line)

    private def isSummary: Boolean = line.nonEmptyCells.forall(_.isFormula)

    private def allNonEmptyCellsRed: Boolean = line.nonEmptyCells.forall(redFont)

    private def allNonEmptyCellsBlue: Boolean = line.nonEmptyCells.forall(blueFont)

  extension (cell: Cell)

    private def redFont: Boolean = cell.fontColor == RED

    private def blueFont: Boolean = cell.fontColor == BLUE

object BrokerageNotesWorksheetTestMessages:
  private val RED = "255,0,0"
  private val BLUE = "91,155,213"

  import BrokerageNotesWorksheetMessages.*

  def tradingDateMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("TradingDate", operationIndex)
  def noteNumberMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("NoteNumber", operationIndex)
  def tickerMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Ticker", operationIndex)
  def qtyMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Qty", operationIndex)
  def priceMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Price", operationIndex)
  def volumeMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Volume", operationIndex)
  def settlementFeeMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("SettlementFee", operationIndex)
  def tradingFeesMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("TradingFees", operationIndex)
  def brokerageMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Brokerage", operationIndex)
  def serviceTaxMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("ServiceTax", operationIndex)
  def totalMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("Total", operationIndex)
  def volumeSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("VolumeSummary", operationIndex)
  def settlementFeeSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("SettlementFeeSummary", operationIndex)
  def tradingFeesSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("TradingFeesSummary", operationIndex)
  def brokerageSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("BrokerageSummary", operationIndex)
  def serviceTaxSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("ServiceTaxSummary", operationIndex)
  def totalSummaryMissing(operationIndex: Int)(using worksheetName: String): String =
    attributeMissing("TotalSummary", operationIndex)

  def unexpectedContentTypeInTradingDate(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingDate", operationIndex)("a date")
  def unexpectedContentTypeInNoteNumber(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "NoteNumber", operationIndex)("an integer number")
  def unexpectedContentTypeInQty(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Qty", operationIndex)("an integer number")
  def priceNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Price", operationIndex)("a currency")
  def priceNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Price", operationIndex)("a double")
  def volumeNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Volume", operationIndex)("a currency")
  def volumeNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Volume", operationIndex)("a double")
  def settlementFeeNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFee", operationIndex)("a currency")
  def settlementFeeNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFee", operationIndex)("a double")
  def tradingFeesNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFees", operationIndex)("a currency")
  def tradingFeesNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFees", operationIndex)("a double")
  def brokerageNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Brokerage", operationIndex)("a currency")
  def brokerageNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Brokerage", operationIndex)("a double")
  def serviceTaxNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTax", operationIndex)("a currency")
  def serviceTaxNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTax", operationIndex)("a double")
  def incomeTaxAtSourceNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSource", operationIndex)("a currency")
  def incomeTaxAtSourceNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSource", operationIndex)("a double")
  def totalNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Total", operationIndex)("a currency")
  def totalNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "Total", operationIndex)("a double")
  def volumeSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "VolumeSummary", operationIndex)("a currency")
  def volumeSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "VolumeSummary", operationIndex)("a double")
  def settlementFeeSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFeeSummary", operationIndex)("a currency")
  def settlementFeeSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFeeSummary", operationIndex)("a double")
  def tradingFeesSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFeesSummary", operationIndex)("a currency")
  def tradingFeesSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFeesSummary", operationIndex)("a double")
  def brokerageSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "BrokerageSummary", operationIndex)("a currency")
  def brokerageSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "BrokerageSummary", operationIndex)("a double")
  def serviceTaxSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTaxSummary", operationIndex)("a currency")
  def serviceTaxSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTaxSummary", operationIndex)("a double")
  def incomeTaxAtSourceSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSourceSummary", operationIndex)("a currency")
  def incomeTaxAtSourceSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSourceSummary", operationIndex)("a double")
  def totalSummaryNotCurrency(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TotalSummary", operationIndex)("a currency")
  def totalSummaryNotDouble(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedContentType(attributeValue, "TotalSummary", operationIndex)("a double")

  def invalidColorInTradingDate(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "TradingDate", operationIndex)
  def invalidColorInNoteNumber(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "NoteNumber", operationIndex)
  def invalidColorInTicker(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Ticker", operationIndex)
  def invalidColorInQty(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Qty", operationIndex)
  def invalidColorInPrice(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Price", operationIndex)
  def invalidColorInVolume(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Volume", operationIndex)
  def invalidColorInSettlementFee(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "SettlementFee", operationIndex)
  def invalidColorInTradingFees(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "TradingFees", operationIndex)
  def invalidColorInBrokerage(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Brokerage", operationIndex)
  def invalidColorInServiceTax(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "ServiceTax", operationIndex)
  def invalidColorInIncomeTaxAtSource(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "IncomeTaxAtSource", operationIndex)
  def invalidColorInTotal(attributeColor: String, operationIndex: Int)(using worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Total", operationIndex)

  def unexpectedColorForSelling(attributeName: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedAttributeColor(s"red($RED)", attributeName, operationIndex)("Buying", "Selling")
  def unexpectedColorForSellingInTradingDate(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("TradingDate", operationIndex)
  def unexpectedColorForSellingInNoteNumber(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("NoteNumber", operationIndex)
  def unexpectedColorForSellingInTicker(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Ticker", operationIndex)
  def unexpectedColorForSellingInQty(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Qty", operationIndex)
  def unexpectedColorForSellingInPrice(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Price", operationIndex)
  def unexpectedColorForSellingInVolume(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Volume", operationIndex)
  def unexpectedColorForSellingInSettlementFee(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("SettlementFee", operationIndex)
  def unexpectedColorForSellingInTradingFees(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("TradingFees", operationIndex)
  def unexpectedColorForSellingInBrokerage(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Brokerage", operationIndex)
  def unexpectedColorForSellingInServiceTax(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("ServiceTax", operationIndex)
  def unexpectedColorForSellingInIncomeTaxAtSource(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("IncomeTaxAtSource", operationIndex)
  def unexpectedColorForSellingInTotal(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForSelling("Total", operationIndex)
  
  def unexpectedColorForBuying(attributeName: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedAttributeColor(s"blue($BLUE)", attributeName, operationIndex)("Selling", "Buying")
  def unexpectedColorForBuyingInTradingDate(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("TradingDate", operationIndex)
  def unexpectedColorForBuyingInNoteNumber(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("NoteNumber", operationIndex)
  def unexpectedColorForBuyingInTicker(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Ticker", operationIndex)
  def unexpectedColorForBuyingInQty(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Qty", operationIndex)
  def unexpectedColorForBuyingInPrice(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Price", operationIndex)
  def unexpectedColorForBuyingInVolume(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Volume", operationIndex)
  def unexpectedColorForBuyingInSettlementFee(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("SettlementFee", operationIndex)
  def unexpectedColorForBuyingInTradingFees(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("TradingFees", operationIndex)
  def unexpectedColorForBuyingInBrokerage(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Brokerage", operationIndex)
  def unexpectedColorForBuyingInServiceTax(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("ServiceTax", operationIndex)
  def unexpectedColorForBuyingInIncomeTaxAtSource(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("IncomeTaxAtSource", operationIndex)
  def unexpectedColorForBuyingInTotal(operationIndex: Int)(using worksheetName: String): String =
    unexpectedColorForBuying("Total", operationIndex)

  def unexpectedNegativeNoteNumber(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedNegativeAttribute("NoteNumber", attributeValue, operationIndex)
  def unexpectedNegativeQty(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedNegativeAttribute("Qty", attributeValue, operationIndex)
  def unexpectedNegativePrice(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedNegativeAttribute("Price", attributeValue, operationIndex)
  def unexpectedNegativeBrokerage(attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedNegativeAttribute("Brokerage", attributeValue, operationIndex)

  private def calculatedAttributeSummaryFormulaDescription(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"
  
  private def settlementFeeSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("SettlementFee", 'G', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedSettlementFeeSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "SettlementFeeSummary", s"G$summaryIndex"
    )(
      expectedValue, settlementFeeSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def tradingFeesSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("TradingFees", 'H', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTradingFeesSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "TradingFeesSummary", s"H$summaryIndex"
    )(
      expectedValue, tradingFeesSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def brokerageSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("Brokerage", 'I', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedBrokerageSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "BrokerageSummary", s"I$summaryIndex"
    )(
      expectedValue, brokerageSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def serviceTaxSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("ServiceTax", 'J', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedServiceTaxSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "ServiceTaxSummary", s"J$summaryIndex"
    )(
      expectedValue, serviceTaxSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def incomeTaxAtSourceSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("IncomeTaxAtSource", 'K', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedIncomeTaxAtSourceSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "IncomeTaxAtSourceSummary", s"K$summaryIndex"
    )(
      expectedValue, incomeTaxAtSourceSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def unexpectedVolumeSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, formulaDescription: String)(using worksheetName: String) =
    unexpectedOperationTypeAwareAttributeSummary(
      actualValue, "VolumeSummary", summaryIndex
    )(
      expectedValue, 'F', formulaDescription
    )

  private def volumeSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) = 
    operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups("Volume", 'F', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedVolumeSummaryForHomogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedVolumeSummary(
      actualValue, summaryIndex
    )(
      expectedValue, volumeSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def volumeSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups("Volume", 'F', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedVolumeSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedVolumeSummary(
      actualValue, summaryIndex
    )(
      expectedValue, volumeSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def unexpectedTotalSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, formulaDescription: String)(using worksheetName: String) =
    unexpectedOperationTypeAwareAttributeSummary(
      actualValue, "TotalSummary", summaryIndex
    )(
      expectedValue, 'L', formulaDescription
    )

  private def totalSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) = 
    operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups("Total", 'L', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTotalSummaryForHomogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )
  
  private def totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups("Total", 'L', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTotalSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(using worksheetName: String): String =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )