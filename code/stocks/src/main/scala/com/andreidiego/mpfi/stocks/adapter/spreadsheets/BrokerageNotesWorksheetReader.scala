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
          assertOperationsHaveSameTradingDate(worksheet.name),
          assertOperationsHaveSameNoteNumber(worksheet.name)
        )
        .map(_.toBrokerageNote)
        .get
      )
    )
  }

  private def assertOperationsHaveSameTradingDate(worksheetName: String): (Line, Line) ⇒ Line = (first: Line, second: Line) ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then throw new IllegalArgumentException(
      s"An invalid 'BrokerageNote' ('${second.cells.tail.head.value}') was found on 'Worksheet' $worksheetName. 'TradingDate's should be the same for all 'Operations' in a 'BrokerageNote' but '${secondTradingDateCell.value}' in '${secondTradingDateCell.address}' is different from '${firstTradingDateCell.value}' in '${firstTradingDateCell.address}'."
    ) else second

  private def assertOperationsHaveSameNoteNumber(worksheetName: String): (Line, Line) ⇒ Line = (first: Line, second: Line) ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then throw new IllegalArgumentException(
      s"An invalid 'BrokerageNote' ('${secondNoteNumberCell.value}') was found on 'Worksheet' $worksheetName. 'NoteNumber's should be the same for all 'Operations' in a 'BrokerageNote' but '${secondNoteNumberCell.value}' in '${secondNoteNumberCell.address}' is different from '${firstNoteNumberCell.value}' in '${firstNoteNumberCell.address}'."
    ) else second

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