package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.{DataFormatter, DateUtil, Row}
import org.apache.poi.ss.usermodel.CellType.{BLANK, FORMULA, NUMERIC, STRING}
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.XSSFCell

import scala.util.{Failure, Try}

// TODO address can be a regex
// TODO `type` can be an enum
// TODO fontColor can be a RGB array ("0,0,0")
// TODO backgroundColor can be a RGB array ("0,0,0")
case class Cell private(address: String, value: String, `type`: String, mask: String, formula: String, note: String, fontColor: String, backgroundColor: String):
  def isEmpty: Boolean = value.isBlank

  def isNotEmpty: Boolean = !isEmpty

  def asInt: Option[Int] = value.toIntOption

  def asDouble: Option[Double] = value.toDoubleOption

  def isCurrency: Boolean = `type` == "CURRENCY"

  def isNotCurrency: Boolean = !isCurrency

// TODO Replace Try + exceptions with Validated
object Cell:
  private val CURRENCY_FORMAT_IDS = Seq(5, 6, 7, 8, 42, 44, 164, 165)
  private val SHORT_DATE_FORMAT_ID = 14
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
      case NUMERIC if poiCell.isInteger && !poiCell.isCurrency ⇒ poiCell.getNumericCellValue.toInt.toString
      case NUMERIC ⇒ poiCell.getNumericCellValue.toString
      case FORMULA if poiCell.getCachedFormulaResultType == STRING =>
        val cellValue = poiCell.getStringCellValue
        val currencyValue = raw"^([-\u00AD]?)(R?\p{Sc})(\s*)(([1-9]\d{0,2}([,.]\d{3})*)|(([1-9]\d*)?\d))([,.]\d\d)?".r.findFirstIn(cellValue)

        cellValue.toIntOption.map(_.toString)
          .orElse(cellValue.replace(",", ".").toDoubleOption.map(_.toString))
          .orElse(currencyValue.flatMap(_.split(raw"\p{Sc}")(1).trim.replace(",", ".").toDoubleOption.map(_.toString)))
          .getOrElse(cellValue)
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC =>
        if poiCell.getNumericCellValue.isValidInt && !poiCell.isCurrency then
          poiCell.getNumericCellValue.toInt.toString
        else
          poiCell.getNumericCellValue.toString
      case _ ⇒
        val cellValue = poiCell.getStringCellValue
        val currencyValue = raw"^([-\u00AD]?)(R?\p{Sc})(\s*)(([1-9]\d{0,2}([,.]\d{3})*)|(([1-9]\d*)?\d))([,.]\d\d)?".r.findFirstIn(cellValue)

        cellValue.toIntOption.map(_.toString)
          .orElse(cellValue.replace(",", ".").toDoubleOption.map(_.toString))
          .orElse(currencyValue.flatMap(_.split(raw"\p{Sc}")(1).trim.replace(",", ".").toDoubleOption.map(_.toString)))
          .getOrElse(cellValue)

    // TODO Do we actually need two conditions? Case affirmative, are we missing tests for the combination of possible results for bothof them ?
    private def isDate: Boolean =
      DateUtil.isCellDateFormatted(poiCell) &&
        poiCell.getCellStyle.getDataFormat == SHORT_DATE_FORMAT_ID

    // TODO Do we actually need two conditions? Case affirmative, are we missing tests for the combination of possible results for bothof them ?
    private def isCurrency: Boolean =
      CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat) &&
        poiCell.getCellStyle.getDataFormatString.contains("$")

    private def isInteger: Boolean = poiCell.getNumericCellValue.isValidInt

    private def `type`: String = poiCell.getCellType match
      case BLANK ⇒ "STRING"
      /*
      This branch assumes Text cells are treated as Text even if they contain only numbers.
      This is what Excel advertises but it looks like POI does not respect that and, therefore, this branch is being ignored for that type of column, at least with the current version of POI.
      As this looks like a bug in POI, in order to be on the safe side and, trying to be future-proof, we'll leave this here so, if this is eventually "corrected" in future versions of POI, we're prepared.
      */
      case STRING ⇒
        val cellValue = poiCell.getStringCellValue
        cellValue.toIntOption.map(_ ⇒ "INTEGER")
        .orElse(cellValue.replace(",", ".").toDoubleOption.map(_ ⇒ "DOUBLE"))
        .orElse(raw"^([-\u00AD]?)(R?\p{Sc})(\s*)(([1-9]\d{0,2}([,.]\d{3})*)|(([1-9]\d*)?\d))([,.]\d\d)?".r.findFirstIn(cellValue).map(_ ⇒ "CURRENCY"))
        .getOrElse("STRING")
      case FORMULA if poiCell.getCachedFormulaResultType == STRING =>
        val cellValue = poiCell.getStringCellValue
        cellValue.toIntOption.map(_ ⇒ "INTEGER")
          .orElse(cellValue.replace(",", ".").toDoubleOption.map(_ ⇒ "DOUBLE"))
          .orElse(raw"^([-\u00AD]?)(R?\p{Sc})(\s*)(([1-9]\d{0,2}([,.]\d{3})*)|(([1-9]\d*)?\d))([,.]\d\d)?".r.findFirstIn(cellValue).map(_ ⇒ "CURRENCY"))
          .getOrElse("STRING")
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC && poiCell.isInteger && !poiCell.isDate && !poiCell.isCurrency => "INTEGER"
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC && !poiCell.isDate && !poiCell.isCurrency => "DOUBLE"
      case FORMULA if poiCell.getCachedFormulaResultType == NUMERIC && !poiCell.isDate => "CURRENCY"
      case NUMERIC if poiCell.isInteger && !poiCell.isDate && !poiCell.isCurrency ⇒ "INTEGER"
      case NUMERIC if !poiCell.isDate && !poiCell.isCurrency ⇒ "DOUBLE"
      case NUMERIC if !poiCell.isDate ⇒ "CURRENCY"
      case t ⇒ t.toString

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