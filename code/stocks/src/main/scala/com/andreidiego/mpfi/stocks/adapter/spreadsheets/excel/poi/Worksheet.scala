package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import cats.data.ValidatedNec
import cats.kernel.Semigroup
import cats.syntax.validated.*
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFSheet}

// TODO Header and Groups does not seem to fit at this level in the architecture. They are higher level concepts and seem to be closer the domain in the sense that they reflect a specific/constrained way of organizing spreadsheets
case class Worksheet private(name: String, header: Header, lines: Seq[Line], groups: Seq[Seq[Line]])

object Worksheet:
  enum WorksheetError(message: String):
    case IllegalArgument(message: String) extends WorksheetError(message)

  import WorksheetError.*

  type Error = WorksheetError | Header.Error | Line.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]

  given Semigroup[Seq[Line]] = (x, y) => if x == y then x else x ++: y

  // FIXME Errors aren't being combined/accumulated. It becomes clear when you have a big Worksheet full of errors and the only errors you're notified of is that there's an invalid blank cell in the header.
  def from(poiWorksheet: XSSFSheet): ErrorsOr[Worksheet] = if poiWorksheet == null then
    IllegalArgument(s"Invalid worksheet found: '$poiWorksheet'").invalidNec
  else rowZeroFrom(poiWorksheet).andThen(poiHeaderRow ⇒ {
    Header.from(poiHeaderRow).andThen(header ⇒ {
      val numberOfColumns: Int = header.columnNames.size

      linesFrom(poiWorksheet.withEmptyRows(numberOfColumns))(numberOfColumns)
        .andThen(validated(_)(poiWorksheet.getSheetName))
        .map(validatedLines ⇒ Worksheet(poiWorksheet.getSheetName, header, validatedLines, grouped(validatedLines)))
    })
  })

  private def rowZeroFrom(poiWorksheet: XSSFSheet): ErrorsOr[XSSFRow] =
    if poiWorksheet.isEmpty then
      IllegalArgument(s"It looks like ${poiWorksheet.getSheetName} is completely empty.").invalidNec
    else poiWorksheet.getRowOrCreateEmpty(0).validNec

  // TODO This looks like sequencing so maybe, cats 'Traverse' has something simpler for us
  private def linesFrom(rows: Seq[XSSFRow])(headerSize: Int): ErrorsOr[Seq[Line]] =
    rows.map(Line.from(_, headerSize))
      .map(_.map(Seq(_)))
      .reduce(_ combine _)
      .map {
        _.reverse
          .dropWhile(_.isEmpty)
          .reverse
      }

  private def validated(lines: Seq[Line])(sheetName: String): ErrorsOr[Seq[Line]] =
    assertRegularLinesFoundIn(lines)(sheetName)
      .andThen {
        assertNoMoreThanOneEmptyLineRightAfterHeaderIn(_)(sheetName) combine assertNoThreeEmptyLinesBetweenRegularLinesOf(lines)(sheetName)
      }

  private def assertRegularLinesFoundIn(lines: Seq[Line])(sheetName: String): ErrorsOr[Seq[Line]] =
    if lines.tail.isEmpty then
      IllegalArgument(s"$sheetName does not seem to have lines other than the header.").invalidNec
    else lines.validNec

  private def assertNoMoreThanOneEmptyLineRightAfterHeaderIn(lines: Seq[Line])(sheetName: String): ErrorsOr[Seq[Line]] =
    if lines.tail.take(2).forall(_.isEmpty) then
      IllegalArgument(s"Irregular empty line interval found right after the header of $sheetName. Only one empty line is allowed in this position.").invalidNec
    else lines.validNec

  private def assertNoThreeEmptyLinesBetweenRegularLinesOf(lines: Seq[Line])(sheetName: String): ErrorsOr[Seq[Line]] =
    lines.sliding(4)
      .foldLeft(false) { (_, value) ⇒
        value match
          case Seq(_, _) ⇒ false
          case Seq(_, _, _) ⇒ false
          case Seq(first, second, third, fourth) ⇒
            if first.isEmpty && second.isEmpty && third.isEmpty && fourth.isNotEmpty then
              return IllegalArgument(s"Irregular empty line interval (${first.number}:${third.number}) found between the regular lines of $sheetName. No more than two empty lines are allowed in this position.").invalidNec
            else false
      }
    lines.validNec

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
      Option(poiWorksheet.getRow(rowNumber)).getOrElse(poiWorksheet.createRow(0))

    private def withEmptyRows(size: Int): Seq[XSSFRow] =

      def newRow(index: Int, size: Int): XSSFRow =
        val row = poiWorksheet.createRow(index)

        (0 until size)
          .map(index ⇒ row.createCell(index))

        row

      (0 to poiWorksheet.getLastRowNum)
        .map(index ⇒ Option(poiWorksheet.getRow(index)).getOrElse(newRow(index, size)))

    private def isEmpty: Boolean = poiWorksheet.getLastRowNum == -1