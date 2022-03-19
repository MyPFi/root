package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation], val financialSummary: FinancialSummary)

class Operation

class BuyingOperation extends Operation

class SellingOperation extends Operation

class FinancialSummary

object BrokerageNotesWorksheetReader:
  private type Group = Seq[Line]

  private val FORMULA = "FORMULA"
  private val RED = "255,0,0"
  private val BLUE = "68,114,196"
  
  def from(worksheet: Worksheet): BrokerageNotesWorksheetReader = BrokerageNotesWorksheetReader(
    worksheet.groups.map(_.toBrokerageNote)
  )

  extension (group: Group)
    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      group.filter(!_.isSummary).map(_.toOperation),
      FinancialSummary()
    )

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def toOperation: Operation = cells.head.fontColor match {
      case RED ⇒ BuyingOperation()
      case BLUE ⇒ SellingOperation()
      case _ ⇒ Operation()
    }

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty