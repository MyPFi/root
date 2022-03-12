package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.{DataFormatter, DateUtil, Row}
import org.apache.poi.ss.usermodel.CellType.{FORMULA, NUMERIC}
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.XSSFCell

import scala.util.{Failure, Try}

// TODO address can be a regex
// TODO `type` can be an enum
// TODO fontColor can be a RGB array ("0,0,0")
// TODO backgroundColor can be a RGB array ("0,0,0")
case class Cell private(address: String, value: String, `type`: String, mask: String, formula: String, note: String, fontColor: String, backgroundColor: String)

// TODO Replace Try + exceptions with Either
object Cell:
  val CURRENCY_FORMAT_ID = 8
  val SHORT_DATE_FORMAT_ID = 14
  private val PT_BR_DATE_FORMAT = "dd/MM/yyyy"

  private val formatter = new DataFormatter(LocaleUtil.getUserLocale)

  def from(poiCell: XSSFCell): Try[Cell] = for {
    validatedPOICell ← poiCell.validated
  } yield Cell(
    validatedPOICell.address,
    validatedPOICell.value,
    validatedPOICell.`type`,
    validatedPOICell.mask,
    validatedPOICell.formula,
    validatedPOICell.note,
    validatedPOICell.fontColor,
    validatedPOICell.backgroundColor
  )

  extension (poiCell: XSSFCell)

    private def validated: Try[XSSFCell] = Try {
      if poiCell == null then
        throw new IllegalArgumentException(s"Invalid cell found: $poiCell")
      else poiCell
    }

    private def address: String = poiCell.getAddress.toString

    private def value: String = poiCell.getCellType match
      case NUMERIC if poiCell.isDate =>
        formatter.addFormat("m/d/yy", new java.text.SimpleDateFormat(PT_BR_DATE_FORMAT))
        formatter.formatCellValue(poiCell)
      case NUMERIC if poiCell.isCurrency => f"${poiCell.getNumericCellValue}%1.2f"
      case NUMERIC if poiCell.isInteger ⇒ poiCell.getNumericCellValue.toInt.toString
      case NUMERIC ⇒ poiCell.getNumericCellValue.toString
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC =>
        if poiCell.getNumericCellValue.isValidInt then
          poiCell.getNumericCellValue.toInt.toString
        else
          poiCell.getNumericCellValue.toString
      case _ ⇒ poiCell.getStringCellValue

    // TODO Do we actually need two conditions? Case affirmative, are we missing tests for the combination of possible results for bothof them ?
    private def isDate: Boolean =
      DateUtil.isCellDateFormatted(poiCell) &&
        poiCell.getCellStyle.getDataFormat == SHORT_DATE_FORMAT_ID

    // TODO Do we actually need two conditions? Case affirmative, are we missing tests for the combination of possible results for bothof them ?
    private def isCurrency: Boolean =
      poiCell.getCellStyle.getDataFormat == CURRENCY_FORMAT_ID &&
        poiCell.getCellStyle.getDataFormatString.contains("$")

    private def isInteger: Boolean = poiCell.getNumericCellValue.isValidInt

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