package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow, XSSFSheet}

import scala.util.Try

// TODO Header and Groups does not seem to fit at this level in the architecture. They are higher level concepts and seem to be closer the domain in the sense that they reflect a specific/constrained way of organizing spreadsheets
case class Worksheet private(header: Header, lines: Seq[Line], groups: Seq[Seq[Line]])

// TODO Replace Try + exceptions with Either
object Worksheet:
  private val AT_ASCII_CHAR = 64

  //  TODO In a second pass, this looks like a sweet spot for cats.Validated
  def from(poiWorksheet: XSSFSheet): Try[Worksheet] = for {
    poiHeaderRow ← rowZeroFrom(poiWorksheet)
    header ← Header.from(poiHeaderRow)
    numberOfColumns = header.columnNames.size
    lines ← linesFrom(poiWorksheet.withEmptyRows(numberOfColumns))
    validatedLines ← validated(lines)(numberOfColumns, poiWorksheet.getSheetName)
  } yield Worksheet(header, validatedLines, grouped(validatedLines))

  private def rowZeroFrom(poiWorksheet: XSSFSheet) = Try {
    if poiWorksheet.isEmpty then
      throw new IllegalArgumentException(s"It looks like ${poiWorksheet.getSheetName} is completely empty.")
    else poiWorksheet.getRowOrCreateEmpty(0)
  }

  private def validated(lines: Seq[Line])(headerSize: Int, sheetName: String): Try[Seq[Line]] = for {
    a ← assertRegularLinesFoundIn(lines)(sheetName)
    b ← assertNoMoreThanOneEmptyLineRightAfterHeaderIn(a)(sheetName)
    c ← assertNoContentFoundBeyondTheHeadersLimitIn(b)(headerSize, sheetName)
    d ← assertNoThreeEmptyLinesBetweenRegularLinesOf(c)(sheetName)
  } yield d

  private def assertRegularLinesFoundIn(lines: Seq[Line])(sheetName: String): Try[Seq[Line]] = Try {
    if lines.tail.isEmpty then
      throw new IllegalArgumentException(s"$sheetName does not seem to have lines other than the header.")

    lines
  }

  private def assertNoMoreThanOneEmptyLineRightAfterHeaderIn(lines: Seq[Line])(sheetName: String): Try[Seq[Line]] = Try {
    if lines.tail.take(2).forall(isEmpty) then
      throw new IllegalArgumentException(s"Irregular empty line interval found right after the header of $sheetName. Only one empty line is allowed in this position.")

    lines
  }

  private def assertNoContentFoundBeyondTheHeadersLimitIn(lines: Seq[Line])(headerSize: Int, sheetName: String): Try[Seq[Line]] = Try {
    findCellBeyondTheHeadersLimitIn(lines)(headerSize)
      .map(illegalCellInfo ⇒ throw new IllegalArgumentException(
        s"A cell (${illegalCellInfo._1}) with value (${illegalCellInfo._2}) was found in $sheetName beyond the limits imposed by the header (${(AT_ASCII_CHAR + headerSize).toChar} column)."
      ))

    lines
  }

  private def findCellBeyondTheHeadersLimitIn(lines: Seq[Line])(headerSize: Int): Option[(String, String)] =
    lines.tail
      .find(_.size > headerSize)
      .map(line ⇒ line.lastNCells(line.size - headerSize))
      .flatMap(lastNCells ⇒ lastNCells.find(cell => cell.isNotEmpty)
        .map(nonEmptyCell ⇒ (nonEmptyCell.address, nonEmptyCell.value))
      )

  private def assertNoThreeEmptyLinesBetweenRegularLinesOf(lines: Seq[Line])(sheetName: String): Try[Seq[Line]] = Try {
    lines.sliding(4)
      .foldLeft(false) { (found, value) ⇒
        value match
          case Seq(first, second) ⇒ false
          case Seq(first, second, third) ⇒ false
          case Seq(first, second, third, fourth) ⇒
            if first.isEmpty && second.isEmpty && third.isEmpty && fourth.isNotEmpty then
              throw new IllegalArgumentException(s"Irregular empty line interval found between the regular lines of $sheetName. No more than two empty lines are allowed in this position.")
            else false
      }

    lines
  }

  private def linesFrom(rows: Seq[XSSFRow]): Try[Seq[Line]] = Try(rows.map(row ⇒ Line.from(row).get))

  private def grouped(lines: Seq[Line]): Seq[Seq[Line]] = lines
    .drop(1)
    .dropWhile(_.isEmpty)
    .foldLeft(Seq(Seq[Line]())) { (acc, line) ⇒
      if line.isNotEmpty then (line +: acc.head) +: acc.tail
      else Seq() +: acc
    }
    .map(_.reverse)
    .reverse

  extension (poiWorksheet: XSSFSheet)

    private def getRowOrCreateEmpty(rowNumber: Int): XSSFRow =
      Option(poiWorksheet.getRow(0)).getOrElse(poiWorksheet.createRow(0))

    private def withEmptyRows(size: Int): Seq[XSSFRow] =

      def newRow(index: Int, size: Int): XSSFRow =
        val row = poiWorksheet.createRow(index)

        (0 until size)
          .map(index ⇒ row.createCell(index))

        row

      (0 to poiWorksheet.getLastRowNum)
        .map(index ⇒ Option(poiWorksheet.getRow(index)).getOrElse(newRow(index, size)))

    private def isEmpty: Boolean = poiWorksheet.getLastRowNum == -1

  extension (line: Line)

    private def lastNCells(n: Int): Seq[Cell] = cells.takeRight(n)

    private def size: Int = cells.size

    private def cells: Seq[Cell] = line.cells

    private def isEmpty: Boolean = cells.forall(_.isEmpty)

    private def isNotEmpty: Boolean = !isEmpty

  extension (cell: Cell)

    private def isEmpty: Boolean = cell.value.isBlank

    private def isNotEmpty: Boolean = !isEmpty