package com.andreidiego.mpfi.stocks.exploratory.spreadsheets.design

import org.apache.poi.ss.usermodel.{DataFormatter, DateUtil, Row, Workbook}
import org.apache.poi.ss.usermodel.CellType.*
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.{CREATE_NULL_AS_BLANK, RETURN_NULL_AND_BLANK}
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class POIExcelExtractor(workbook: Workbook):
  val GENERAL_FORMAT_ID = 0
  val CURRENCY_FORMAT_ID = 8
  val SHORT_DATE_FORMAT_ID = 14
  val PT_BR_DATE_FORMAT = "dd/MM/yyyy"

  val formatter = new DataFormatter(LocaleUtil.getUserLocale)

  def headerOf(sheetName: String): Try[Seq[String]] =
    def validate(row: Row): Unit =
      if isEmpty(row) then
        throw new IllegalStateException("Header is empty.")
      else if hasBlankCells(row) then
        throw new IllegalStateException("An illegal blank cell was found.")

    def isEmpty(row: Row) =
      logicalCellsFrom(row).forall(_.isEmpty)

    def logicalCellsFrom(row: Row) =
      (0 to row.getLastCellNum)
        .map(index ⇒ row.getCell(index, CREATE_NULL_AS_BLANK).asInstanceOf[XSSFCell])

    def hasBlankCells(row: Row): Boolean =
      logicalCellsFrom(row)
        .sliding(2)
        .foreach {
          case IndexedSeq(first, last) ⇒
            if first.isBlank && last.isNotBlank then
              return true
        }
      false

    def nonBlankCellsFrom(row: Row) =
      for {
        cell ← logicalCellsFrom(row) if cell.isNotBlank
      } yield cell.getStringCellValue

    Try {
      getRowFrom(sheetName)(0).map { row =>
        validate(row)
        nonBlankCellsFrom(row)
      }.get

    } recoverWith {
      case e ⇒ Failure {
        new IllegalStateException(s"$sheetName does not seem to have a valid header.", e)
      }
    }

  def line(index: Int)(sheetName: String): Try[Seq[Map[String, String]]] = {
    // val lastLineNumber = lastLineNumberIn(sheetName)
    Try {
      assert(index > 1)
      // assert(index <= lastLineNumber)

      getRowFrom(sheetName)(index - 1).map(_.cells(sheetName)).get
    } recoverWith {
      case e if index <= 1 ⇒ Failure {
        new IllegalArgumentException(s"Invalid line number: $index. Regular lines start with 2.", e)
      }
      // case e if index > lastLineNumber ⇒ Failure {
      //   new IllegalArgumentException(s"Invalid line number: $index. Last line number in $sheetName is $lastLineNumber.", e)
      // }
    }
  }

  def lastLineNumberIn(sheetName: String): Int =
    getSheet(sheetName)
      .sliding(4)
      .foldLeft(0) { (lastLineNumber: Int, value: Iterable[Row]) ⇒
        value match
          case Seq(first: Row) ⇒ 1

          case Seq(first: Row, second: Row) ⇒
            if second.isEmpty(sheetName) then 1 else 2

          case Seq(first: Row, second: Row, third: Row)
            if third.isNotEmpty(sheetName) ⇒ 3

          case Seq(first: Row, second: Row, third: Row) ⇒
            if second.isNotEmpty(sheetName) then 2 else 1

          case Seq(first: Row, second: Row, third: Row, fourth: Row)
            if second.getRowNum == 1 &&
              second.isEmpty(sheetName) &&
              third.isEmpty(sheetName) ⇒ 1

          case Seq(first: Row, second: Row, third: Row, fourth: Row)
            if first.isNotEmpty(sheetName) &&
              second.isEmpty(sheetName) &&
              third.isEmpty(sheetName) &&
              fourth.isEmpty(sheetName) ⇒ return lastLineNumber

          case Seq(first: Row, second: Row, third: Row, fourth: Row)
            if fourth.isNotEmpty(sheetName) ⇒ fourth.getRowNum + 1

          case Seq(first: Row, second: Row, third: Row, fourth: Row)
            if third.isNotEmpty(sheetName) ⇒ third.getRowNum + 1

          case Seq(first: Row, second: Row, third: Row, fourth: Row)
            if second.isNotEmpty(sheetName) ⇒ second.getRowNum + 1

          case _ ⇒ -1
      }

  private def getRowFrom(sheetName: String)(index: Int) = Try {
    getSheet(sheetName)(index)
  } recoverWith {
    case e: IndexOutOfBoundsException ⇒ Failure {
      new IllegalStateException(s"Row #$index does not exist in worksheet $sheetName.")
    }
  }

  private def getSheet(sheetName: String) =
    val sheet = workbook.getSheet(sheetName)

    (0 to sheet.getLastRowNum)
      .map(index ⇒ Option(sheet.getRow(index)).getOrElse(sheet.createRow(index)))

  extension (row: Row)

    private def cells(sheetName: String): Seq[Map[String, String]] =
      headerOf(sheetName).get.indices
        .map(columIndex ⇒ row.getCell(columIndex, CREATE_NULL_AS_BLANK).asInstanceOf[XSSFCell])
        .map(_.properties)

    private def isEmpty(sheetName: String): Boolean =
      cells(sheetName).forall(cell ⇒ cell("CONTENT").isBlank)

    private def isNotEmpty(sheetName: String): Boolean = !isEmpty(sheetName)

  extension (cell: XSSFCell)

    private def content: String = cell.getCellType match
      case BOOLEAN => cell.getBooleanCellValue.toString
      case NUMERIC if cell.isDate =>
        formatter.addFormat("m/d/yy", new java.text.SimpleDateFormat(PT_BR_DATE_FORMAT))
        formatter.formatCellValue(cell)
      case NUMERIC if cell.isCurrency => f"${cell.getNumericCellValue}%1.2f"
      case NUMERIC if cell.isRegularNumber =>
        if cell.getNumericCellValue.isValidInt then cell.getNumericCellValue.toInt.toString
        else f"${cell.getNumericCellValue}%1.2f"
      case FORMULA if cell.getCachedFormulaResultType == NUMERIC =>
        f"${cell.getNumericCellValue}%1.2f"
      case FORMULA => cell.getStringCellValue
      case STRING => cell.getStringCellValue
      case BLANK => cell.getStringCellValue
      case _ => ""

    private def properties: Map[String, String] = Map(
      "ADDRESS" → cell.address,
      "CONTENT" → cell.content,
      "TYPE" → cell.`type`,
      "FONT-COLOR" → cell.fontColor,
      "BACKGROUND-COLOR" → cell.backgroundColor,
      "FORMULA" → cell.formula,
      "NOTE" → cell.note
    )

    private def address: String =
      cell.getAddress.toString

    private def `type`: String =
      cell.getCellType.toString

    private def fontColor: String =
      Option(cell.getCellStyle.getFont.getXSSFColor)
        .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
        .getOrElse("0,0,0")

    private def backgroundColor: String =
      Option(cell.getCellStyle.getFillForegroundColorColor)
        .map(_.getRGBWithTint
          .map(b => b & 0xFF)
          .mkString(",")
        ).getOrElse("")

    private def formula: String = Try {
      cell.getCellFormula
    }.getOrElse("")

    private def note: String = Option(cell.getCellComment)
      .map(_.getString.toString)
      .getOrElse("")

    // TODO What about numeric / numeric formula cells?
    private def isEmpty: Boolean =
      cell.content.isBlank

    private def isBlank: Boolean =
      cell.content.isBlank && cell.isNotSeparator

    private def isNotBlank: Boolean = !isBlank

    private def isNotSeparator: Boolean =
      cell.backgroundColor.isEmpty

    private def isRegularNumber: Boolean = cell.getCellStyle.getDataFormat == GENERAL_FORMAT_ID

    private def isCurrency: Boolean =
      cell.getCellStyle.getDataFormat == CURRENCY_FORMAT_ID &&
        cell.getCellStyle.getDataFormatString.contains("$")

    private def isDate: Boolean =
      DateUtil.isCellDateFormatted(cell) &&
        cell.getCellStyle.getDataFormat == SHORT_DATE_FORMAT_ID