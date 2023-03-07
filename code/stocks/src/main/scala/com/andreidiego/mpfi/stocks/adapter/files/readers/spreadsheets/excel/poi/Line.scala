package com.andreidiego.mpfi.stocks.adapter.files.readers.spreadsheets.excel.poi

import cats.data.ValidatedNec
import cats.syntax.validated.*
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.xssf.usermodel.XSSFRow

case class Line private(number: Int, cells: Seq[Cell]):
  def isEmpty: Boolean = cells.forall(_.isEmpty)

  def isNotEmpty: Boolean = !isEmpty

  def nonEmptyCells: Seq[Cell] = cells.filter(_.isNotEmpty)

object Line:
  enum LineError(message: String):
    case IllegalArgument(message: String) extends LineError(message)

  import LineError.*

  type Error = LineError | Cell.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]

  def from(poiRow: XSSFRow, size: Int): ErrorsOr[Line] = poiRow.validated
    .andThen(_.cells(size).map(Line(poiRow.getRowNum + 1, _)))

  extension (poiRow: XSSFRow)

    // FIXME Errors aren't being combined/accumulated
    private def validated: ErrorsOr[XSSFRow] =
      if poiRow == null then
        IllegalArgument(s"Invalid line found: '$poiRow'").invalidNec
      else if hasNoCells then
        IllegalArgument(s"Invalid line found: line '${poiRow.getRowNum}' does not seem to have any cells.").invalidNec
      else poiRow.validNec

    private def hasNoCells: Boolean = Option(poiRow).forall(_.getLastCellNum == -1)

    private def cells(qty: Int): ErrorsOr[Seq[Cell]] =
      (0 until qty)
        .map(index ⇒ Cell.from(poiRow.getCell(index, CREATE_NULL_AS_BLANK)))
        .map(errorsOrCell => errorsOrCell.map(cell ⇒ Seq(cell)))
        .reduce(_ combine _)