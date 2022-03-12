package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.CellType.{FORMULA, NUMERIC}
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

case class Line private(cells: Seq[(String, String, String, String, String, String, String, String)])

// TODO Replace Try + exceptions with Either
object Line:
  val CURRENCY_FORMAT_ID = 8
  val SHORT_DATE_FORMAT_ID = 14
  val PT_BR_DATE_FORMAT = "dd/MM/yyyy"

  val formatter = new DataFormatter(LocaleUtil.getUserLocale)

  def from(poiRow: XSSFRow): Try[Line] = for {
    validatedPOIRow ← poiRow.validated
  } yield Line(validatedPOIRow.cells)

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

    private def cells: Seq[(String, String, String, String, String, String, String, String)] =
      (0 until poiRow.getLastCellNum)
        .map(index ⇒ poiRow.getCell(index, CREATE_NULL_AS_BLANK))
        .map(cell ⇒ (cell.address, cell.value, cell.`type`, cell.mask, cell.formula, cell.note, cell.fontColor, cell.backgroundColor)
        )

  extension (poiCell: XSSFCell)

    private def address: String = poiCell.getAddress.toString

    private def value: String = poiCell.getCellType match
      case NUMERIC if DateUtil.isCellDateFormatted(poiCell) =>
        formatter.addFormat("m/d/yy", new java.text.SimpleDateFormat(PT_BR_DATE_FORMAT))
        formatter.formatCellValue(poiCell)
      case NUMERIC if poiCell.getNumericCellValue.isValidInt ⇒
        poiCell.getNumericCellValue.toInt.toString
      case NUMERIC ⇒ poiCell.getNumericCellValue.toString
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC =>
        if poiCell.getNumericCellValue.isValidInt then
          poiCell.getNumericCellValue.toInt.toString
        else
          poiCell.getNumericCellValue.toString
      case _ ⇒ poiCell.getStringCellValue

    private def `type`: String = poiCell.getCellType.toString

    private def mask: String =
      val mask = poiCell.getCellStyle.getDataFormatString

      if mask == "General" then "" else mask

    private def formula: String =
      if poiCell.getCellType == FORMULA then poiCell.getCellFormula else ""

    private def note: String = Option(poiCell.getCellComment).map(_.getString.toString).getOrElse("")

    private def fontColor: String =
      if value.isBlank then ""
      else Option(poiCell.getCellStyle.getFont.getXSSFColor)
        .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
        .getOrElse("0,0,0")

    private def backgroundColor: String = Option(poiCell.getCellStyle.getFillForegroundColorColor)
      .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
      .getOrElse("")