package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}

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
  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

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
    val expectedValue = qtyCell.asInt * priceCell.asDouble

    if volumeCell.asDouble != expectedValue then throw new IllegalArgumentException(
      s"An invalid calculated 'Cell' ('${volumeCell.address}:Volume') was found on 'Worksheet' $worksheetName. It was supposed to contain '$expectedValue', which is equal to '${qtyCell.address}:Qty * ${priceCell.address}:Price (${qtyCell.asInt} * ${priceCell.asDouble})' but, it actually contained '${volumeCell.asDouble}'."
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