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
                      'message(s"A required attribute ('TradingDate') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'.")
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(s"'TradingDate' ('-39757') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a date.")
                    )
                  }
                  "when containing extraneous characters (anything other than numbers and the  '/' symbol)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(s"'TradingDate' ('O5/11/2008') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a date.")
                    )
                  }
                  "when containing an invalid date." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingDateInvalidDate"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(s"'TradingDate' ('05/13/2008') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a date.")
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TradingDateBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'TradingDate's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingDateRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'TradingDate' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingDateBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'TradingDate' has font-color blue('$BLUE') which denotes 'Selling'.")
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
                      'message(s"A required attribute ('NoteNumber') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'.")
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "NoteNumberNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"'NoteNumber' (-1662) on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be negative.")
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "NoteNumberExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentType]),
                      'message(s"'NoteNumber' ('I662') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as an integer number.")
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "NoteNumberBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'NoteNumber's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "NoteNumberRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'NoteNumber' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "NoteNumberBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'NoteNumber' has font-color blue('$BLUE') which denotes 'Selling'.")
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
                      'message(s"A required attribute ('Ticker') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'.")
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TickerBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Ticker's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TickerRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Ticker' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TickerBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Ticker' has font-color blue('$BLUE') which denotes 'Selling'.")
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
                      'message(s"A required attribute ('Qty') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'.")
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "QtyNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"'Qty' (-100) on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be negative.")
                    )
                  }
                  "when containing extraneous characters (anything other than numbers)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "QtyExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'Qty' ('l00') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as an integer number."
                    ))
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "QtyBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Qty's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "QtyRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Qty' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "QtyBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Qty' has font-color blue('$BLUE') which denotes 'Selling'.")
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
                      'message(s"A required attribute ('Price') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'.")
                    )
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "PriceNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"'Price' (-15.34) on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be negative.")
                    )
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "PriceExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'Price' ('R$$ l5,34') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "PriceBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Price's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "PriceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Price' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "PriceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Price' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('Volume') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "VolumeExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'Volume' ('R$$ l534,00') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than 'Qty' * 'Price'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("VolumeDoesNotMatchQtyTimesPrice")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('F2:Volume') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '7030.00', which is equal to 'D2:Qty * E2:Price (200 * 35.15)' but, it actually contained '7030.01'.")
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "VolumeBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Volume's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." ignore { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "VolumeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Volume' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." ignore { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "VolumeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Volume' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('SettlementFee') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "SettlementFeeExtraneousChars"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'SettlementFee' ('R$$ O,42') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than 'Volume' * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' when 'OperationalMode' is" - {
                    "'Normal'." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeNotVolumeTimesRate")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('G2:SettlementFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '2.75', which is equal to 'F2:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (11000.00 * 0.0250%)' but, it actually contained '2.76'.")
                      )
                    }
                    "'DayTrade'." ignore { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeNotVolumeTimesRate")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('G2:SettlementFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '2.75', which is equal to 'F2:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (11000.00 * 0.00025)' but, it actually contained '2.76'.")
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
                        'message(s"'SettlementFee's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "SettlementFeeRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'SettlementFee' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "SettlementFeeBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'SettlementFee' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('TradingFees') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TradingFeesExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'TradingFees' ('R$$ O,11') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than 'Volume' * 'TradingFeesRate' at 'TradingDateTime' when 'TradingTime' falls within" - {
                    "'PreOpening'." ignore { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeNotVolumeTimesRate")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('G2:SettlementFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '2.75', which is equal to 'F2:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (11000.00 * 0.00025)' but, it actually contained '2.76'.")
                      )
                    }
                    "'Trading'." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTradingFees")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('H2:NegotiationsFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.55', which is equal to 'F2:Volume * 'NegotiationsFeeRate' at 'TradingDateTime' (11000.00 * 0.0050%)' but, it actually contained '0.56'.")
                      )
                    }
                    "'ClosingCall'." ignore { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeNotVolumeTimesRate")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('G2:SettlementFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '2.75', which is equal to 'F2:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (11000.00 * 0.00025)' but, it actually contained '2.76'.")
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
                        'message(s"'TradingFees's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingFeesRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'TradingFees' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TradingFeesBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'TradingFees' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('Brokerage') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "BrokerageNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentValue(
                      s"'Brokerage' (-15.99) on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be negative."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "BrokerageExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'Brokerage' ('R$$ l5,99') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "BrokerageBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Brokerage's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "BrokerageRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Brokerage' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "BrokerageBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Brokerage' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('ServiceTax') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "if negative." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxNegative"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentValue(
                      s"'ServiceTax' (-0.8) on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be negative."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "ServiceTaxExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'ServiceTax' ('R$$ O,8O') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than 'Brokerage' * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidServiceTax")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('J2:ServiceTax') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.13', which is equal to 'I2:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' (1.99 * 6.5%)' but, it actually contained '0.12'.")
                    )
                  }
                  "if displayed with an invalid font-color" - {
                    "one that is neither red (255,0,0) nor blue (68,114,196)." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "ServiceTaxBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'ServiceTax's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "ServiceTaxRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'ServiceTax' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "ServiceTaxBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'ServiceTax' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(UnexpectedContentType(
                      s"'IncomeTaxAtSource' ('R$$ O,OO') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than" - {
                    "for 'SellingOperations', (('Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax') - ('AverageStockPrice' for the 'Ticker' * 'Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' when 'OperationalMode' is 'Normal'" in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidIncomeTaxAtSource")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('K2:IncomeTaxAtSource') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.09', which is equal to (('F2:Volume' - 'G2:SettlementFee' - 'H2:TradingFees' - 'I2:Brokerage' - 'J2:ServiceTax') - ('AverageStockPrice' for the 'C2:Ticker' * 'D2:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' (1803.47 * 0.0050%)' but, it actually contained '0.19'.")
                      )
                    }
                    "for 'BuyingOperations, if not empty, zero." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NonZeroIncomeTaxAtSourceBuying")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('K2:IncomeTaxAtSource') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to be either empty or equal to '0.00' but, it actually contained '0.01'.")
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
                        'message(s"'IncomeTaxAtSource's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "IncomeTaxAtSourceRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'IncomeTaxAtSource' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "IncomeTaxAtSourceBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'IncomeTaxAtSource' has font-color blue('$BLUE') which denotes 'Selling'.")
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

                    errors should contain(RequiredValueMissing(
                      s"A required attribute ('Total') is missing on line '2' of 'Worksheet' '$TEST_SHEET_NAME'."
                    ))
                  }
                  "when containing extraneous characters (anything other than numbers, a dot or comma, and currency symbols $ or R$)." in { poiWorkbook ⇒
                    val TEST_SHEET_NAME = "TotalExtraneousCharacters"
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                    val errors = BrokerageNotesWorksheetReader.from(TEST_SHEET).errors

                    errors should contain(UnexpectedContentType(
                      s"'Total' ('R$$ l551,32') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' cannot be interpreted as a currency."
                    ))
                  }
                  "if different than" - {
                    "for 'SellingOperations', 'Volume' - 'SettlementFee' - 'TradingFees' - 'Brokerage' - 'ServiceTax'." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalForSelling")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('L2:Total') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '7010.78', which is equal to 'F2:Volume' - 'G2:SettlementFee' - 'H2:TradingFees' - 'I2:Brokerage' - 'J2:ServiceTax' but, it actually contained '7010.81'.")
                      )
                    }
                    "for 'BuyingOperations', 'Volume' + 'SettlementFee' + 'TradingFees' + 'Brokerage' + 'ServiceTax'." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalForBuying")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('L2:Total') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '11005.42', which is equal to 'F2:Volume' + 'G2:SettlementFee' + 'H2:TradingFees' + 'I2:Brokerage' + 'J2:ServiceTax' but, it actually contained '11005.45'.")
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
                        'message(s"'Total's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('$RED') or blue ('$BLUE').")
                      )
                    }
                    "for the operation type:" - { 
                      "Red for 'Sellings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TotalRedForSelling"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Selling' but, 'Total' has font-color red('$RED') which denotes 'Buying'.")
                        )
                      }
                      "Blue for 'Buyings'." in { poiWorkbook ⇒
                        val TEST_SHEET_NAME = "TotalBlueForBuying"
                        val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                        val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                        error should have(
                          'class(classOf[UnexpectedContentColor]),
                          'message(s"The 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' looks like 'Buying' but, 'Total' has font-color blue('$BLUE') which denotes 'Selling'.")
                        )
                      }
                    }
                  }
                }
              }
              "the impossibility of determining its type when it has exactly half of it's 'Attribute's from each of the two valid colors." in { poiWorkbook ⇒
                val TEST_SHEET_NAME = "NoEmptyAttributeHalfOfEachColor"
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentColor]),
                  'message(s"Impossible to determine the type of 'Operation' on line '2' of 'Worksheet' '$TEST_SHEET_NAME' due to exactly half of the non-empty 'Attribute's of each valid color.")
                )
              }
            }
            "Not harmonic with each other, that is, contain different" - {
              "'TradingDate's." in { poiWorkbook ⇒
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupWithDifferentTradingDates")).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  // TODO Replace the 'NoteNumber' below by the 'GroupIndex' after it has been added to the 'Group' class
                  'message(s"An invalid 'Group' ('1662') was found on 'Worksheet' '${TEST_SHEET.name}'. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '06/11/2008' in 'A3' is different from '05/11/2008' in 'A2'.")
                )
              }
              "'NoteNumbers's." in { poiWorkbook ⇒
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupWithDifferentNoteNumbers")).get
                assume(TEST_SHEET.groups.size == 4)

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[UnexpectedContentValue]),
                  'message(s"An invalid 'Group' ('1663') was found on 'Worksheet' '${TEST_SHEET.name}'. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '1663' in 'B3' is different from '1662' in 'B2'.")
                )
              }
            }
          }
          "having more than one 'Operation'" - { 
            "don't have a 'Summary'." in { poiWorkbook ⇒
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("MultiLineGroupWithNoSummary")).get

              val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

              error should have(
                'class(classOf[RequiredValueMissing]),
                'message(s"An invalid 'Group' ('85060') was found on 'Worksheet' '${TEST_SHEET.name}'. 'MultilineGroup's must have a 'SummaryLine'.")
              )
            }
            "have an invalid 'Summary', in which" - {
              "'Volume'" - {
                // TODO Maybe break this test in two???
                "does not consider 'SellingOperations' as increasing and 'BuyingOperations' as decreasing the result." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidVolumeSummaryMixedOps")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('F4:VolumeSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '-2110.00', which is the sum of all 'SellingOperation's 'Volume's minus the sum of all 'BuyingOperation's 'Volume's of the 'Group' (F2...F3) but, it actually contained '16810.00'.")
                  )
                }
              }
              "'SettlementFee'" - {
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidSettlementFeeSummary")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('G4:SettlementFeeSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '5.65', which is the sum of all 'SettlementFee's of the 'Group' (G2...G3) but, it actually contained '5.68'.")
                  )
                }
              }
              "'TradingFees'" - {
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTradingFeesSummary")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('H4:TradingFeesSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '1.13', which is the sum of all 'TradingFees's of the 'Group' (H2...H3) but, it actually contained '1.10'.")
                  )
                }
              }
              "'Brokerage'" - {
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidBrokerageSummary")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('I4:BrokerageSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '3.98', which is the sum of all 'Brokerage's of the 'Group' (I2...I3) but, it actually contained '3.95'.")
                  )
                }
              }
              "'ServiceTax'" - {
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidServiceTaxSummary")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('J4:ServiceTaxSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.26', which is the sum of all 'ServiceTax's of the 'Group' (J2...J3) but, it actually contained '0.29'.")
                  )
                }
              }
              "'IncomeTaxAtSource'" - {
                // TODO There are a few of special cases when it comes to IncomeTaxAtSourceSummary: It could be either empty or zero for Buyings and, empty, zero, or have a greater than zero value for Sellings
                "does not equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidIncomeTaxAtSourceSummary")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('K5:IncomeTaxAtSourceSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.08', which is the sum of all 'IncomeTaxAtSource's of the 'Group' (K2...K4) but, it actually contained '0.05'.")
                  )
                }
              }
              "'Total'" - {
                "does not consider 'SellingOperations' as increasing and 'BuyingOperations' as decreasing the result." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalSummaryMixedOps")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'SummaryCell' ('L4:TotalSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '-2110.69', which is the sum of all 'SellingOperation's 'Total's minus the sum of all 'BuyingOperation's 'Total's of the 'Group' (L2...L3) but, it actually contained '16820.69'.")
                  )
                }
              }
            }
            "have an invalid 'Summary' (one where not all empty cells are formulas)." in { poiWorkbook ⇒
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("GroupWithInvalidSummary")).get

              val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

              error should have(
                'class(classOf[UnexpectedContentType]),
                'message(s"An invalid 'Group' ('85060') was found on 'Worksheet' '${TEST_SHEET.name}'. All non-empty 'Cell's of a 'Group's 'Summary' are supposed to be formulas but, that's not the case with '[G4:CURRENCY]'.")
              )
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

    private def brokerageNotes: Seq[BrokerageNote] =
      errorsOrBrokerageNotesWorksheetReader.toEither.value.brokerageNotes

    private def errors: Seq[BrokerageNotesWorksheetReader.Error] =
      errorsOrBrokerageNotesWorksheetReader.toEither.left.value.toNonEmptyList.toList

    private def error: BrokerageNotesWorksheetReader.Error =
      errors.head

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