package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.xssf.usermodel.XSSFRow

import scala.util.{Failure, Try}

case class Line private(number: Int, cells: Seq[Cell]):
  def isEmpty: Boolean = cells.forall(_.isEmpty)
  def isNotEmpty: Boolean = !isEmpty
  def nonEmptyCells: Seq[Cell] = cells.filter(_.isNotEmpty)

// TODO Replace Try + exceptions with Validated
object Line:

  def from(poiRow: XSSFRow, size: Int): Try[Line] = for {
    validatedPOIRow ← poiRow.validated
  } yield Line(validatedPOIRow.getRowNum + 1, validatedPOIRow.cells(size))

  extension (poiRow: XSSFRow)

    private def validated: Try[XSSFRow] = Try {
      if hasNoCells then
        throw new IllegalArgumentException(s"Line ${poiRow.getRowNum} does not seem to have any cells.")
      else poiRow
    } recoverWith {
      case e ⇒ Failure {
        new IllegalArgumentException(s"Invalid line found.", e)
      }
    }

    private def hasNoCells: Boolean = Option(poiRow).forall(_.getLastCellNum == -1)

    private def cells(qty: Int): Seq[Cell] =
      (0 until qty)
        .map(index ⇒ Cell.from(poiRow.getCell(index, CREATE_NULL_AS_BLANK)).get)