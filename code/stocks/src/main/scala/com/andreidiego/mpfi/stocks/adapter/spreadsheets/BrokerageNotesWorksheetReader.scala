package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import SettlementFeeRate.OperationalMode
import SettlementFeeRate.OperationalMode.Normal
import excel.poi.{Cell, Line, Worksheet}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap
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

// TODO This will become a separate service soon
class SettlementFeeRate private(val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]):

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRate =
    SettlementFeeRate(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): SettlementFeeRate =
    SettlementFeeRate(
      SortedMap(
        ratesHistory
          .filter(_._1.isBefore(tradingDate))
          .last
      )
    )

  def value: Double =
    val ratesByOperationalModes: Map[OperationalMode, Double] = ratesHistory.last._2

    if ratesByOperationalModes.size == 1 then ratesByOperationalModes.last._2
    else ratesByOperationalModes.getOrElse(Normal, 0.0)

object SettlementFeeRate:
  enum OperationalMode:
    case Normal, DayTrade

  import BrokerageNotesWorksheetReader.dateTimeFormatter
  import OperationalMode.*

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.parse("01/01/0001", dateTimeFormatter) -> Map(Normal -> 0.000275, DayTrade -> 0.0002),
    LocalDate.parse("30/12/2009", dateTimeFormatter) -> Map(Normal -> 0.000275, DayTrade -> 0.0002),
    LocalDate.parse("12/03/2021", dateTimeFormatter) -> Map(Normal -> 0.00025, DayTrade -> 0.00018)
  )

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRate = SettlementFeeRate(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): SettlementFeeRate = SettlementFeeRate(ratesHistory).at(tradingDate)

object BrokerageNotesWorksheetReader:
  private type Group = Seq[Line]

  private val FORMULA = "FORMULA"
  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def from(worksheet: Worksheet): Try[BrokerageNotesWorksheetReader] = Try {
    BrokerageNotesWorksheetReader(
      worksheet.groups.map(_
        .validatedWith(
          assertLinesInGroupHaveSameTradingDate(worksheet.name),
          assertLinesInGroupHaveSameNoteNumber(worksheet.name),
          assertCellsInLineHaveSameFontColor(worksheet.name),
          assertCellsInLineHaveFontColorRedOrBlue(worksheet.name),
          assertCellsInLineAreCalculatedCorrectly(worksheet.name)
        )
        .map(_.toBrokerageNote)
        .get
      )
    )
  }

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

  private def assertCellsInLineAreCalculatedCorrectly(worksheetName: String): (Line, Line) ⇒ Line = (firstLine: Line, secondLine: Line) ⇒
    val qtyCell = firstLine.cells(3)
    val priceCell = firstLine.cells(4)
    val volumeCell = firstLine.cells(5)
    val expectedVolume = qtyCell.asInt * priceCell.asDouble

    if volumeCell.asDouble != expectedVolume then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${volumeCell.address}:Volume') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedVolume', which is equal to '${qtyCell.address}:Qty * ${priceCell.address}:Price (${qtyCell.asInt} * ${priceCell.asDouble})' but, it actually contained '${volumeCell.asDouble}'."
    )

    val settlementFeeCell = firstLine.cells(6)
    val tradingDate = firstLine.cells.head.asLocalDate
    // TODO Actually detecting the correct 'OperationalMode' may prove challenging when creating a 'BrokerageNote', unless it happens in real-time, since the difference between 'Normal' and 'DayTrade' is actually time-related. A 'BrokerageNote' instance is supposed to be created when a brokerage note document is detected in the filesystem or is provided to the system by any other means. That document contains only the 'TradingDate' and not the time so, unless the system is provided with information about the brokerage note document as soon as an 'Order' gets executed (the moment that gives birth to a brokerage note), that won't be possible. It is important to note that, generally, brokerage notes are not made available by 'Broker's until the day after the fact ('Operation's for the whole day are grouped in a brokerage note, that's why). Maybe we should try a different try and error approach when ingesting a brokerage note document: First we try to check the calculation of the 'SettlementFee' assuming the 'Normal' 'OperationMode' and if that does not work, than we switch it to 'DayTrade' and try again. If that does not work, then we have found a problem with the calculation applied by the 'Broker'.
    val settlementFeeRate = SettlementFeeRate.forOperationalMode(Normal).at(tradingDate).value
    val expectedSettlementFee = volumeCell.asDouble * settlementFeeRate

    if settlementFeeCell.asDouble != expectedSettlementFee then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${settlementFeeCell.address}:SettlementFee') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedSettlementFee', which is equal to '${volumeCell.address}:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (${volumeCell.asDouble} * ${settlementFeeRate.formatted("%.5f")})' but, it actually contained '${settlementFeeCell.asDouble}'."
    )
    secondLine

  extension (worksheet: Worksheet)

    private def name: String = "???Placeholder until we add the name field to the Worksheet class???"

  extension (group: Group)

    private def validatedWith(validations: (Line, Line) ⇒ Line*): Try[Group] = Try {
      group.reduceLeft { (first: Line, second: Line) ⇒
        if second.isSummary then first
        else
          validations.foreach(_ (first, second))
          second
      }
      group
    }

    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      nonSummaryLines.map(_.toOperation),
      group.head.toFinancialSummary
    )

    private def nonSummaryLines: Seq[Line] = group.filter(!_.isSummary)

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def calculatedCells: Seq[Cell] = cells.filter(_.address.startsWith("F"))

    private def toOperation: Operation = cells.head.fontColor match {
      case RED ⇒ BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
      case BLUE ⇒ SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
      case _ ⇒ Operation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)
    }

    private def toFinancialSummary: FinancialSummary =
      FinancialSummary(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty

    private def asInt: Int = cell.value.toInt

    private def asDouble: Double = cell.value.replace(",", ".").toDouble

    private def asLocalDate: LocalDate = LocalDate.parse(cell.value, dateTimeFormatter)

  extension (double: Double)
    private def formatted(format: String): String = String.format(java.util.Locale.US, format, double)