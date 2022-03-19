package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation])

class Operation

object BrokerageNotesWorksheetReader:
  private type Group = Seq[Line]

  private val FORMULA = "FORMULA"

  def from(worksheet: Worksheet): BrokerageNotesWorksheetReader = BrokerageNotesWorksheetReader(
    worksheet.groups.map(_.toBrokerageNote)
  )

  extension (group: Group)
    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      group.filter(!_.isSummary).map(_ ⇒ Operation())
    )

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = line.cells.filter(nonEmpty)

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty