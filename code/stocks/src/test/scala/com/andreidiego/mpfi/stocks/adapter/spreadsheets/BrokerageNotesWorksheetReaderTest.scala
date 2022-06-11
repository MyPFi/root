package com.andreidiego.mpfi.stocks.adapter.spreadsheets

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
  import BrokerageNotesWorksheetReader.BrokerageNoteReaderError.{RequiredValueMissing, UnexpectedContentColor, UnexpectedContentType, UnexpectedContentValue}
  import BrokerageNotesWorksheetMessages.*
  import BrokerageNotesWorksheetTestMessages.*
  import BrokerageNotesWorksheetReaderTest.*

  override protected type FixtureParam = XSSFWorkbook

  private var testWorkbook: XSSFWorkbook = _
  
  override protected def beforeAll(): Unit = 
    testWorkbook = XSSFWorkbookFactory.createWorkbook(
      OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
    )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest(testWorkbook))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  "A BrokerageNotesWorksheetReader should" - {
    "be built from a Worksheet" in { poiWorkbook ⇒
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
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(tradingDateMissing(2)(TEST_SHEET_NAME))
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(unexpectedContentTypeInTradingDate("-39757", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers and the  '/' symbol)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(unexpectedContentTypeInTradingDate("O5/11/2008", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "when containing an invalid date." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateInvalidDate"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(unexpectedContentTypeInTradingDate("05/13/2008", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TradingDateBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTradingDate("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingDateRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTradingDate(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingDateBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTradingDate(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'NoteNumber'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "NoteNumberMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(noteNumberMissing(2)(TEST_SHEET_NAME))
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "NoteNumberNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativeNoteNumber("-1662", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "NoteNumberExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(unexpectedContentTypeInNoteNumber("I662", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "NoteNumberBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInNoteNumber("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "NoteNumberRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInNoteNumber(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "NoteNumberBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInNoteNumber(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Ticker'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TickerMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(tickerMissing(2)(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TickerBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTicker("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TickerRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTicker(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TickerBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTicker(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Qty'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "QtyMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(qtyMissing(2)(TEST_SHEET_NAME))
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "QtyNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativeQty("-100", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "QtyExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInQty("l00", 2)(TEST_SHEET_NAME)))
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "QtyBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInQty("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "QtyRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInQty(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "QtyBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInQty(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Price'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "PriceMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[RequiredValueMissing]),
                      'message(priceMissing(2)(TEST_SHEET_NAME))
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "PriceNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedNegativePrice("-15.34", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "PriceExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(priceNotCurrency("R$ l5,34", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(priceNotDouble("R$ l5,34", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "PriceBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInPrice("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "PriceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInPrice(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "PriceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInPrice(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Volume'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "VolumeMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(volumeMissing(2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "VolumeExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(volumeNotCurrency("R$ l534,00", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(volumeNotDouble("R$ l534,00", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than 'Qty' * 'Price'." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "VolumeDoesNotMatchQtyTimesPrice"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolume("7030.01", 2)("7030.00", "200", "35.15")(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "VolumeBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInVolume("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "VolumeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInVolume(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "VolumeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInVolume(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'SettlementFee'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "SettlementFeeMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(settlementFeeMissing(2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "SettlementFeeExtraneousChars"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(settlementFeeNotCurrency("R$ O,42", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(settlementFeeNotDouble("R$ O,42", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than 'Volume' * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' when 'OperationalMode' is" - {
                    "'Normal'." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "SettlementFeeNotVolumeTimesRate"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedSettlementFee("2.76", 2)("2.75", "11000.00", "0.0250%")(TEST_SHEET_NAME))
                      )
                    }
                    "'DayTrade'." ignore { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "SettlementFeeNotVolumeTimesRate"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedSettlementFee("2.76", 2)("2.75", "11000.00", "0.0250%")(TEST_SHEET_NAME))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "SettlementFeeBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInSettlementFee("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "SettlementFeeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInSettlementFee(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "SettlementFeeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInSettlementFee(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'TradingFees'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingFeesMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(tradingFeesMissing(2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingFeesExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(tradingFeesNotCurrency("R$ O,11", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(tradingFeesNotDouble("R$ O,11", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than 'Volume' * 'TradingFeesRate' at 'TradingDateTime' when 'TradingTime' falls within" - {
                    "'PreOpening'." ignore { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "SettlementFeeNotVolumeTimesRate"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTradingFees("2.76", 2)("2.75", "11000.00", "0.0250%")(TEST_SHEET_NAME))
                      )
                    }
                    "'Trading'." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "InvalidTradingFees"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTradingFees("0.56", 2)("0.55", "11000.00", "0.0050%")(TEST_SHEET_NAME))
                      )
                    }
                    "'ClosingCall'." ignore { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "SettlementFeeNotVolumeTimesRate"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTradingFees("0.56", 2)("0.55", "11000.00", "0.0050%")(TEST_SHEET_NAME))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TradingFeesBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTradingFees("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingFeesRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTradingFees(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingFeesBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTradingFees(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Brokerage'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "BrokerageMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(brokerageMissing(2)(TEST_SHEET_NAME)))
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "BrokerageNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentValue(unexpectedNegativeBrokerage("-15.99", 2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "BrokerageExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(brokerageNotCurrency("R$ l5,99", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(brokerageNotDouble("R$ l5,99", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "BrokerageBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInBrokerage("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "BrokerageRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInBrokerage(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "BrokerageBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInBrokerage(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'ServiceTax'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(serviceTaxMissing(2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(serviceTaxNotCurrency("R$ O,8O", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(serviceTaxNotDouble("R$ O,8O", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than 'Brokerage' * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity'." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "InvalidServiceTax"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedServiceTax("0.12", 2)("0.13", "1.99", "6.5%")(TEST_SHEET_NAME))
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "ServiceTaxBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInServiceTax("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "ServiceTaxRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInServiceTax(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "ServiceTaxBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInServiceTax(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'IncomeTaxAtSource'" - {
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "IncomeTaxAtSourceExtraneousChar"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(incomeTaxAtSourceNotCurrency("R$ O,OO", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(incomeTaxAtSourceNotDouble("R$ O,OO", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than" - {
                    "for 'SellingOperations', (('Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax') - ('AverageStockPrice' for the 'Ticker' * 'Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' when 'OperationalMode' is 'Normal'" in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "InvalidIncomeTaxAtSource"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedIncomeTaxAtSourceForSellings("0.19", 2)("0.09", "1803.47", "0.0050%")(TEST_SHEET_NAME))
                      )
                    }
                    "for 'BuyingOperations, if not empty, zero." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "NonZeroIncomeTaxAtSourceBuying"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedIncomeTaxAtSourceForBuyings("0.01", 2)(TEST_SHEET_NAME))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "IncomeTaxAtSourceBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInIncomeTaxAtSource("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "IncomeTaxAtSourceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInIncomeTaxAtSource(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "IncomeTaxAtSourceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInIncomeTaxAtSource(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
                "'Total'" - {
                  "when missing." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TotalMissing"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(RequiredValueMissing(totalMissing(2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TotalExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    actualErrors should contain allOf(
                      UnexpectedContentType(totalNotCurrency("R$ l551,32", 2)(TEST_SHEET_NAME)),
                      UnexpectedContentType(totalNotDouble("R$ l551,32", 2)(TEST_SHEET_NAME))
                    )
                  }
                  "if different than" - {
                    "for 'SellingOperations', 'Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax'." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "InvalidTotalForSelling"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTotalForSellings("7010.81", 2)("7010.78")(TEST_SHEET_NAME))
                      )
                    }
                    "for 'BuyingOperations', 'Volume' + 'SettlementFee' + 'TradingFees' + 'Brokerage' + 'ServiceTax'." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "InvalidTotalForBuying"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(unexpectedTotalForBuyings("11005.45", 2)("11005.42")(TEST_SHEET_NAME))
                      )
                    }
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TotalBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(invalidColorInTotal("0,0,0", 2)(TEST_SHEET_NAME))
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TotalRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForSellingInTotal(2)(TEST_SHEET_NAME))
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TotalBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(unexpectedColorForBuyingInTotal(2)(TEST_SHEET_NAME))
                        )
                      }
                    }
                  }
                }
              }
              "the impossibility of determining its type when it has exactly half of it's" - { 
                "'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  val TEST_SHEET_NAME = "NoEmptyAttributeHalfOfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2)(TEST_SHEET_NAME))
                  )
                }
                "non-empty 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  val TEST_SHEET_NAME = "NonEmptyAttribsHalfOfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2)(TEST_SHEET_NAME))
                  )
                }
                "valid-colored 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  val TEST_SHEET_NAME = "ValidColoredAttribHalfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2)(TEST_SHEET_NAME))
                  )
                }
                "non-empty valid-colored 'Attribute's from each of the two valid colors." in { poiWorkbook =>
                  val TEST_SHEET_NAME = "NEValidColorAttrHalfEachColor"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentColor]),
                    'message(impossibleToDetermineMostLikelyOperationType(2)(TEST_SHEET_NAME))
                  )
                }
              }
            }
            "Not harmonic with each other, that is, contain different" - {
              "'TradingDate's." in { poiWorkbook ⇒
                val TEST_SHEET_NAME = "GroupWithDifferentTradingDates"
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  // TODO Replace the 'NoteNumber' below by the 'GroupIndex' after it has been added to the 'Group' class
                  'message(conflictingTradingDate("A3", "1662", "06/11/2008")("05/11/2008", "A2")(TEST_SHEET_NAME))
                )
              }
              "'NoteNumbers's." in { poiWorkbook ⇒
                val TEST_SHEET_NAME = "GroupWithDifferentNoteNumbers"
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  'message(conflictingNoteNumber("B3", "1663", "1663")("1662", "B2")(TEST_SHEET_NAME))
                )
              }
            }
          }
          "having more than one 'Operation'" - { 
            "don't have a 'Summary (last 'Operation' will be taken as the 'Summary')'." in { poiWorkbook ⇒
              val TEST_SHEET_NAME = "MultiLineGroupWithNoSummary"
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

              val expectedErrors = Seq(
                UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("11600.00", 3)("11000.00", 2, 2)(TEST_SHEET_NAME)),
                UnexpectedContentValue(unexpectedSettlementFeeSummary("2.90", 3)("2.75", 2, 2)(TEST_SHEET_NAME)),
                UnexpectedContentValue(unexpectedTradingFeesSummary("0.58", 3)("0.55", 2, 2)(TEST_SHEET_NAME)),
                UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("11605.60", 3)("11005.42", 2, 2)(TEST_SHEET_NAME))
              )

              val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

              actualErrors should have size 4
              actualErrors should contain theSameElementsAs expectedErrors
            }
            "have an invalid 'Summary', in which" - {
              "'VolumeSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "VolumeSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(volumeSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "VolumeSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(volumeSummaryNotCurrency("-R$ 9.322,OO", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(volumeSummaryNotDouble("-R$ 9.322,OO", 5)(TEST_SHEET_NAME))
                  )
                }
                "is different than the sum of the 'Volume's of all" - {
                  "'Operation's, for homogenoues groups (comprised exclusively of 'Operation's of the same type)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "InvalidVolumeSummaryHomogGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolumeSummaryForHomogeneousGroups("-2110.00", 4)("16810.00", 2, 3)(TEST_SHEET_NAME))
                    )
                  }
                  "'SellingOperation's minus the sum of the 'Volume's of all 'BuyingOperation's for mixed groups (comprised of 'Operation's from different types)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "InvalidVolumeSummaryMixedGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedVolumeSummaryForHeterogeneousGroups("16810.00", 4)("-2110.00", 2, 3)(TEST_SHEET_NAME))
                    )
                  }
                }
              }
              "'SettlementFeeSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "SettlementFeeSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(settlementFeeSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "SettlementFeeSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(settlementFeeSummaryNotCurrency("R$ 2,S6", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(settlementFeeSummaryNotDouble("R$ 2,S6", 5)(TEST_SHEET_NAME))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "InvalidSettlementFeeSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedSettlementFeeSummary("5.68", 4)("5.65", 2, 3)(TEST_SHEET_NAME))
                  )
                }
              }
              "'TradingFeesSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "TradingFeesSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(tradingFeesSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "TradingFeesSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(tradingFeesSummaryNotCurrency("R$ O,65", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(tradingFeesSummaryNotDouble("R$ O,65", 5)(TEST_SHEET_NAME))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "InvalidTradingFeesSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedTradingFeesSummary("1.10", 4)("1.13", 2, 3)(TEST_SHEET_NAME))
                  )
                }
              }
              "'BrokerageSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "BrokerageSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(brokerageSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "BrokerageSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(brokerageSummaryNotCurrency("R$ 4T,97", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(brokerageSummaryNotDouble("R$ 4T,97", 5)(TEST_SHEET_NAME))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "InvalidBrokerageSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedBrokerageSummary("3.95", 4)("3.98", 2, 3)(TEST_SHEET_NAME))
                  )
                }
              }
              "'ServiceTaxSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "ServiceTaxSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(serviceTaxSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "ServiceTaxSummaryExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(serviceTaxSummaryNotCurrency("R$ 2,4O", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(serviceTaxSummaryNotDouble("R$ 2,4O", 5)(TEST_SHEET_NAME))
                  )
                }
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "InvalidServiceTaxSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedServiceTaxSummary("0.29", 4)("0.26", 2, 3)(TEST_SHEET_NAME))
                  )
                }
              }
              "'IncomeTaxAtSourceSummary'" - {
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "IncomeTaxAtSourceSummExtrChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(incomeTaxAtSourceSummaryNotCurrency("R$ O,OO", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(incomeTaxAtSourceSummaryNotDouble("R$ O,OO", 5)(TEST_SHEET_NAME))
                  )
                }
                // TODO There are a few of special cases when it comes to IncomeTaxAtSourceSummary: It could be either empty or zero for Buyings and, empty, zero, or have a greater than zero value for Sellings
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "InvalidIncomeTaxAtSourceSummary"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(unexpectedIncomeTaxAtSourceSummary("0.05", 5)("0.08", 2, 4)(TEST_SHEET_NAME))
                  )
                }
              }
              "'TotalSummary'" - {
                "is missing." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "TotalSummaryMissing"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[RequiredValueMissing]),
                    'message(totalSummaryMissing(5)(TEST_SHEET_NAME))
                  )
                }
                "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                  val TEST_SHEET_NAME = "TotalSummaryExtraneousChars"
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                  val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                  actualErrors should contain allOf(
                    UnexpectedContentType(totalSummaryNotCurrency("-R$ 9.37S,S9", 5)(TEST_SHEET_NAME)),
                    UnexpectedContentType(totalSummaryNotDouble("-R$ 9.37S,S9", 5)(TEST_SHEET_NAME))
                  )
                }
                "is different than the sum of the 'Total's of all" - {
                  "'Operation's, for homogenoues groups (comprised exclusively of 'Operation's of the same type)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "InvalidTotalSummaryHomogGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedTotalSummaryForHomogeneousGroups("-2110.69", 4)("16820.69", 2, 3)(TEST_SHEET_NAME))
                    )
                  }
                  "'SellingOperation's minus the sum of the 'Total's of all 'BuyingOperation's, for mixed groups (comprised of 'Operation's from different types)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "InvalidTotalSummaryMixedGroups"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(unexpectedTotalSummaryForHeterogeneousGroups("16820.69", 4)("-2110.69", 2, 3)(TEST_SHEET_NAME))
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    "accumulate errors" in { poiWorkbook ⇒
      val TEST_SHEET_NAME = "MultipleErrors"
      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

      val expectedErrors = Seq(
        // Line 2
        RequiredValueMissing(tradingDateMissing(2)(TEST_SHEET_NAME)),
        UnexpectedContentType(unexpectedContentTypeInNoteNumber("II62", 2)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedNegativeQty("-100", 2)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInVolume(2)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedServiceTax("1.10", 2)("0.80", "15.99", "5.0%")(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTotalForBuyings("-1517.14", 2)("-1517.44")(TEST_SHEET_NAME)),

        // Line 4
        UnexpectedContentType(unexpectedContentTypeInTradingDate("39757", 4)(TEST_SHEET_NAME)),
        RequiredValueMissing(noteNumberMissing(4)(TEST_SHEET_NAME)),
        UnexpectedContentType(unexpectedContentTypeInQty("S00", 4)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentType(volumeNotCurrency("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(volumeNotDouble("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeNotCurrency("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeNotDouble("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 4)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 4)("16.79")(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForBuyingInBrokerage(4)(TEST_SHEET_NAME)),

        // Line 6
        UnexpectedContentType(unexpectedContentTypeInTradingDate("0S/11/2008", 6)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedNegativeNoteNumber("-1662", 6)(TEST_SHEET_NAME)),
        RequiredValueMissing(tickerMissing(6)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInQty(6)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTradingFees("0.49", 6)("0.19", "2750.00", "0.0070%")(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInTradingFees(6)(TEST_SHEET_NAME)),
        UnexpectedContentType(brokerageNotCurrency("R$ l5,99", 6)(TEST_SHEET_NAME)),
        UnexpectedContentType(brokerageNotDouble("R$ l5,99", 6)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentType(totalNotCurrency("#VALUE!", 6)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 6)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 6)("2751.25")(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL
        UnexpectedContentType(serviceTaxNotCurrency("R$ O,8O", 6)(TEST_SHEET_NAME)),
        UnexpectedContentType(serviceTaxNotDouble("R$ O,8O", 6)(TEST_SHEET_NAME)),

        // Line 8
        UnexpectedContentType(unexpectedContentTypeInTradingDate("-39757", 8)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInTicker(8)(TEST_SHEET_NAME)),
        RequiredValueMissing(qtyMissing(8)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedNegativeBrokerage("-15.99", 8)(TEST_SHEET_NAME)),
        // FIXME Not true and doesn't make any sense - Investigate
        UnexpectedContentColor(unexpectedColorForBuyingInBrokerage(8)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInTotal(8)(TEST_SHEET_NAME)),

        // Line 10
        UnexpectedContentColor(unexpectedColorForBuyingInNoteNumber(10)(TEST_SHEET_NAME)),
        RequiredValueMissing(priceMissing(10)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInSettlementFee(10)(TEST_SHEET_NAME)),
        UnexpectedContentType(tradingFeesNotCurrency("R$ O,11", 10)(TEST_SHEET_NAME)),
        UnexpectedContentType(tradingFeesNotDouble("R$ O,11", 10)(TEST_SHEET_NAME)),
        // TODO The following is not exactly CONSEQUENTIAL since it's not a formula. It is going to give reason to a Warning when warnings are implemented.
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForBuyings("16.90", 10)("16.79")(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForBuyings("0.01", 10)(TEST_SHEET_NAME)),

        // Line 11
        UnexpectedContentValue(conflictingTradingDate("A11", "1345", "05/11/2008")("06/11/2008", "A10")(TEST_SHEET_NAME)),
        UnexpectedContentValue(conflictingNoteNumber("B11", "1345", "1345")("1344", "B10")(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInPrice(11)(TEST_SHEET_NAME)),
        RequiredValueMissing(volumeMissing(11)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolume("0.00", 11)("769.00", "100", "7.69")(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInServiceTax(11)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForBuyingInIncomeTaxAtSource(11)(TEST_SHEET_NAME)),

        // Line 12
        UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.30", 12)("0.00", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedSettlementFeeSummary("0.30", 12)("0.00", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTradingFeesSummary("0.30", 12)("0.00", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedBrokerageSummary("32.28", 12)("31.98", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedServiceTaxSummary("1.90", 12)("1.60", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.31", 12)("0.01", 10, 11)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("33.99", 12)("33.69", 10, 11)(TEST_SHEET_NAME)),

        // Line 14
        UnexpectedContentColor(unexpectedColorForBuyingInTradingDate(14)(TEST_SHEET_NAME)),
        UnexpectedContentType(priceNotCurrency("R$ l5,34", 14)(TEST_SHEET_NAME)),
        UnexpectedContentType(priceNotDouble("R$ l5,34", 14)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentType(volumeNotCurrency("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentType(volumeNotDouble("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 14)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalForBuyings("0.00", 14)("16.79")(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL
        RequiredValueMissing(settlementFeeMissing(14)(TEST_SHEET_NAME)),

        // Line 16
        UnexpectedContentColor(unexpectedColorForSellingInTradingDate(16)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInQty(16)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedNegativePrice("-31.5", 16)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedVolume("3150.00", 16)("-3150.00", "100", "-31.50")(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedSettlementFee("1.17", 16)("0.87", "3150.00", "0.0275%")(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInSettlementFee(16)(TEST_SHEET_NAME)),
        RequiredValueMissing(tradingFeesMissing(16)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTradingFees("0.00", 16)("0.22", "3150.00", "0.0070%")(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTotalForSellings("3132.34", 16)("3132.04")(TEST_SHEET_NAME)),

        // Line 17
        UnexpectedContentColor(unexpectedColorForSellingInNoteNumber(17)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInPrice(17)(TEST_SHEET_NAME)),
        UnexpectedContentType(settlementFeeNotDouble("R$ O,87", 17)(TEST_SHEET_NAME)),
        UnexpectedContentType(settlementFeeNotCurrency("R$ O,87", 17)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedSettlementFee("0.00", 17)("0.87", "3150.00", "0.0275%")(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotCurrency("#VALUE!", 17)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalNotDouble("#VALUE!", 17)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalForSellings("0.00", 17)("3149.78")(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeSummaryNotCurrency("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeSummaryNotDouble("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 20)("2.60", 16, 19)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalSummaryNotCurrency("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentType(totalSummaryNotDouble("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 20)("3132.34", 16, 19)(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForSellingInTradingFees(17)(TEST_SHEET_NAME)),
        RequiredValueMissing(brokerageMissing(17)(TEST_SHEET_NAME)),
        UnexpectedContentType(incomeTaxAtSourceNotCurrency("R$ O,OO", 17)(TEST_SHEET_NAME)),
        UnexpectedContentType(incomeTaxAtSourceNotDouble("R$ O,OO", 17)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInTotal(17)(TEST_SHEET_NAME)),

        // Line 18
        UnexpectedContentColor(unexpectedColorForSellingInTicker(18)(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInVolume(18)(TEST_SHEET_NAME)),
        UnexpectedContentType(volumeNotCurrency("R$ 1770,OO", 18)(TEST_SHEET_NAME)),
        UnexpectedContentType(volumeNotDouble("R$ 1770,OO", 18)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedVolume("0.00", 18)("1770.00", "100", "17.70")(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeNotDouble("#VALUE!", 18)(TEST_SHEET_NAME)),
          UnexpectedContentType(settlementFeeNotCurrency("#VALUE!", 18)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotCurrency("#VALUE!", 18)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesNotDouble("#VALUE!", 18)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesSummaryNotCurrency("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentType(tradingFeesSummaryNotDouble("#VALUE!", 20)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 20)("0.59", 16, 19)(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL
        UnexpectedContentColor(unexpectedColorForSellingInBrokerage(18)(TEST_SHEET_NAME)),
        RequiredValueMissing(serviceTaxMissing(18)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTax("0.00", 18)("0.80", "15.99", "5.0%")(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForSellings("-0.50", 18)("-0.00", "-15.99", "0.0050%")(TEST_SHEET_NAME)),
        UnexpectedContentType(totalNotCurrency("R$ l.753,43", 18)(TEST_SHEET_NAME)),
        UnexpectedContentType(totalNotDouble("R$ l.753,43", 18)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForSellings("0.00", 18)("-15.99")(TEST_SHEET_NAME)),

        // Line 19
        UnexpectedContentColor(unexpectedColorForSellingInServiceTax(19)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceForSellings("0.05", 19)("0.26", "5201.41", "0.0050%")(TEST_SHEET_NAME)),
        UnexpectedContentColor(unexpectedColorForSellingInIncomeTaxAtSource(19)(TEST_SHEET_NAME)),
        RequiredValueMissing(totalMissing(19)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForSellings("0.00", 19)("5201.41")(TEST_SHEET_NAME)),

        // Line 22
        UnexpectedContentColor(invalidColorInTradingDate("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInNoteNumber("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInTicker("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInQty("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInPrice("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInVolume("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInSettlementFee("0,0,0", 22)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInTradingFees("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInBrokerage("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInServiceTax("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInIncomeTaxAtSource("0,0,0", 22)(TEST_SHEET_NAME)),
        UnexpectedContentColor(invalidColorInTotal("0,0,0", 22)(TEST_SHEET_NAME)),

        // Line 26
        UnexpectedContentType(unexpectedContentTypeInTradingDate("05/13/2008", 26)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(conflictingTradingDate("A27", "903", "25/02/2009")("05/13/2008", "A26")(TEST_SHEET_NAME)),

        // Line 27
        UnexpectedContentValue(unexpectedServiceTax("-0.80", 27)("0.80", "15.99", "5.0%")(TEST_SHEET_NAME)),

        // Line 29
        UnexpectedContentValue(unexpectedVolumeSummaryForHeterogeneousGroups("12934.00", 29)("-2494.00", 26, 28)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTotalSummaryForHeterogeneousGroups("12950.05", 29)("-2547.23", 26, 28)(TEST_SHEET_NAME)),

        // Line 33
        UnexpectedContentType(volumeSummaryNotCurrency("R$ 4.793,OO", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(volumeSummaryNotDouble("R$ 4.793,OO", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.00", 33)("4793.00", 31, 32)(TEST_SHEET_NAME)),
        UnexpectedContentType(settlementFeeSummaryNotCurrency("R$ l,32", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(settlementFeeSummaryNotDouble("R$ l,32", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 33)("1.32", 31, 32)(TEST_SHEET_NAME)),
        UnexpectedContentType(tradingFeesSummaryNotCurrency("R$ O,34", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(tradingFeesSummaryNotDouble("R$ O,34", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 33)("0.34", 31, 32)(TEST_SHEET_NAME)),
        UnexpectedContentType(brokerageSummaryNotCurrency("R$ 3l,98", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(brokerageSummaryNotDouble("R$ 3l,98", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedBrokerageSummary("0.00", 33)("31.98", 31, 32)(TEST_SHEET_NAME)),
        UnexpectedContentType(serviceTaxSummaryNotCurrency("R$ 1,6O", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(serviceTaxSummaryNotDouble("R$ 1,6O", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTaxSummary("0.00", 33)("1.60", 31, 32)(TEST_SHEET_NAME)),
        UnexpectedContentType(incomeTaxAtSourceSummaryNotCurrency("R$ O,OO", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(incomeTaxAtSourceSummaryNotDouble("R$ O,OO", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(totalSummaryNotCurrency("R$ 4.828,2E", 33)(TEST_SHEET_NAME)),
        UnexpectedContentType(totalSummaryNotDouble("R$ 4.828,2E", 33)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 33)("4828.23", 31, 32)(TEST_SHEET_NAME)),

        // Line 39
        UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(39)(TEST_SHEET_NAME)),
        /* NECESSARY */ RequiredValueMissing(noteNumberMissing(39)(TEST_SHEET_NAME)),
        // +CONSEQUENTIAL
          UnexpectedContentValue(unexpectedVolumeSummaryForHeterogeneousGroups("2726.00", 41)("7714.00", 39, 40)(TEST_SHEET_NAME)),
          UnexpectedContentValue(unexpectedTotalSummaryForHeterogeneousGroups("2689.76", 41)("7713.06", 39, 40)(TEST_SHEET_NAME)),
        // -CONSEQUENTIAL

        // Line 40
        UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(40)(TEST_SHEET_NAME)),
        // +NECESSARY 
          RequiredValueMissing(noteNumberMissing(40)(TEST_SHEET_NAME)),
          UnexpectedContentColor(invalidColorInVolume("0,0,0", 40)(TEST_SHEET_NAME)),
          UnexpectedContentColor(invalidColorInSettlementFee("0,0,0", 40)(TEST_SHEET_NAME)),
        // -NECESSARY 
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalForBuyings("5201.41", 40)("5238.59")(TEST_SHEET_NAME)),

        // Line 44
        UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("2908.00", 44)("4290.00", 43, 43)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedSettlementFeeSummary("0.80", 44)("1.18", 43, 43)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTradingFeesSummary("0.20", 44)("0.30", 43, 43)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.14", 44)("0.21", 43, 43)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("2890.21", 44)("4271.73", 43, 43)(TEST_SHEET_NAME)),

        // Line 48
        RequiredValueMissing(volumeSummaryMissing(48)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedVolumeSummaryForHomogeneousGroups("0.00", 48)("7198.00", 46, 47)(TEST_SHEET_NAME)),
        RequiredValueMissing(settlementFeeSummaryMissing(48)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedSettlementFeeSummary("0.00", 48)("1.98", 46, 47)(TEST_SHEET_NAME)),
        RequiredValueMissing(tradingFeesSummaryMissing(48)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTradingFeesSummary("0.00", 48)("0.50", 46, 47)(TEST_SHEET_NAME)),
        RequiredValueMissing(brokerageSummaryMissing(48)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedBrokerageSummary("0.00", 48)("31.98", 46, 47)(TEST_SHEET_NAME)),
        RequiredValueMissing(serviceTaxSummaryMissing(48)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedServiceTaxSummary("0.00", 48)("1.60", 46, 47)(TEST_SHEET_NAME)),
        UnexpectedContentValue(unexpectedIncomeTaxAtSourceSummary("0.00", 48)("0.35", 46, 47)(TEST_SHEET_NAME)),
        
        // Line 52
        RequiredValueMissing(totalSummaryMissing(52)(TEST_SHEET_NAME)),
        /* CONSEQUENTIAL */ UnexpectedContentValue(unexpectedTotalSummaryForHomogeneousGroups("0.00", 52)("7161.94", 50, 51)(TEST_SHEET_NAME))
        
        // TODO Add Cell, Line, and, Worksheet errors once we fix the error accumulation strategy of those classes
      )

      val actualErrors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

      actualErrors should have size 187
      actualErrors should contain theSameElementsAs expectedErrors
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
  private val BLUE = "68,114,196"

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
  private val BLUE = "68,114,196"

  import BrokerageNotesWorksheetMessages.*

  def tradingDateMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("TradingDate", operationIndex)(worksheetName)
  def noteNumberMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("NoteNumber", operationIndex)(worksheetName)
  def tickerMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Ticker", operationIndex)(worksheetName)
  def qtyMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Qty", operationIndex)(worksheetName)
  def priceMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Price", operationIndex)(worksheetName)
  def volumeMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Volume", operationIndex)(worksheetName)
  def settlementFeeMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("SettlementFee", operationIndex)(worksheetName)
  def tradingFeesMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("TradingFees", operationIndex)(worksheetName)
  def brokerageMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Brokerage", operationIndex)(worksheetName)
  def serviceTaxMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("ServiceTax", operationIndex)(worksheetName)
  def totalMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("Total", operationIndex)(worksheetName)
  def volumeSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("VolumeSummary", operationIndex)(worksheetName)
  def settlementFeeSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("SettlementFeeSummary", operationIndex)(worksheetName)
  def tradingFeesSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("TradingFeesSummary", operationIndex)(worksheetName)
  def brokerageSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("BrokerageSummary", operationIndex)(worksheetName)
  def serviceTaxSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("ServiceTaxSummary", operationIndex)(worksheetName)
  def totalSummaryMissing(operationIndex: Int)(worksheetName: String): String =
    attributeMissing("TotalSummary", operationIndex)(worksheetName)

  def unexpectedContentTypeInTradingDate(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingDate", operationIndex)("a date")(worksheetName)
  def unexpectedContentTypeInNoteNumber(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "NoteNumber", operationIndex)("an integer number")(worksheetName)
  def unexpectedContentTypeInQty(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Qty", operationIndex)("an integer number")(worksheetName)
  def priceNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Price", operationIndex)("a currency")(worksheetName)
  def priceNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Price", operationIndex)("a double")(worksheetName)
  def volumeNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Volume", operationIndex)("a currency")(worksheetName)
  def volumeNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Volume", operationIndex)("a double")(worksheetName)
  def settlementFeeNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFee", operationIndex)("a currency")(worksheetName)
  def settlementFeeNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFee", operationIndex)("a double")(worksheetName)
  def tradingFeesNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFees", operationIndex)("a currency")(worksheetName)
  def tradingFeesNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFees", operationIndex)("a double")(worksheetName)
  def brokerageNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Brokerage", operationIndex)("a currency")(worksheetName)
  def brokerageNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Brokerage", operationIndex)("a double")(worksheetName)
  def serviceTaxNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTax", operationIndex)("a currency")(worksheetName)
  def serviceTaxNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTax", operationIndex)("a double")(worksheetName)
  def incomeTaxAtSourceNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSource", operationIndex)("a currency")(worksheetName)
  def incomeTaxAtSourceNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSource", operationIndex)("a double")(worksheetName)
  def totalNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Total", operationIndex)("a currency")(worksheetName)
  def totalNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Total", operationIndex)("a double")(worksheetName)
  def volumeSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "VolumeSummary", operationIndex)("a currency")(worksheetName)
  def volumeSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "VolumeSummary", operationIndex)("a double")(worksheetName)
  def settlementFeeSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFeeSummary", operationIndex)("a currency")(worksheetName)
  def settlementFeeSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFeeSummary", operationIndex)("a double")(worksheetName)
  def tradingFeesSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFeesSummary", operationIndex)("a currency")(worksheetName)
  def tradingFeesSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFeesSummary", operationIndex)("a double")(worksheetName)
  def brokerageSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "BrokerageSummary", operationIndex)("a currency")(worksheetName)
  def brokerageSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "BrokerageSummary", operationIndex)("a double")(worksheetName)
  def serviceTaxSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTaxSummary", operationIndex)("a currency")(worksheetName)
  def serviceTaxSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTaxSummary", operationIndex)("a double")(worksheetName)
  def incomeTaxAtSourceSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSourceSummary", operationIndex)("a currency")(worksheetName)
  def incomeTaxAtSourceSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSourceSummary", operationIndex)("a double")(worksheetName)
  def totalSummaryNotCurrency(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TotalSummary", operationIndex)("a currency")(worksheetName)
  def totalSummaryNotDouble(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TotalSummary", operationIndex)("a double")(worksheetName)

  def invalidColorInTradingDate(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "TradingDate", operationIndex)(worksheetName)
  def invalidColorInNoteNumber(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "NoteNumber", operationIndex)(worksheetName)
  def invalidColorInTicker(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Ticker", operationIndex)(worksheetName)
  def invalidColorInQty(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Qty", operationIndex)(worksheetName)
  def invalidColorInPrice(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Price", operationIndex)(worksheetName)
  def invalidColorInVolume(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Volume", operationIndex)(worksheetName)
  def invalidColorInSettlementFee(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "SettlementFee", operationIndex)(worksheetName)
  def invalidColorInTradingFees(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "TradingFees", operationIndex)(worksheetName)
  def invalidColorInBrokerage(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Brokerage", operationIndex)(worksheetName)
  def invalidColorInServiceTax(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "ServiceTax", operationIndex)(worksheetName)
  def invalidColorInIncomeTaxAtSource(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "IncomeTaxAtSource", operationIndex)(worksheetName)
  def invalidColorInTotal(attributeColor: String, operationIndex: Int)(worksheetName: String): String =
    invalidAttributeColor(attributeColor, "Total", operationIndex)(worksheetName)

  def unexpectedColorForSelling(attributeName: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedAttributeColor(s"red($RED)", attributeName, operationIndex)("Buying", "Selling")(worksheetName)
  def unexpectedColorForSellingInTradingDate(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("TradingDate", operationIndex)(worksheetName)
  def unexpectedColorForSellingInNoteNumber(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("NoteNumber", operationIndex)(worksheetName)
  def unexpectedColorForSellingInTicker(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Ticker", operationIndex)(worksheetName)
  def unexpectedColorForSellingInQty(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Qty", operationIndex)(worksheetName)
  def unexpectedColorForSellingInPrice(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Price", operationIndex)(worksheetName)
  def unexpectedColorForSellingInVolume(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Volume", operationIndex)(worksheetName)
  def unexpectedColorForSellingInSettlementFee(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("SettlementFee", operationIndex)(worksheetName)
  def unexpectedColorForSellingInTradingFees(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("TradingFees", operationIndex)(worksheetName)
  def unexpectedColorForSellingInBrokerage(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Brokerage", operationIndex)(worksheetName)
  def unexpectedColorForSellingInServiceTax(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("ServiceTax", operationIndex)(worksheetName)
  def unexpectedColorForSellingInIncomeTaxAtSource(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("IncomeTaxAtSource", operationIndex)(worksheetName)
  def unexpectedColorForSellingInTotal(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForSelling("Total", operationIndex)(worksheetName)
  
  def unexpectedColorForBuying(attributeName: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedAttributeColor(s"blue($BLUE)", attributeName, operationIndex)("Selling", "Buying")(worksheetName)
  def unexpectedColorForBuyingInTradingDate(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("TradingDate", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInNoteNumber(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("NoteNumber", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInTicker(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Ticker", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInQty(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Qty", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInPrice(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Price", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInVolume(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Volume", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInSettlementFee(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("SettlementFee", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInTradingFees(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("TradingFees", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInBrokerage(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Brokerage", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInServiceTax(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("ServiceTax", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInIncomeTaxAtSource(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("IncomeTaxAtSource", operationIndex)(worksheetName)
  def unexpectedColorForBuyingInTotal(operationIndex: Int)(worksheetName: String): String =
    unexpectedColorForBuying("Total", operationIndex)(worksheetName)

  def unexpectedNegativeNoteNumber(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedNegativeAttribute("NoteNumber", attributeValue, operationIndex)(worksheetName)
  def unexpectedNegativeQty(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedNegativeAttribute("Qty", attributeValue, operationIndex)(worksheetName)
  def unexpectedNegativePrice(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedNegativeAttribute("Price", attributeValue, operationIndex)(worksheetName)
  def unexpectedNegativeBrokerage(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedNegativeAttribute("Brokerage", attributeValue, operationIndex)(worksheetName)

  private def calculatedAttributeSummaryFormulaDescription(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"
  
  private def settlementFeeSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("SettlementFee", 'G', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedSettlementFeeSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "SettlementFeeSummary", s"G$summaryIndex"
    )(
      expectedValue, settlementFeeSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def tradingFeesSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("TradingFees", 'H', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTradingFeesSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "TradingFeesSummary", s"H$summaryIndex"
    )(
      expectedValue, tradingFeesSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def brokerageSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("Brokerage", 'I', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedBrokerageSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "BrokerageSummary", s"I$summaryIndex"
    )(
      expectedValue, brokerageSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def serviceTaxSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("ServiceTax", 'J', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedServiceTaxSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "ServiceTaxSummary", s"J$summaryIndex"
    )(
      expectedValue, serviceTaxSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def incomeTaxAtSourceSummaryFormulaDescription(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    calculatedAttributeSummaryFormulaDescription("IncomeTaxAtSource", 'K', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedIncomeTaxAtSourceSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, "IncomeTaxAtSourceSummary", s"K$summaryIndex"
    )(
      expectedValue, incomeTaxAtSourceSummaryFormulaDescription(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def unexpectedVolumeSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, formulaDescription: String)(worksheetName: String) =
    unexpectedOperationTypeAwareAttributeSummary(
      actualValue, "VolumeSummary", summaryIndex
    )(
      expectedValue, 'F', formulaDescription
    )(worksheetName)

  private def volumeSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) = 
    operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups("Volume", 'F', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedVolumeSummaryForHomogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedVolumeSummary(
      actualValue, summaryIndex
    )(
      expectedValue, volumeSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def volumeSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups("Volume", 'F', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedVolumeSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedVolumeSummary(
      actualValue, summaryIndex
    )(
      expectedValue, volumeSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def unexpectedTotalSummary(actualValue: String, summaryIndex: Int)(expectedValue: String, formulaDescription: String)(worksheetName: String) =
    unexpectedOperationTypeAwareAttributeSummary(
      actualValue, "TotalSummary", summaryIndex
    )(
      expectedValue, 'L', formulaDescription
    )(worksheetName)

  private def totalSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) = 
    operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups("Total", 'L', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTotalSummaryForHomogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups("Total", 'L', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTotalSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)