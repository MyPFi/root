package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.Worksheet
import org.scalatest.freespec.FixtureAnyFreeSpec

class BrokerageNotesWorksheetReaderTest extends FixtureAnyFreeSpec :

  import java.io.File
  import org.apache.poi.openxml4j.opc.OPCPackage
  import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFWorkbookFactory}
  import scala.language.deprecated.symbolLiterals
  import org.scalatest.Outcome
  import org.scalatest.matchers.should.Matchers.*
  import org.scalatest.Inspectors.{forAll, forExactly}
  import BrokerageNotesWorksheetReader.BrokerageNoteReaderError.{RequiredValueMissing, UnexpectedContentColor, UnexpectedContentType, UnexpectedContentValue}
  import BrokerageNotesWorksheetMessages.*
  import BrokerageNotesWorksheetTestMessages.*
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInPrice("R$ l5,34", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInVolume("R$ l534,00", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInSettlementFee("R$ O,42", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInTradingFees("R$ O,11", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInBrokerage("R$ l5,99", 2)(TEST_SHEET_NAME)))
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
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentValue(unexpectedNegativeServiceTax("-0.8", 2)(TEST_SHEET_NAME)))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInServiceTax("R$ O,8O", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInIncomeTaxAtSource("R$ O,OO", 2)(TEST_SHEET_NAME)))
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

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(unexpectedContentTypeInTotal("R$ l551,32", 2)(TEST_SHEET_NAME)))
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
            "don't have a 'Summary'." in { poiWorkbook ⇒
              val TEST_SHEET_NAME = "MultiLineGroupWithNoSummary"
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

              val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

              error should have(
                'class(classOf[RequiredValueMissing]),
                'message(summaryLineMissing("85060")(TEST_SHEET_NAME))
              )
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInVolumeSummary("-R$ 9.322,OO", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInSettlementFeeSummary("R$ 2,S6", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInTradingFeesSummary("R$ O,65", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInBrokerageSummary("R$ 4T,97", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInServiceTaxSummary("R$ 2,4O", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInIncomeTaxAtSourceSummary("R$ O,OO", 5)(TEST_SHEET_NAME))
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

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentType]),
                    'message(unexpectedContentTypeInTotalSummary("-R$ 9.37S,S9", 5)(TEST_SHEET_NAME))
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
  def unexpectedContentTypeInPrice(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Price", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInVolume(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Volume", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInSettlementFee(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFee", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInTradingFees(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFees", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInBrokerage(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Brokerage", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInServiceTax(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTax", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInIncomeTaxAtSource(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSource", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInTotal(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "Total", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInVolumeSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "VolumeSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInSettlementFeeSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "SettlementFeeSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInTradingFeesSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TradingFeesSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInBrokerageSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "BrokerageSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInServiceTaxSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "ServiceTaxSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInIncomeTaxAtSourceSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "IncomeTaxAtSourceSummary", operationIndex)("a currency")(worksheetName)
  def unexpectedContentTypeInTotalSummary(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedContentType(attributeValue, "TotalSummary", operationIndex)("a currency")(worksheetName)

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
  def unexpectedNegativeServiceTax(attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedNegativeAttribute("ServiceTax", attributeValue, operationIndex)(worksheetName)

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
  def unexpectedVolumeSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String) =
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
  def unexpectedTotalSummaryForHomogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String) =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHomogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
  
  private def totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups("Total", 'L', indexOfFirstOperation, indexOfLastOperation)
  def unexpectedTotalSummaryForHeterogeneousGroups(actualValue: String, summaryIndex: Int)(expectedValue: String, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String) =
    unexpectedTotalSummary(
      actualValue, summaryIndex
    )(
      expectedValue, totalSummaryFormulaDescriptionForHeterogeneousGroups(indexOfFirstOperation, indexOfLastOperation)
    )(worksheetName)
    
// NOT USED
  // def unexpectedOperationTypeAwareAttributeSummaryForHomogeneousGroups(actualValue: String, attributeName: String, summaryIndex: Int)(expectedValue: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
  //   unexpectedOperationTypeAwareAttributeSummary(
  //     actualValue, attributeName, summaryIndex
  //   )(
  //     expectedValue, attributeLetter, operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups(attributeName, attributeLetter, indexOfFirstOperation, indexOfLastOperation)
  //   )(worksheetName)
    
  // def unexpectedOperationTypeAwareAttributeSummaryForHeterogeneousGroups(actualValue: String, attributeName: String, summaryIndex: Int)(expectedValue: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int)(worksheetName: String): String =
  //   unexpectedOperationTypeAwareAttributeSummary(
  //     actualValue, attributeName, summaryIndex
  //   )(
  //     expectedValue, attributeLetter, operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups(attributeName, attributeLetter, indexOfFirstOperation, indexOfLastOperation)
  //   )(worksheetName)
// NOT USED