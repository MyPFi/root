package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import com.andreidiego.mpfi.stocks.adapter.services.*
import com.andreidiego.mpfi.stocks.adapter.services.OperationalMode.Normal
import excel.poi.{Cell, Line, Worksheet}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.math.Ordering.Implicits.*
import scala.util.Try

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation], val financialSummary: FinancialSummary)

class Operation(val volume: String, val settlementFee: String, val negotiationFees: String, val brokerage: String, val serviceTax: String, val incomeTaxAtSource: String, val total: String)

class BuyingOperation(volume: String, settlementFee: String, negotiationFees: String, brokerage: String, serviceTax: String, incomeTaxAtSource: String, total: String)
  extends Operation(volume, settlementFee, negotiationFees, brokerage, serviceTax, incomeTaxAtSource, total)

class SellingOperation(volume: String, settlementFee: String, negotiationFees: String, brokerage: String, serviceTax: String, incomeTaxAtSource: String, total: String)
  extends Operation(volume, settlementFee, negotiationFees, brokerage, serviceTax, incomeTaxAtSource, total)

class FinancialSummary(val volume: String, val settlementFee: String, val negotiationFees: String, val brokerage: String, val serviceTax: String, val incomeTaxAtSource: String, val total: String)

object BrokerageNotesWorksheetReader:
  private type Group = Seq[Line]

  private val FORMULA = "FORMULA"
  private val NUMERIC = "NUMERIC"
  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

  given comparisonPrecision: Double = 0.02

  def from(worksheet: Worksheet): Try[BrokerageNotesWorksheetReader] = Try {
    BrokerageNotesWorksheetReader(
      worksheet.groups.map(_
        .validatedWith(
          Seq(
            assertMultilineGroupHasSummary(worksheet.name),
            assertSettlementFeeSummaryIsCalculatedCorrectly(worksheet.name),
            assertNegotiationFeesSummaryIsCalculatedCorrectly(worksheet.name),
            assertBrokerageSummaryIsCalculatedCorrectly(worksheet.name),
            assertServiceTaxSummaryIsCalculatedCorrectly(worksheet.name)
          ),
          Seq(
            assertLinesInGroupHaveSameTradingDate(worksheet.name),
            assertLinesInGroupHaveSameNoteNumber(worksheet.name),
            assertCellsInLineHaveSameFontColor(worksheet.name),
            assertCellsInLineHaveFontColorRedOrBlue(worksheet.name),
            assertVolumeIsCalculatedCorrectly(worksheet.name),
            assertSettlementFeeIsCalculatedCorrectly(worksheet.name),
            assertNegotiationFeesIsCalculatedCorrectly(worksheet.name),
            assertServiceTaxIsCalculatedCorrectly(worksheet.name),
            assertIncomeTaxAtSourceIsCalculatedCorrectly(worksheet.name),
            assertTotalIsCalculatedCorrectly(worksheet.name)
          )
        )
        .map(_.toBrokerageNote)
        .get
      )
    )
  }

  private def assertMultilineGroupHasSummary(worksheetName: String): Group ⇒ Group = group ⇒
    group.nonSummaryLines match
      case Seq(a, b, _*) ⇒ group.summary match
        case None ⇒ group.summaryLikeLine match
          case Some(summaryLikeLine) ⇒
            val invalidSummaryCells = summaryLikeLine
              .nonEmptyCells
              .filter(!_.isFormula)
              .map(cell ⇒ s"${cell.address}:${cell.`type`}")
              .mkString("[", ",", "]")

            throw new IllegalArgumentException(
              s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' $worksheetName. All non-empty 'Cell's of a 'Group's 'Summary' are supposed to be formulas but, that's not the case with '$invalidSummaryCells'."
            )
          case _ ⇒ throw new IllegalArgumentException(
            s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' $worksheetName. 'MultilineGroup's must have a 'SummaryLine'."
          )
        case _ ⇒
      case _ ⇒
    group

  private def assertSettlementFeeSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ Group = group ⇒
    assertSummaryCellIsCalculatedCorrectly(6, "SettlementFee")(worksheetName, group)

  private def assertSummaryCellIsCalculatedCorrectly(cellIndex: Int, cellName: String)(worksheetName: String, group: Group) = {
    if group.hasSummary then
      val summaryCell = group.last.cells(cellIndex)

      val expectedCellSummary = group.dropRight(1).foldLeft(0.0)((acc, line) ⇒ acc + line.cells(cellIndex).asDouble)
      val actualCellSummary = summaryCell.asDouble

      if actualCellSummary !~= expectedCellSummary then throw new IllegalArgumentException(
        s"An invalid calculated 'SummaryCell' ('${summaryCell.address}:${cellName}Summary') was found on 'Worksheet' $worksheetName. It was supposed to contain '${expectedCellSummary.formatted("%.2f")}', which is the sum of all '$cellName's of the 'Group' (${group.head.cells(cellIndex).address}...${group.takeRight(2).head.cells(cellIndex).address}) but, it actually contained '${actualCellSummary.formatted("%.2f")}'."
      )
    group
  }

  private def assertNegotiationFeesSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ Group = group ⇒
    assertSummaryCellIsCalculatedCorrectly(7, "NegotiationFees")(worksheetName, group)

  private def assertBrokerageSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ Group = group ⇒
    assertSummaryCellIsCalculatedCorrectly(8, "Brokerage")(worksheetName, group)

  private def assertServiceTaxSummaryIsCalculatedCorrectly(worksheetName: String): Group ⇒ Group = group ⇒
    assertSummaryCellIsCalculatedCorrectly(9, "ServiceTax")(worksheetName, group)

  private def assertLinesInGroupHaveSameTradingDate(worksheetName: String): (Line, Line) ⇒ Line = (first: Line, second: Line) ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then throw new IllegalArgumentException(
      s"An invalid 'Group' ('${second.cells.tail.head.value}') was found on 'Worksheet' $worksheetName. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondTradingDateCell.value}' in '${secondTradingDateCell.address}' is different from '${firstTradingDateCell.value}' in '${firstTradingDateCell.address}'."
    ) else second

  private def assertLinesInGroupHaveSameNoteNumber(worksheetName: String): (Line, Line) ⇒ Line = (first: Line, second: Line) ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then throw new IllegalArgumentException(
      s"An invalid 'Group' ('${secondNoteNumberCell.value}') was found on 'Worksheet' $worksheetName. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondNoteNumberCell.value}' in '${secondNoteNumberCell.address}' is different from '${firstNoteNumberCell.value}' in '${firstNoteNumberCell.address}'."
    ) else second

  private def assertCellsInLineHaveSameFontColor(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒

    firstLine.nonEmptyCells.reduceLeft { (firstCell: Cell, secondCell: Cell) ⇒
      if firstCell.fontColor != secondCell.fontColor then throw new IllegalArgumentException(
        s"An invalid 'Line' ('${firstLine.cells.head.value} - ${firstLine.cells.tail.head.value} - ${firstLine.cells.tail.tail.head.value} - ${firstLine.cells.tail.tail.tail.head.value}') was found on 'Worksheet' $worksheetName. 'FontColor' should be the same for all 'Cell's in a 'Line' in order to being able to turn it into an 'Operation' but, '${secondCell.fontColor}' in '${secondCell.address}' is different from '${firstCell.fontColor}' in '${firstCell.address}'."
      ) else secondCell
    }
    secondLine

  private def assertCellsInLineHaveFontColorRedOrBlue(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒

    firstLine.nonEmptyCells.reduceLeft { (firstCell: Cell, secondCell: Cell) ⇒
      firstCell.fontColor match
        case "255,0,0" | "68,114,196" ⇒ secondCell
        case _ ⇒ throw new IllegalArgumentException(
          s"An invalid 'Line' ('${firstLine.cells.head.value} - ${firstLine.cells.tail.head.value} - ${firstLine.cells.tail.tail.head.value} - ${firstLine.cells.tail.tail.tail.head.value}') was found on 'Worksheet' $worksheetName. 'Line's should have font-color either red (255,0,0) or blue (68,114,196) in order to being able to turn them into 'Operation's but this 'Line' has font-color '0,0,0'."
        )
    }
    secondLine

  private def assertVolumeIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val qtyCell = firstLine.cells(3)
    val priceCell = firstLine.cells(4)
    val volumeCell = firstLine.cells(5)
    val expectedVolume = qtyCell.asInt * priceCell.asDouble

    if volumeCell.asDouble != expectedVolume then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${volumeCell.address}:Volume') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedVolume', which is equal to '${qtyCell.address}:Qty * ${priceCell.address}:Price (${qtyCell.asInt} * ${priceCell.asDouble})' but, it actually contained '${volumeCell.asDouble}'."
    )
    secondLine

  private def assertSettlementFeeIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val volumeCell = firstLine.cells(5)
    val settlementFeeCell = firstLine.cells(6)
    val tradingDate = firstLine.cells.head.asLocalDate
    // TODO Actually detecting the correct 'OperationalMode' may prove challenging when creating a 'BrokerageNote', unless it happens in real-time, since the difference between 'Normal' and 'DayTrade' is actually time-related. A 'BrokerageNote' instance is supposed to be created when a brokerage note document is detected in the filesystem or is provided to the system by any other means. That document contains only the 'TradingDate' and not the time so, unless the system is provided with information about the brokerage note document as soon as an 'Order' gets executed (the moment that gives birth to a brokerage note), that won't be possible. It is important to note that, generally, brokerage notes are not made available by 'Broker's until the day after the fact ('Operation's for the whole day are grouped in a brokerage note, that's why). Maybe we should try a different try and error approach when ingesting a brokerage note document: First we try to check the calculation of the 'SettlementFee' assuming the 'Normal' 'OperationMode' and if that does not work, than we switch it to 'DayTrade' and try again. If that does not work, then we have found a problem with the calculation applied by the 'Broker'.
    val settlementFeeRate = SettlementFeeRate.forOperationalMode(Normal).at(tradingDate).value
    val expectedSettlementFee = volumeCell.asDouble * settlementFeeRate

    if settlementFeeCell.asDouble != expectedSettlementFee then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${settlementFeeCell.address}:SettlementFee') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedSettlementFee', which is equal to '${volumeCell.address}:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (${volumeCell.asDouble} * ${(settlementFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '${settlementFeeCell.asDouble}'."
    )
    secondLine

  private def assertNegotiationFeesIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val volumeCell = firstLine.cells(5)
    val negotiationsFeeCell = firstLine.cells(7)
    val tradingDate = firstLine.cells.head.asLocalDate
    val tradingTime = NegotiationFeesRate.TRADING
    // TODO Same challenge here since 'NegotiationFees' is also dependent on the time of order execution which is not part of the brokerage note document.
    val negotiationsFeeRate = NegotiationFeesRate.at(LocalDateTime.of(tradingDate, tradingTime))
    val expectedNegotiationsFee = (volumeCell.asDouble * negotiationsFeeRate).formatted("%.2f")
    val actualNegotiationsFee = negotiationsFeeCell.asDouble.formatted("%.2f")

    if actualNegotiationsFee != expectedNegotiationsFee then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${negotiationsFeeCell.address}:NegotiationsFee') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedNegotiationsFee', which is equal to '${volumeCell.address}:Volume * 'NegotiationsFeeRate' at 'TradingDateTime' (${volumeCell.asDouble} * ${(negotiationsFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '$actualNegotiationsFee'."
    )
    secondLine

  private def assertServiceTaxIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val tradingDate = firstLine.cells.head.asLocalDate
    val brokerageCell = firstLine.cells(8)
    val serviceTaxCell = firstLine.cells(9)
    // TODO The city used to calculate the ServiceTax can be determined, in the future, by looking into the Broker information present in the brokerage note document.
    val serviceTaxRate = ServiceTaxRate.at(tradingDate).value
    val expectedServiceTax = (brokerageCell.asDouble * serviceTaxRate).formatted("%.2f")
    val actualServiceTax = serviceTaxCell.asDouble.formatted("%.2f")

    if actualServiceTax != expectedServiceTax then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${serviceTaxCell.address}:ServiceTax') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedServiceTax', which is equal to '${brokerageCell.address}:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' (${brokerageCell.asDouble} * ${(serviceTaxRate * 100).formatted("%.1f")}%)' but, it actually contained '$actualServiceTax'."
    )
    secondLine

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val incomeTaxAtSourceCell = firstLine.cells(10)

    firstLine.cells.head.fontColor match
      case BLUE ⇒
        val tickerCell = firstLine.cells(2)
        val qtyCell = firstLine.cells(3)
        val volumeCell = firstLine.cells(5)
        val settlementFeeCell = firstLine.cells(6)
        val negotiationFeesCell = firstLine.cells(7)
        val brokerageCell = firstLine.cells(8)
        val serviceTaxCell = firstLine.cells(9)

        val tradingDate = firstLine.cells.head.asLocalDate
        val incomeTaxAtSourceRate = IncomeTaxAtSourceRate.forOperationalMode(Normal).at(tradingDate).value
        val operationNetResult = volumeCell.asDouble - settlementFeeCell.asDouble - negotiationFeesCell.asDouble - brokerageCell.asDouble - serviceTaxCell.asDouble
        val operationAverageCost = AverageStockPrice.forTicker(tickerCell.value) * qtyCell.asInt
        // TODO When the ticker cannot be found in the portfolio, 0.0 is returned which should trigger an exception since I'm trying to sell something I do not posses. For now, I'll tweak TEST_SPREADSHEET so that all BuyingOperations refer to VALE5 and have the appropriate calculation for the IncomeTaxAtSource.
        val operationProfit = operationNetResult - operationAverageCost
        val expectedIncomeTaxAtSource = operationProfit * incomeTaxAtSourceRate
        val actualIncomeTaxAtSource = incomeTaxAtSourceCell.asDouble

        if actualIncomeTaxAtSource !~= expectedIncomeTaxAtSource then throw new IllegalArgumentException(
          s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' $worksheetName. It was supposed to contain '${expectedIncomeTaxAtSource.formatted("%.2f")}', which is equal to (('${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${negotiationFeesCell.address}:NegotiationFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax') - ('AverageStockPrice' for the '${tickerCell.address}:Ticker' * '${qtyCell.address}:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' (${operationProfit.formatted("%.2f")} * ${(incomeTaxAtSourceRate * 100).formatted("%.4f")}%)' but, it actually contained '${actualIncomeTaxAtSource.formatted("%.2f")}'."
        )
      case _ ⇒
        if incomeTaxAtSourceCell.nonEmpty && (!incomeTaxAtSourceCell.isCurrency || incomeTaxAtSourceCell.asDouble > 0.0) then throw new IllegalArgumentException(
          s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' $worksheetName. It was supposed to be either empty or equal to '0.00' but, it actually contained '${if incomeTaxAtSourceCell.isCurrency then incomeTaxAtSourceCell.asDouble.formatted("%.2f") else incomeTaxAtSourceCell.value}'."
        )
    secondLine

  private def assertTotalIsCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val volumeCell = firstLine.cells(5)
    val settlementFeeCell = firstLine.cells(6)
    val negotiationFeesCell = firstLine.cells(7)
    val brokerageCell = firstLine.cells(8)
    val serviceTaxCell = firstLine.cells(9)
    val totalCell = firstLine.cells(11)

    firstLine.cells.head.fontColor match
      case BLUE ⇒
        val expectedTotal = volumeCell.asDouble - settlementFeeCell.asDouble - negotiationFeesCell.asDouble - brokerageCell.asDouble - serviceTaxCell.asDouble
        val actualTotal = totalCell.asDouble

        if actualTotal !~= expectedTotal then throw new IllegalArgumentException(
          s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' $worksheetName. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${negotiationFeesCell.address}:NegotiationFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
        )
      case _ ⇒
        val expectedTotal = volumeCell.asDouble + settlementFeeCell.asDouble + negotiationFeesCell.asDouble + brokerageCell.asDouble + serviceTaxCell.asDouble
        val actualTotal = totalCell.asDouble

        if actualTotal !~= expectedTotal then throw new IllegalArgumentException(
          s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' $worksheetName. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' + '${settlementFeeCell.address}:SettlementFee' + '${negotiationFeesCell.address}:NegotiationFees' + '${brokerageCell.address}:Brokerage' + '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
        )
    secondLine

  extension (worksheet: Worksheet)

    private def name: String = "???Placeholder until we add the name field to the Worksheet class???"

  extension (group: Group)

    private def validatedWith(groupValidations: Seq[Group ⇒ Group] = Seq(), lineValidations: Seq[(Line, Line) ⇒ Line] = Seq()): Try[Group] = Try {
      groupValidations.foreach(_ (group))

      group.reduceLeft { (first: Line, second: Line) ⇒
        if second.isSummary then first
        else
          lineValidations.foreach(_ (first, second))
          second
      }
      group
    }

    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      nonSummaryLines.map(_.toOperation),
      group.head.toFinancialSummary
    )

    private def nonSummaryLines: Seq[Line] = group.filter(!_.isSummary)

    private def summary: Option[Line] = Option(group.last).filter(_.isSummary)

    private def summaryLikeLine: Option[Line] = Option(group.last).filter(_.isSummaryLikeLine)

    private def hasSummary: Boolean = summary.nonEmpty

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

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def asDouble: Double = cell.value.replace(",", ".").toDouble

    private def asInt: Int = cell.value.toInt

    private def asLocalDate: LocalDate = LocalDate.parse(cell.value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    private def nonEmpty: Boolean = cell.value.nonEmpty

    private def isCurrency: Boolean = cell.`type` == NUMERIC && cell.mask.contains("$")

  extension (double: Double)

    private def !~=(other: Double)(using precision: Double): Boolean = (double - other).abs > precision

    private def formatted(format: String): String = String.format(Locale.US, format, double)