package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import cats.data.ValidatedNec
import cats.implicits.*
import cats.kernel.Semigroup
import cats.syntax.validated.*
import com.andreidiego.mpfi.stocks.adapter.services.*
import com.andreidiego.mpfi.stocks.adapter.services.OperationalMode.Normal
import excel.poi.{Cell, Line, Worksheet}

import java.time.{LocalDate, LocalDateTime}
import java.util.Locale
import scala.annotation.targetName
import scala.math.Ordering.Implicits.*

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation], val financialSummary: FinancialSummary)

class Operation(val volume: String, val settlementFee: String, val tradingFees: String, val brokerage: String, val serviceTax: String, val incomeTaxAtSource: String, val total: String)

// TODO Add test for turning BuyingOperation into a case class
case class BuyingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total)

// TODO Add test for turning SellingOperation into a case class
case class SellingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total)

// TODO Add test for turning FinancialSummary into a case class
case class FinancialSummary(volume: String, settlementFee: String, tradingFees: String, brokerage: String, serviceTax: String, incomeTaxAtSource: String, total: String)

object BrokerageNotesWorksheetReader:
  enum BrokerageNoteReaderError(message: String):
    case RequiredValueMissing(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentValue(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentType(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentColor(message: String) extends BrokerageNoteReaderError(message)

  import BrokerageNoteReaderError.*

  type Error = BrokerageNoteReaderError | Worksheet.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]
  private type Group = Seq[Line]

  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

  given comparisonPrecision: Double = 0.02

  given Semigroup[Group] = (x: Group, y: Group) => if x == y then x else x ++: y

  def from(worksheet: Worksheet): ErrorsOr[BrokerageNotesWorksheetReader] = worksheet.groups
    .map(_.validatedWith(Seq(
      assertMultilineGroupHasSummary(worksheet.name),
      assertSettlementFeeSummaryIsCalculatedCorrectly(worksheet.name),
      assertTradingFeesSummaryIsCalculatedCorrectly(worksheet.name),
      assertBrokerageSummaryIsCalculatedCorrectly(worksheet.name),
      assertServiceTaxSummaryIsCalculatedCorrectly(worksheet.name),
      assertIncomeTaxAtSourceSummaryIsCalculatedCorrectly(worksheet.name),
      assertVolumeSummaryIsCalculatedCorrectly(worksheet.name),
      assertTotalSummaryIsCalculatedCorrectly(worksheet.name)
    ), Seq(
      assertVolumeIsCalculatedCorrectly(worksheet.name),
      assertSettlementFeeIsCalculatedCorrectly(worksheet.name),
      assertTradingFeesIsCalculatedCorrectly(worksheet.name),
      assertServiceTaxIsCalculatedCorrectly(worksheet.name),
      assertIncomeTaxAtSourceIsCalculatedCorrectly(worksheet.name),
      assertTotalIsCalculatedCorrectly(worksheet.name)
    ), Seq(
      assertLinesInGroupHaveSameTradingDate(worksheet.name),
      assertLinesInGroupHaveSameNoteNumber(worksheet.name)
    ), Seq(
      assertTradingDate(isPresent, hasAValidFontColor)(worksheet.name),
      assertNoteNumber(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor)(worksheet.name),
      assertTicker(isPresent, hasAValidFontColor)(worksheet.name),
      assertQty(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor)(worksheet.name),
      assertPrice(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertVolume(isPresent, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertSettlementFee(isPresent, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertTradingFees(isPresent, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertBrokerage(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertServiceTax(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertIncomeTaxAtSource(isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertTotal(isPresent, isAValidCurrency, hasAValidFontColor)(worksheet.name),
      assertCellsInLineHaveFontColorRedOrBlue(worksheet.name)
    ), Seq(
      assertCellsInLineHaveSameFontColor(worksheet.name)
    )).andThen((group: Group) ⇒ Seq(group.toBrokerageNote).validNec))
    .reduce(_ |+| _)
    .map(BrokerageNotesWorksheetReader(_))

  private def assertMultilineGroupHasSummary(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    group.nonSummaryLines match
      case Seq(_, _, _*) ⇒ group.summary match
        case None ⇒ group.summaryLikeLine match
          case Some(summaryLikeLine) ⇒
            val invalidSummaryCells = summaryLikeLine.nonEmptyCells
              .filter(!_.isFormula)
              .map(cell ⇒ s"${cell.address}:${cell.`type`}")
              .mkString("[", ",", "]")

            UnexpectedContentType(
              s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. All non-empty 'Cell's of a 'Group's 'Summary' are supposed to be formulas but, that's not the case with '$invalidSummaryCells'."
            ).invalidNec

          case _ ⇒ RequiredValueMissing(
            s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. 'MultilineGroup's must have a 'SummaryLine'."
          ).invalidNec
        case _ ⇒ group.validNec
      case _ ⇒ group.validNec

  private def assertSettlementFeeSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(6, "SettlementFee")(group, worksheetName)

  private def assertOperationIndependentSummaryCellIsCalculatedCorrectly(cellIndex: Int, cellName: String)(group: Group, worksheetName: String): ErrorsOr[Group] =
    group.summary.map { summary ⇒
      val summaryCell = summary.cells(cellIndex)

      val expectedSummaryValue = group.nonSummaryLines.foldLeft(0.0)((acc, line) ⇒ acc + line.cells(cellIndex).asDouble.getOrElse(0.0))
      val actualSummaryValue = summaryCell.asDouble.getOrElse(0.0)

      if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
        s"An invalid calculated 'SummaryCell' ('${summaryCell.address}:${cellName}Summary') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all '$cellName's of the 'Group' (${group.head.cells(cellIndex).address}...${group.takeRight(2).head.cells(cellIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
      ).invalidNec
      else group.validNec
    }.getOrElse(group.validNec)

  private def assertTradingFeesSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(7, "TradingFees")(group, worksheetName)

  private def assertBrokerageSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(8, "Brokerage")(group, worksheetName)

  private def assertServiceTaxSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(9, "ServiceTax")(group, worksheetName)

  private def assertIncomeTaxAtSourceSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(10, "IncomeTaxAtSource")(group, worksheetName)

  private def assertVolumeSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationDependentSummaryCellIsCalculatedCorrectly(5, "Volume", _.volume.asDouble)(group, worksheetName)

  private def assertOperationDependentSummaryCellIsCalculatedCorrectly(cellIndex: Int, cellName: String, valueToSummarizeFrom: Operation ⇒ Double)(group: Group, worksheetName: String) =
    group.summary.map { summary ⇒
      val summaryCell = summary.cells(cellIndex)

      val expectedSummaryValue = group.nonSummaryLines.map(_.toOperation).foldLeft(0.0) { (acc, operation) ⇒
        operation match {
          case operation: BuyingOperation ⇒ acc - valueToSummarizeFrom(operation)
          case _ ⇒ acc + valueToSummarizeFrom(operation)
        }
      }
      val actualSummaryValue = summaryCell.asDouble.getOrElse(0.0)

      if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
        s"An invalid calculated 'SummaryCell' ('${summaryCell.address}:${cellName}Summary') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all 'SellingOperation's '$cellName's minus the sum of all 'BuyingOperation's '$cellName's of the 'Group' (${group.head.cells(cellIndex).address}...${group.takeRight(2).head.cells(cellIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
      ).invalidNec
      else group.validNec
    }.getOrElse(group.validNec)

  private def assertTotalSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ ErrorsOr[Group] = group ⇒
    assertOperationDependentSummaryCellIsCalculatedCorrectly(11, "Total", _.total.asDouble)(group, worksheetName)

  private def assertVolumeIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val qtyCell = line.cells(3)
    val priceCell = line.cells(4)
    val volumeCell = line.cells(5)
    val qty = qtyCell.asInt.getOrElse(0)
    val price = priceCell.asDouble.getOrElse(0.0)

    val actualVolume = volumeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    val expectedVolume = (qty * price).formatted("%.2f")

    if actualVolume != expectedVolume then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${volumeCell.address}:Volume') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedVolume', which is equal to '${qtyCell.address}:Qty * ${priceCell.address}:Price ($qty * ${price.formatted("%.2f")})' but, it actually contained '$actualVolume'."
    ).invalidNec
    else group.validNec

  // TODO Think about adding a reason to the errors for the cases where the requirements for the validation are not present. For instance, in the case below, any one of the following, if missing or having any other problem, would result in an impossible validation: tradingDate, volume and, actualSettlementFee. For now, we'll choose default values for them in case of problem and the validation will fail because of them. Hopefully, the original cause will be caught by other validation.
  private def assertSettlementFeeIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val volumeCell = line.cells(5)
    val settlementFeeCell = line.cells(6)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualSettlementFee = settlementFeeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Actually detecting the correct 'OperationalMode' may prove challenging when creating a 'BrokerageNote', unless it happens in real-time, since the difference between 'Normal' and 'DayTrade' is actually time-related. A 'BrokerageNote' instance is supposed to be created when a brokerage note document is detected in the filesystem or is provided to the system by any other means. That document contains only the 'TradingDate' and not the time so, unless the system is provided with information about the brokerage note document as soon as an 'Order' gets executed (the moment that gives birth to a brokerage note), that won't be possible. It is important to note that, generally, brokerage notes are not made available by 'Broker's until the day after the fact ('Operation's for the whole day are grouped in a brokerage note, that's why). Maybe we should try a different try and error approach when ingesting a brokerage note document: First we try to check the calculation of the 'SettlementFee' assuming the 'Normal' 'OperationMode' and if that does not work, than we switch it to 'DayTrade' and try again. If that does not work, then we have found a problem with the calculation applied by the 'Broker'.
    val settlementFeeRate = SettlementFeeRate.forOperationalMode(Normal).at(tradingDate).value
    val expectedSettlementFee = (volume * settlementFeeRate).formatted("%.2f")

    if actualSettlementFee != expectedSettlementFee then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${settlementFeeCell.address}:SettlementFee') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedSettlementFee', which is equal to '${volumeCell.address}:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (${volume.formatted("%.2f")} * ${(settlementFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '$actualSettlementFee'."
    ).invalidNec
    else group.validNec

  private def assertTradingFeesIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val volumeCell = line.cells(5)
    val negotiationsFeeCell = line.cells(7)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val tradingTime = TradingFeesRate.TRADING
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualNegotiationsFee = negotiationsFeeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Same challenge here since 'TradingFees' is also dependent on the time of order execution which is not part of the brokerage note document.
    val negotiationsFeeRate = TradingFeesRate.at(LocalDateTime.of(tradingDate, tradingTime))
    val expectedNegotiationsFee = (volume * negotiationsFeeRate).formatted("%.2f")

    if actualNegotiationsFee != expectedNegotiationsFee then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${negotiationsFeeCell.address}:NegotiationsFee') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedNegotiationsFee', which is equal to '${volumeCell.address}:Volume * 'NegotiationsFeeRate' at 'TradingDateTime' (${volume.formatted("%.2f")} * ${(negotiationsFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '$actualNegotiationsFee'."
    ).invalidNec
    else group.validNec

  private def assertServiceTaxIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val actualServiceTax = serviceTaxCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO The city used to calculate the ServiceTax can be determined, in the future, by looking into the Broker information present in the brokerage note document.
    val serviceTaxRate = ServiceTaxRate.at(tradingDate).value
    val expectedServiceTax = (brokerage * serviceTaxRate).formatted("%.2f")

    if actualServiceTax != expectedServiceTax then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${serviceTaxCell.address}:ServiceTax') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedServiceTax', which is equal to '${brokerageCell.address}:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' (${brokerage.formatted("%.2f")} * ${(serviceTaxRate * 100).formatted("%.1f")}%)' but, it actually contained '$actualServiceTax'."
    ).invalidNec
    else group.validNec

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val incomeTaxAtSourceCell = line.cells(10)
    // TODO IncomeTaxAtSource can never be negative. It is not like I can restitute it if I have a loss. Restitutions do not occur at the source
    val actualIncomeTaxAtSource = incomeTaxAtSourceCell.asDouble.getOrElse(0.0)

    line.cells.head.fontColor match
      case BLUE ⇒
        val tickerCell = line.cells(2)
        val qtyCell = line.cells(3)
        val volumeCell = line.cells(5)
        val settlementFeeCell = line.cells(6)
        val tradingFeesCell = line.cells(7)
        val brokerageCell = line.cells(8)
        val serviceTaxCell = line.cells(9)

        val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
        val qty = qtyCell.asInt.getOrElse(0)
        val volume = volumeCell.asDouble.getOrElse(0.0)
        val settlementFee = settlementFeeCell.asDouble.getOrElse(0.0)
        val tradingFees = tradingFeesCell.asDouble.getOrElse(0.0)
        val brokerage = brokerageCell.asDouble.getOrElse(0.0)
        val serviceTax = serviceTaxCell.asDouble.getOrElse(0.0)
        val operationNetResult = volume - settlementFee - tradingFees - brokerage - serviceTax
        val operationAverageCost = AverageStockPrice.forTicker(tickerCell.value) * qty
        // TODO When the ticker cannot be found in the portfolio, 0.0 is returned which should trigger an exception since I'm trying to sell something I do not posses. For now, I'll tweak TEST_SPREADSHEET so that all BuyingOperations refer to VALE5 and have the appropriate calculation for the IncomeTaxAtSource.
        val operationProfit = operationNetResult - operationAverageCost
        val incomeTaxAtSourceRate = IncomeTaxAtSourceRate.forOperationalMode(Normal).at(tradingDate).value
        val expectedIncomeTaxAtSource = operationProfit * incomeTaxAtSourceRate

        if actualIncomeTaxAtSource !~= expectedIncomeTaxAtSource then UnexpectedContentValue(
          s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedIncomeTaxAtSource.formatted("%.2f")}', which is equal to (('${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${tradingFeesCell.address}:TradingFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax') - ('AverageStockPrice' for the '${tickerCell.address}:Ticker' * '${qtyCell.address}:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' (${operationProfit.formatted("%.2f")} * ${(incomeTaxAtSourceRate * 100).formatted("%.4f")}%)' but, it actually contained '${actualIncomeTaxAtSource.formatted("%.2f")}'."
        ).invalidNec
        else group.validNec
      case _ ⇒
        if incomeTaxAtSourceCell.nonEmpty then {
          if !incomeTaxAtSourceCell.isCurrency then UnexpectedContentType(
            s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to be either empty or equal to '0.00' but, it actually contained '${incomeTaxAtSourceCell.value}'."
          ).invalidNec
          else group.validNec
        } combine {
          if actualIncomeTaxAtSource > 0.0 then UnexpectedContentValue(
            s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to be either empty or equal to '0.00' but, it actually contained '${if incomeTaxAtSourceCell.isCurrency then actualIncomeTaxAtSource.formatted("%.2f") else incomeTaxAtSourceCell.value}'."
          ).invalidNec
          else group.validNec
        }
        else group.validNec

  private def assertTotalIsCalculatedCorrectly(worksheetName: String): Group ⇒ Line ⇒ ErrorsOr[Group] = group ⇒ line ⇒
    val volumeCell = line.cells(5)
    val settlementFeeCell = line.cells(6)
    val tradingFeesCell = line.cells(7)
    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val totalCell = line.cells(11)
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val settlementFee = settlementFeeCell.asDouble.getOrElse(0.0)
    val tradingFees = tradingFeesCell.asDouble.getOrElse(0.0)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val serviceTax = serviceTaxCell.asDouble.getOrElse(0.0)
    val actualTotal = totalCell.asDouble.getOrElse(0.0)

    line.cells.head.fontColor match
      case BLUE ⇒
        val expectedTotal = volume - settlementFee - tradingFees - brokerage - serviceTax

        if actualTotal !~= expectedTotal then UnexpectedContentValue(
          s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${tradingFeesCell.address}:TradingFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
        ).invalidNec
        else group.validNec
      case _ ⇒
        val expectedTotal = volume + settlementFee + tradingFees + brokerage + serviceTax

        if actualTotal !~= expectedTotal then UnexpectedContentValue(
          s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' + '${settlementFeeCell.address}:SettlementFee' + '${tradingFeesCell.address}:TradingFees' + '${brokerageCell.address}:Brokerage' + '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
        ).invalidNec
        else group.validNec

  private def assertLinesInGroupHaveSameTradingDate(worksheetName: String): Group ⇒ (Line, Line) ⇒ ErrorsOr[Group] = group ⇒ (first: Line, second: Line) ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${second.cells.tail.head.value}') was found on 'Worksheet' '$worksheetName'. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondTradingDateCell.value}' in '${secondTradingDateCell.address}' is different from '${firstTradingDateCell.value}' in '${firstTradingDateCell.address}'."
    ).invalidNec
    else group.validNec

  private def assertLinesInGroupHaveSameNoteNumber(worksheetName: String): Group ⇒ (Line, Line) ⇒ ErrorsOr[Group] = group ⇒ (first: Line, second: Line) ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${secondNoteNumberCell.value}') was found on 'Worksheet' '$worksheetName'. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondNoteNumberCell.value}' in '${secondNoteNumberCell.address}' is different from '${firstNoteNumberCell.value}' in '${firstNoteNumberCell.address}'."
    ).invalidNec
    else group.validNec

  // TODO Add a test for making sure we do not require empty cells to have a fontColor
  private def assertCellsInLineHaveFontColorRedOrBlue(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, _) ⇒
    cell.fontColor match
      case "255,0,0" | "68,114,196" | "" ⇒ group.validNec
      case _ ⇒ UnexpectedContentColor(
        s"An invalid 'Cell' '${cell.address}' was found on 'Worksheet' '$worksheetName'. 'Cell's should have font-color either red (255,0,0) or blue (68,114,196) in order to being able to turn the 'Line's they are in into 'Operation's but this 'Cell' has font-color '${cell.fontColor}'."
      ).invalidNec

  private def assertTradingDate(tradingDateValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("TradingDate", _.isTradingDate, tradingDateValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertNoteNumber(noteNumberValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("NoteNumber", _.isNoteNumber, noteNumberValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertTicker(tickerValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Ticker", _.isTicker, tickerValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertQty(qtyValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Qty", _.isQty, qtyValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertPrice(priceValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Price", _.isPrice, priceValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertVolume(volumeValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Volume", _.isVolume, volumeValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertSettlementFee(settlementFeeValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("SettlementFee", _.isSettlementFee, settlementFeeValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertTradingFees(tradingFeesValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("TradingFees", _.isTradingFees, tradingFeesValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertBrokerage(brokerageValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Brokerage", _.isBrokerage, brokerageValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertServiceTax(serviceTaxValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("ServiceTax", _.isServiceTax, serviceTaxValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertIncomeTaxAtSource(incomeTaxAtSourceValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("IncomeTaxAtSource", _.isIncomeTaxAtSource, incomeTaxAtSourceValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertTotal(totalValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String): Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group] = group ⇒ (cell, lineNumber) ⇒
    assertAttribute("Total", _.isTotal, totalValidations: _*)(worksheetName, group, cell, lineNumber)

  private def assertAttribute(attributeName: String, attributeGuard: Cell ⇒ Boolean, attributeValidations: Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]*)(worksheetName: String, group: Group, cell: Cell, lineNumber: Int) =
    given Semigroup[Cell] = (x, _) => x

    if attributeGuard(cell) then
      attributeValidations
        .map(_ (cell)(attributeName, lineNumber, worksheetName))
        .reduce(_ combine _)
        .map(_ ⇒ group)
    else group.validNec

  private def isPresent(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isNotEmpty then cell.validNec
    else RequiredValueMissing(
      s"A required attribute ('$cellHeader') is missing on line '$lineNumber' of 'Worksheet' '$worksheetName'."
    ).invalidNec

  private def hasAValidFontColor(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isEmpty || Seq(RED, BLUE).contains(cell.fontColor) then cell.validNec
    else UnexpectedContentColor(
      s"'$cellHeader's font-color ('${cell.fontColor}') on line '$lineNumber' of 'Worksheet' '$worksheetName' can only be red ('$RED') or blue ('$BLUE')."
    ).invalidNec

  private def isNotNegative(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.asDouble.forall(_ >= 0.0) then cell.validNec
    else UnexpectedContentValue(
      s"'$cellHeader' (${cell.value}) on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be negative."
    ).invalidNec

  private def isAValidInteger(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.asInt.isDefined then cell.validNec
    else UnexpectedContentType(
      s"'$cellHeader' ('${cell.value}') on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be interpreted as an integer number."
    ).invalidNec

  private def isAValidCurrency(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isEmpty || cell.isCurrency then cell.validNec
    else UnexpectedContentType(
      s"'$cellHeader' ('${cell.value}') on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be interpreted as a currency."
    ).invalidNec

  // TODO Add a test to make sure that empty cells are allowed when comparing cell colors among cells
  private def assertCellsInLineHaveSameFontColor(worksheetName: String): Group ⇒ (Cell, Cell) ⇒ ErrorsOr[Group] = group ⇒ (first: Cell, second: Cell) ⇒
    if first.fontColor.nonEmpty && second.fontColor.nonEmpty && first.fontColor != second.fontColor then UnexpectedContentColor(
      s"An invalid 'Cell' '${second.address}' was found on 'Worksheet' '$worksheetName'. 'FontColor' should be the same for all 'Cell's in a 'Line' in order to being able to turn it into an 'Operation' but, '${second.fontColor}' in '${second.address}' is different from '${first.fontColor}' in '${first.address}'."
    ).invalidNec
    else group.validNec

  extension (group: Group)

    private def validatedWith(groupValidations: Seq[Group ⇒ ErrorsOr[Group]] = Seq(),
                              lineValidations: Seq[Group ⇒ Line ⇒ ErrorsOr[Group]] = Seq(),
                              interLineValidations: Seq[Group ⇒ (Line, Line) ⇒ ErrorsOr[Group]] = Seq(),
                              cellValidations: Seq[Group ⇒ (Cell, Int) ⇒ ErrorsOr[Group]] = Seq(),
                              interCellValidations: Seq[Group ⇒ (Cell, Cell) ⇒ ErrorsOr[Group]] = Seq()
                             ): ErrorsOr[Group] =
      group.nonSummaryLines.sliding(2).foldLeft(
        group.nonSummaryLines.foldLeft(groupValidations.map(_ (group)).reduce(_ `combine` _)) { (acc, line) ⇒
          line.cells.foldLeft(acc.combine(lineValidations.map(_ (group)(line)).reduce(_ `combine` _))) { (acc, cell) ⇒
            acc.combine(cellValidations.map(_ (group)(cell, line.number)).reduce(_ `combine` _))
          }
        }
      ) { (independentValidations, lines) ⇒
        lines match
          case Seq(line1, line2, _*) ⇒
            val lineValidations = independentValidations.combine(interLineValidations.map(_ (group)(line1, line2)).reduce(_ `combine` _))

            validateCellsFromLine(line1)(interCellValidations, lineValidations)
          // TODO How do we validate the cells of the last line??? if nonSummaryLines.size > 1 then validateCells of the latest nonSummaryLine
          case Seq(line) ⇒
            validateCellsFromLine(line)(interCellValidations, independentValidations)
      }

    private def validateCellsFromLine(line: Line)(cellValidations: Seq[Group ⇒ (Cell, Cell) ⇒ ErrorsOr[Group]], accumulatedValidations: ErrorsOr[Group]) =
      line.cells.sliding(2).foldLeft(accumulatedValidations) { (errorAccumulator, cells) ⇒
        cells match
          case Seq(cell1, cell2, _*) ⇒
            errorAccumulator.combine(cellValidations.map(_ (group)(cell1, cell2)).reduce(_ `combine` _))
          case Seq(_) ⇒
            group.validNec
      }

    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      nonSummaryLines.map(_.toOperation),
      group.head.toFinancialSummary
    )

    private def nonSummaryLines: Seq[Line] = group.filter(line => !line.isSummary && !line.isSummaryLikeLine)

    private def summary: Option[Line] = Option(group.last).filter(_.isSummary)

    private def summaryLikeLine: Option[Line] = Option(group.last).filter(_.isSummaryLikeLine)

  extension (line: Line)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def toOperation: Operation = cells.head.fontColor match {
      case RED ⇒ BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
      case BLUE ⇒ SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
      case _ ⇒ Operation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
    }

    private def toFinancialSummary: FinancialSummary =
      FinancialSummary(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)

    private def isSummaryLikeLine: Boolean = nonEmptyCells.count(_.isFormula) > nonEmptyCells.size / 2

  extension (cell: Cell)

    private def isFormula: Boolean = cell.formula.nonEmpty

    private def nonEmpty: Boolean = cell.value.nonEmpty

    private def isTradingDate: Boolean = cell.address.startsWith("A")

    private def isNoteNumber: Boolean = cell.address.startsWith("B")

    private def isTicker: Boolean = cell.address.startsWith("C")

    private def isQty: Boolean = cell.address.startsWith("D")

    private def isPrice: Boolean = cell.address.startsWith("E")

    private def isVolume: Boolean = cell.address.startsWith("F")

    private def isSettlementFee: Boolean = cell.address.startsWith("G")

    private def isTradingFees: Boolean = cell.address.startsWith("H")

    private def isBrokerage: Boolean = cell.address.startsWith("I")

    private def isServiceTax: Boolean = cell.address.startsWith("J")

    private def isIncomeTaxAtSource: Boolean = cell.address.startsWith("K")

    private def isTotal: Boolean = cell.address.startsWith("L")

  extension (double: Double)

    @targetName("differentBeyondPrecision")
    private def !~=(other: Double)(using precision: Double): Boolean = (double - other).abs > precision

    private def formatted(format: String): String = String.format(Locale.US, format, double)

  extension (string: String)

    private def asDouble: Double = string.replace(",", ".").toDouble