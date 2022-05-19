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
            "Invalid:" - {
              "Contain 'Attribute's which are either" - {
                "invalid, like:" - {
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
                    "if displayed with an invalid font-color (neither red (255,0,0) nor blue (68,114,196))." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TradingDateBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'TradingDate's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('255,0,0') or blue ('68,114,196').")
                      )
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
                    "if displayed with an invalid font-color (neither red (255,0,0) nor blue (68,114,196))." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "NoteNumberBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'NoteNumber's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('255,0,0') or blue ('68,114,196').")
                      )
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
                    "if displayed with an invalid font-color (neither red (255,0,0) nor blue (68,114,196))." in { poiWorkbook ⇒
                      val TEST_SHEET_NAME = "TickerBlack"
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet(TEST_SHEET_NAME)).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentColor]),
                        'message(s"'Ticker's font-color ('0,0,0') on line '2' of 'Worksheet' '$TEST_SHEET_NAME' can only be red ('255,0,0') or blue ('68,114,196').")
                      )
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
                  }
                }
              }
            }
          }
        }
        "whose" - {
          "'Groups'" - {
            "contain 'Lines' with different" - {
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
            "that contain more than one 'Operation'" - {
              "don't have a 'Summary'." in { poiWorkbook ⇒
                val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("MultiLineGroupWithNoSummary")).get

                val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                error should have(
                  'class(classOf[RequiredValueMissing]),
                  'message(s"An invalid 'Group' ('85060') was found on 'Worksheet' '${TEST_SHEET.name}'. 'MultilineGroup's must have a 'SummaryLine'.")
                )
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
          "'Line's contain 'Cell's" - {
            "with different font-colors." in { poiWorkbook ⇒
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("LineWithDifferentFontColors")).get

              val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

              error should have(
                'class(classOf[UnexpectedContentColor]),
                'message(s"An invalid 'Cell' 'B3' was found on 'Worksheet' '${TEST_SHEET.name}'. 'FontColor' should be the same for all 'Cell's in a 'Line' in order to being able to turn it into an 'Operation' but, '68,114,196' in 'B3' is different from '255,0,0' in 'A3'.")
              )
            }
            "whose font-colors are neither red (255,0,0) nor blue (68,114,196)." ignore { poiWorkbook ⇒
              val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("LineWithBlackFontColor")).get

              val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

              error should have(
                'class(classOf[UnexpectedContentColor]),
                'message(s"An invalid 'Cell' 'A2' was found on 'Worksheet' '${TEST_SHEET.name}'. 'Cell's should have font-color either red (255,0,0) or blue (68,114,196) in order to being able to turn the 'Line's they are in into 'Operation's but this 'Cell' has font-color '0,0,0'.")
              )
            }
            "whose values are supposed to have been calculated from other 'Cell's but, do not pass the recalculation test, namely:" - {
              "For 'Operation's:" - {
                "'Volume', which should equal 'Qty' * 'Price'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("VolumeDoesNotMatchQtyTimesPrice")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'Cell' ('F2:Volume') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '7030.00', which is equal to 'D2:Qty * E2:Price (200 * 35.15)' but, it actually contained '7030.01'.")
                  )
                }
                "'SettlementFee', which should equal the 'Volume' * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' when 'OperationalMode' is" - {
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
                "'NegotiationFees', which should equal the 'Volume' * 'NegotiationFeesRate' at 'TradingDateTime' when 'TradingTime' falls within" - {
                  "'PreOpening'." ignore { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("SettlementFeeNotVolumeTimesRate")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('G2:SettlementFee') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '2.75', which is equal to 'F2:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (11000.00 * 0.00025)' but, it actually contained '2.76'.")
                    )
                  }
                  "'Trading'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidNegotiationsFee")).get

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
                "'ServiceTax', which should equal the 'Brokerage' * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity'." in { poiWorkbook ⇒
                  val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidServiceTax")).get

                  val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                  error should have(
                    'class(classOf[UnexpectedContentValue]),
                    'message(s"An invalid calculated 'Cell' ('J2:ServiceTax') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.13', which is equal to 'I2:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' (1.99 * 6.5%)' but, it actually contained '0.12'.")
                  )
                }
                "'IncomeTaxAtSource', which, for" - {
                  "'SellingOperations', should equal (('Volume' - 'SettlementFee' - 'NegotiationFees' - 'Brokerage' - 'ServiceTax') - ('AverageStockPrice' for the 'Ticker' * 'Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' when 'OperationalMode' is 'Normal'" in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidIncomeTaxAtSource")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('K2:IncomeTaxAtSource') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.09', which is equal to (('F2:Volume' - 'G2:SettlementFee' - 'H2:NegotiationFees' - 'I2:Brokerage' - 'J2:ServiceTax') - ('AverageStockPrice' for the 'C2:Ticker' * 'D2:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' (1803.47 * 0.0050%)' but, it actually contained '0.19'.")
                    )
                  }
                  "'BuyingOperations', should not be calculated and, therefore, should not contain values that are either" - {
                    "non-currencies" in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("IncomeTaxAtSourceNot$OnBuying")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentType]),
                        'message(s"An invalid calculated 'Cell' ('K2:IncomeTaxAtSource') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to be either empty or equal to '0.00' but, it actually contained '0'.")
                      )
                    }
                    "or non-zero." in { poiWorkbook ⇒
                      val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("NonZeroIncomeTaxAtSourceBuying")).get

                      val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                      error should have(
                        'class(classOf[UnexpectedContentValue]),
                        'message(s"An invalid calculated 'Cell' ('K2:IncomeTaxAtSource') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to be either empty or equal to '0.00' but, it actually contained '0.01'.")
                      )
                    }
                  }
                }
                "'Total', which, for" - {
                  "'SellingOperations', should equal the 'Volume' - 'SettlementFee' - 'NegotiationFees' - 'Brokerage' - 'ServiceTax'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalForSelling")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('L2:Total') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '7010.78', which is equal to 'F2:Volume' - 'G2:SettlementFee' - 'H2:NegotiationFees' - 'I2:Brokerage' - 'J2:ServiceTax' but, it actually contained '7010.81'.")
                    )
                  }
                  "'BuyingOperations', should equal 'Volume' + 'SettlementFee' + 'NegotiationFees' + 'Brokerage' + 'ServiceTax'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalForBuying")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'Cell' ('L2:Total') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '11005.42', which is equal to 'F2:Volume' + 'G2:SettlementFee' + 'H2:NegotiationFees' + 'I2:Brokerage' + 'J2:ServiceTax' but, it actually contained '11005.45'.")
                    )
                  }
                }
              }
              "For 'FinancialSummary's:" - {
                "the accessory costs' columns below, which should, each, equal the sum of the corresponding field for all 'Operation's in the 'BrokerageNote', namely:" - {
                  "'SettlementFee'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidSettlementFeeSummary")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('G4:SettlementFeeSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '5.65', which is the sum of all 'SettlementFee's of the 'Group' (G2...G3) but, it actually contained '5.68'.")
                    )
                  }
                  "'NegotiationFees'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidNegotiationFeesSummary")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('H4:NegotiationFeesSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '1.13', which is the sum of all 'NegotiationFees's of the 'Group' (H2...H3) but, it actually contained '1.10'.")
                    )
                  }
                  "'Brokerage'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidBrokerageSummary")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('I4:BrokerageSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '3.98', which is the sum of all 'Brokerage's of the 'Group' (I2...I3) but, it actually contained '3.95'.")
                    )
                  }
                  "'ServiceTax'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidServiceTaxSummary")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('J4:ServiceTaxSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.26', which is the sum of all 'ServiceTax's of the 'Group' (J2...J3) but, it actually contained '0.29'.")
                    )
                  }
                  // TODO There are a few of special cases when it comes to IncomeTaxAtSourceSummary: It could be either empty or zero for Buyings and, empty, zero, or have a greater than zero value for Sellings
                  "'IncomeTaxAtSource'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidIncomeTaxAtSourceSummary")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('K5:IncomeTaxAtSourceSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '0.08', which is the sum of all 'IncomeTaxAtSource's of the 'Group' (K2...K4) but, it actually contained '0.05'.")
                    )
                  }
                }
                "the columns below, which should, each, consider 'SellingOperations' as increasing and 'BuyingOperations' as decreasing the result:" - {
                  "'Volume'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidVolumeSummaryMixedOps")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('F4:VolumeSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '-2110.00', which is the sum of all 'SellingOperation's 'Volume's minus the sum of all 'BuyingOperation's 'Volume's of the 'Group' (F2...F3) but, it actually contained '16810.00'.")
                    )
                  }
                  "'Total'." in { poiWorkbook ⇒
                    val TEST_SHEET = Worksheet.from(poiWorkbook.getSheet("InvalidTotalSummaryMixedOps")).get

                    val error = BrokerageNotesWorksheetReader.from(TEST_SHEET).error

                    error should have(
                      'class(classOf[UnexpectedContentValue]),
                      'message(s"An invalid calculated 'SummaryCell' ('L4:TotalSummary') was found on 'Worksheet' '${TEST_SHEET.name}'. It was supposed to contain '-2110.69', which is the sum of all 'SellingOperation's 'Total's minus the sum of all 'BuyingOperation's 'Total's of the 'Group' (L2...L3) but, it actually contained '16820.69'.")
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
        financialSummary.negotiationFees should equal(operation.negotiationFees)
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