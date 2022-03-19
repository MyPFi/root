package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.{Cell, Line, Worksheet}

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

  def from(worksheet: Worksheet): BrokerageNotesWorksheetReader = BrokerageNotesWorksheetReader(
    worksheet.groups.map(_.toBrokerageNote)
  )

  extension (group: Group)

    private def toBrokerageNote: BrokerageNote = BrokerageNote(
      group.filter(!_.isSummary).map(_.toOperation),
      FinancialSummary(group.toTuple._1, group.toTuple._2, group.toTuple._3, group.toTuple._4, group.toTuple._5, group.toTuple._6, group.toTuple._7)
    )

    // TODO Would this be a good use case for Shapeless?
    private def toTuple: (String, String, String, String, String, String, String) = {
      group.head.cells.map(_.value) match {
        case Seq(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, _*) ⇒ (v6, v7, v8, v9, v10, v11, v12)
      }
    }

  extension (line: Line)

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def nonEmptyCells: Seq[Cell] = cells.filter(nonEmpty)

    private def cells: Seq[Cell] = line.cells

    private def toOperation: Operation = cells.head.fontColor match {
      case RED ⇒ BuyingOperation(
        line.cells(5).value, line.cells(6).value, line.cells(7).value, line.cells(8).value, line.cells(9).value, line.cells(10).value, line.cells(11).value
      )
      case BLUE ⇒ SellingOperation(
        line.cells(5).value, line.cells(6).value, line.cells(7).value, line.cells(8).value, line.cells(9).value, line.cells(10).value, line.cells(11).value
      )
      case _ ⇒ Operation(
        line.cells(5).value, line.cells(6).value, line.cells(7).value, line.cells(8).value, line.cells(9).value, line.cells(10).value, line.cells(11).value
      )
    }

  extension (cell: Cell)

    private def isFormula: Boolean = cell.`type` == FORMULA

    private def nonEmpty: Boolean = cell.value.nonEmpty