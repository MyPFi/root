package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.{DataFormatter, DateUtil}
import org.apache.poi.ss.usermodel.{Row, Cell as POICell}
import org.apache.poi.ss.usermodel.CellType.{FORMULA, NUMERIC}
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.ss.util.CellAddress
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFSheet}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Try}

case class Worksheet private(header: Header, lines: Seq[Line])

// TODO Replace Try + exceptions with Either
object Worksheet:
  private val AT_ASCII_CHAR = 64
  private val CURRENCY_FORMAT_ID = 8
  private val SHORT_DATE_FORMAT_ID = 14
  private val PT_BR_DATE_FORMAT = "dd/MM/yyyy"

  private val formatter = DataFormatter(LocaleUtil.getUserLocale)

  //  TODO In a second pass, this looks like a sweet spot for cats.Validated
  def from(poiWorksheet: XSSFSheet): Try[Worksheet] = for {
    poiHeaderRow ← rowZeroFrom(poiWorksheet)
    header ← Header.from(poiHeaderRow)
    rows ← validated(poiWorksheet.withEmptyRows(header.columnNames.size))(header.columnNames.size, poiWorksheet.getSheetName)
    lines ← linesFrom(rows)
  } yield Worksheet(header, lines)

  private def rowZeroFrom(poiWorksheet: XSSFSheet) = Try {
    if poiWorksheet.isEmpty then
      throw new IllegalArgumentException(s"It looks like ${poiWorksheet.getSheetName} is completely empty.")
    else poiWorksheet.getRowOrCreateEmpty(0)
  }

  private def validated(rows: Seq[XSSFRow])(headerSize: Int, sheetName: String): Try[Seq[XSSFRow]] = for {
    a ← assertRegularLinesFoundIn(rows)(sheetName)
    b ← assertNoMoreThanOneEmptyLineRightAfterHeaderIn(a)(sheetName)
    c ← assertNoContentFoundBeyondTheHeadersLimitIn(b)(headerSize, sheetName)
    d ← assertNoThreeEmptyLinesBetweenRegularLinesOf(c)(sheetName)
  } yield d

  private def assertRegularLinesFoundIn(rows: Seq[XSSFRow])(sheetName: String): Try[Seq[XSSFRow]] = Try {
    if rows.tail.isEmpty then
      throw new IllegalArgumentException(s"$sheetName does not seem to have lines other than the header.")

    rows
  }

  private def assertNoMoreThanOneEmptyLineRightAfterHeaderIn(rows: Seq[XSSFRow])(sheetName: String): Try[Seq[XSSFRow]] = Try {
    if rows.tail.take(2).forall(isEmpty) then
      throw new IllegalArgumentException(s"Irregular empty line interval found right after the header of $sheetName. Only one empty line is allowed in this position.")

    rows
  }

  private def assertNoContentFoundBeyondTheHeadersLimitIn(rows: Seq[XSSFRow])(headerSize: Int, sheetName: String): Try[Seq[XSSFRow]] = Try {
    if rows.tail.exists(_.size > headerSize) then
      val illegalCellInfo = findCellBeyondTheHeadersLimitIn(rows)(headerSize)

      throw new IllegalArgumentException(s"A cell (${illegalCellInfo._1}) with value (${illegalCellInfo._2}) was found in $sheetName beyond the limits imposed by the header (${(AT_ASCII_CHAR + headerSize).toChar} column).")

    rows
  }

  // TODO There's an undesirable dependency between this method and the one above
  // TODO After some time away from this codebase, it is not clear what the dependency claimed above is
  private def findCellBeyondTheHeadersLimitIn(rows: Seq[Row])(headerSize: Int): (String, String) =
    rows.tail
      .find(_.size > headerSize)
      .map(row ⇒ row.asScala.takeRight(row.getLastCellNum - headerSize))
      .flatMap(cells ⇒ cells.find(cell => !cell.isEmpty)
        .map(cell ⇒ (cell.getAddress.toString, cell.value))
      ).get

  private def assertNoThreeEmptyLinesBetweenRegularLinesOf(rows: Seq[XSSFRow])(sheetName: String): Try[Seq[XSSFRow]] = Try {
    rows.sliding(4)
      .foldLeft(false) { (found, value) ⇒
        value match
          case Seq(first, second) ⇒ false
          case Seq(first, second, third) ⇒ false
          case Seq(first, second, third, fourth) ⇒
            if first.isEmpty && second.isEmpty && third.isEmpty && fourth.isNotEmpty then
              throw new IllegalArgumentException(s"Irregular empty line interval found between the regular lines of $sheetName. No more than two empty lines are allowed in this position.")
            else false
      }

    rows
  }

  private def linesFrom(rows: Seq[XSSFRow]): Try[Seq[Line]] = Try(rows.map(row ⇒ Line.from(row).get))

  extension (poiWorksheet: XSSFSheet)

    private def getRowOrCreateEmpty(rowNumber: Int): XSSFRow =
      Option(poiWorksheet.getRow(0)).getOrElse(poiWorksheet.createRow(0))

    private def withEmptyRows(size: Int): Seq[XSSFRow] =
      (0 to poiWorksheet.getLastRowNum)
        .map(index ⇒ Option(poiWorksheet.getRow(index)).getOrElse(newRow(index, size)))

    private def newRow(index: Int, size: Int): XSSFRow =
      val row = poiWorksheet.createRow(index)

      (0 until size)
        .map(index ⇒ row.createCell(index))

      row

    private def isEmpty: Boolean = poiWorksheet.getLastRowNum == -1

  extension (poiRow: Row)

    private def size: Int = cells.size

    // TODO Using the cellIterator may not be the right thing to do. It may not return empty cells which are allowed to exist in regular rows. Let's see.
    private def cells: Seq[POICell] = poiRow.cellIterator().asScala.toSeq

    private def isEmpty: Boolean = cells.forall(_.isEmpty)

    private def isNotEmpty: Boolean = !isEmpty

  extension (poiCell: POICell)

    private def isEmpty: Boolean = value.isBlank

    private def value: String = poiCell.getCellType match
      case NUMERIC if poiCell.isDate =>
        formatter.addFormat("m/d/yy", new java.text.SimpleDateFormat(PT_BR_DATE_FORMAT))
        formatter.formatCellValue(poiCell)
      case NUMERIC if poiCell.isCurrency => f"${poiCell.getNumericCellValue}%1.2f"
      case NUMERIC ⇒ poiCell.getNumericCellValue.toInt.toString
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC =>
        f"${poiCell.getNumericCellValue}%1.2f"
      case _ ⇒ poiCell.getStringCellValue

    private def isDate: Boolean =
      DateUtil.isCellDateFormatted(poiCell) &&
        poiCell.getCellStyle.getDataFormat == SHORT_DATE_FORMAT_ID

    private def isCurrency: Boolean =
      poiCell.getCellStyle.getDataFormat == CURRENCY_FORMAT_ID &&
        poiCell.getCellStyle.getDataFormatString.contains("$")