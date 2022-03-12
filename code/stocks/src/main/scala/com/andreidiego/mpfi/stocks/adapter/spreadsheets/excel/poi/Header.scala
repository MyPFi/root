package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow}

import scala.util.{Failure, Try}

case class Header private(columnNames: Seq[String])

// TODO Replace Try + exceptions with Either
object Header:

  def from(poiHeaderRow: XSSFRow): Try[Header] = for {
    validatedPOIHeaderRow ← poiHeaderRow.validated
  } yield Header(validatedPOIHeaderRow.nonBlankCells)

  extension (poiHeaderRow: XSSFRow)

    private def validated: Try[XSSFRow] = Try {
      if isEmpty then
        throw new IllegalArgumentException("Header is empty.")
      else if hasBlankCells then
        throw new IllegalArgumentException("An illegal blank cell was found in the header.")
      else if startsWithSeparator then
        throw new IllegalArgumentException("Separators not allowed at the beggining of the header.")
      else if hasMultipleContiguousSeparators then
        throw new IllegalArgumentException("Multiple contiguous separators not allowed.")
      else poiHeaderRow

    } recoverWith {
      case e ⇒ Failure {
        new IllegalArgumentException(s"Worksheet does not seem to have a valid header.", e)
      }
    }

    private def isEmpty: Boolean =
      logicalCells.forall(_.isEmpty)

    private def logicalCells: Seq[XSSFCell] =
      (0 until poiHeaderRow.getLastCellNum)
        .map(index ⇒ poiHeaderRow.getCell(index, CREATE_NULL_AS_BLANK))

    private def hasBlankCells: Boolean = {
      logicalCells.sliding(2)
        .foreach {
          case IndexedSeq(first: XSSFCell, last: XSSFCell) ⇒
            if first.isBlank && last.isNotBlank then
              return true
        }
      false
    }

    private def startsWithSeparator: Boolean =
      logicalCells.head.isSeparator

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

  // TODO What about numeric / numeric formula cells?
    private def isEmpty: Boolean =
      cell.getStringCellValue.isBlank

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