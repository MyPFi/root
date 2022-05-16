package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import cats.data.ValidatedNec
import cats.syntax.validated.*
import org.apache.poi.ss.usermodel.CellType.{BLANK, BOOLEAN, FORMULA, NUMERIC, STRING}
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow}

case class Header private(columnNames: Seq[String])

object Header:
  // TODO Remove the val when 'Worksheet' is migrated to Validated
  enum HeaderError(val message: String):
    case IllegalArgument(override val message: String) extends HeaderError(message)

  import HeaderError.*

  type Error = HeaderError
  type ErrorsOr[A] = ValidatedNec[Error, A]

  def from(poiHeaderRow: XSSFRow): ErrorsOr[Header] =
    poiHeaderRow.validated
      .map(validatedPOIHeaderRow ⇒ Header(validatedPOIHeaderRow.nonBlankCells))

  extension (poiHeaderRow: XSSFRow)

    private def validated: ErrorsOr[XSSFRow] =
      val INVALID_HEADER = "Worksheet does not seem to have a valid header"
      val reason = (cellType: String) ⇒ s"An illegal $cellType cell was found in the header."

      if isNull then IllegalArgument(s"$INVALID_HEADER: '$poiHeaderRow'.").invalidNec
      else if isEmpty then IllegalArgument(s"$INVALID_HEADER. Header is empty.").invalidNec
      else if hasBlankCells then IllegalArgument(s"$INVALID_HEADER. ${reason("blank")}").invalidNec
      else if hasNumericCells then IllegalArgument(s"$INVALID_HEADER. ${reason("numeric")}").invalidNec
      else if hasBooleanCells then IllegalArgument(s"$INVALID_HEADER. ${reason("boolean")}").invalidNec
      else if hasDateCells then IllegalArgument(s"$INVALID_HEADER. ${reason("date")}").invalidNec
      else if hasNumericFormulaCells then IllegalArgument(s"$INVALID_HEADER. ${reason("numeric formula")}").invalidNec
      else if startsWithSeparator then IllegalArgument(s"$INVALID_HEADER. Separators not allowed at the beggining of the header.").invalidNec
      else if hasMultipleContiguousSeparators then IllegalArgument(s"$INVALID_HEADER. Multiple contiguous separators not allowed.").invalidNec
      else poiHeaderRow.validNec

    private def isNull: Boolean = poiHeaderRow == null

    private def isEmpty: Boolean = logicalCells.forall(_.isEmpty)

    private def logicalCells: Seq[XSSFCell] =
      (0 until poiHeaderRow.getLastCellNum)
        .map(index ⇒ poiHeaderRow.getCell(index, CREATE_NULL_AS_BLANK))

    private def hasBlankCells: Boolean =
      logicalCells.sliding(2)
        .foreach {
          case IndexedSeq(first: XSSFCell, last: XSSFCell) ⇒
            if first.isBlank && last.isNotBlank then
              return true
        }
      false

    private def hasNumericCells: Boolean =
      logicalCells.exists(cell ⇒ cell.getCellType == NUMERIC && !DateUtil.isCellDateFormatted(cell))

    private def hasBooleanCells: Boolean =
      logicalCells.exists(_.getCellType == BOOLEAN)

    private def hasDateCells: Boolean =
      logicalCells.exists(cell ⇒ cell.getCellType == NUMERIC && DateUtil.isCellDateFormatted(cell))

    private def hasNumericFormulaCells: Boolean =
      logicalCells.exists(cell ⇒ cell.getCellType == FORMULA && cell.getCachedFormulaResultType == NUMERIC)

    private def startsWithSeparator: Boolean = logicalCells.head.isSeparator

    private def hasMultipleContiguousSeparators: Boolean = {
      logicalCells.sliding(2)
        .foreach {
          case IndexedSeq(first: XSSFCell, last: XSSFCell) ⇒
            if first.isSeparator && last.isSeparator then
              return true
        }
      false
    }

    private def nonBlankCells: Seq[String] = for {
      cell ← logicalCells if cell.isNotBlank
    } yield cell.getStringCellValue

  extension (cell: XSSFCell)

    private def isEmpty: Boolean =
      cell.getCellType match
        case BLANK ⇒ true
        case STRING ⇒ cell.getStringCellValue.isBlank
        case FORMULA if cell.getCachedFormulaResultType == STRING ⇒ cell.getStringCellValue.isBlank
        case _ ⇒ false

    private def isBlank: Boolean =
      isEmpty && isNotSeparator

    private def isNotSeparator: Boolean =
      !isSeparator

    private def isSeparator: Boolean =
      cell.backgroundColor.nonEmpty

    private def backgroundColor: String =
      Option(cell.getCellStyle.getFillForegroundColorColor)
        .map(_.getRGBWithTint
          .map(b => b & 0xFF)
          .mkString(",")
        ).getOrElse("")

    private def isNotBlank: Boolean = !isBlank