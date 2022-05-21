package com.andreidiego.mpfi.stocks.adapter.spreadsheets.excel.poi

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

enum CellType:
  case INTEGER, DOUBLE, CURRENCY, DATE, STRING

// TODO address can be a regex
// TODO fontColor can be a RGB array ("0,0,0")
// TODO backgroundColor can be a RGB array ("0,0,0")
case class Cell private(address: String, value: String, `type`: CellType, mask: String, formula: String, note: String, fontColor: String, backgroundColor: String):

  import CellType.*

  def isEmpty: Boolean = value.isBlank

  def isNotEmpty: Boolean = !isEmpty

  def asInt: Option[Int] = if `type` == INTEGER then Some(value.toInt) else None

  def asDouble: Option[Double] =
    if Seq(DOUBLE, INTEGER, CURRENCY) contains `type` then Some(value.toDouble)
    else None

  def isCurrency: Boolean = `type` == CURRENCY

  def isNotCurrency: Boolean = !isCurrency

  def isDate: Boolean = `type` == DATE

  def isNotDate: Boolean = !isDate

  def isFormula: Boolean = !formula.isBlank

  def isNotFormula: Boolean = !isFormula

  // TODO Add test for invalid date
  def asLocalDate: Option[LocalDate] =
    if `type` == DATE then Try(LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))).toOption
    else None

object Cell:
  enum CellError(message: String):
    case IllegalArgument(message: String) extends CellError(message)

  import org.apache.poi.xssf.usermodel.XSSFCell
  import org.apache.poi.ss.usermodel.CellType.{ERROR as POI_ERROR, BLANK, FORMULA, NUMERIC, STRING as POI_STRING}
  import scala.annotation.targetName
  import cats.data.ValidatedNec
  import cats.syntax.validated.*
  import CellType.*
  import CellError.*

  type Error = CellError
  type ErrorsOr[A] = ValidatedNec[Error, A]
  private val CURRENCY_FORMAT_IDS = Seq(5, 6, 7, 8, 42, 44)
  private val SHORT_DATE_FORMAT_IDS = Seq(14, 15, 16, 17, 22)
  private val PT_BR_DATE_FORMAT = "dd/MM/yyyy"

  given Conversion[Option[String], Boolean] = _.isDefined

  def from(poiCell: XSSFCell): ErrorsOr[Cell] =
    poiCell.validated
      .map(validatedPOICell ⇒ Cell(
        validatedPOICell.address,
        validatedPOICell.value,
        validatedPOICell.`type`,
        validatedPOICell.mask,
        validatedPOICell.formula,
        validatedPOICell.note,
        validatedPOICell.fontColor,
        validatedPOICell.backgroundColor
      ))

  extension (poiCell: XSSFCell)

    private def validated: ErrorsOr[XSSFCell] =
      if `null` then
        IllegalArgument(s"Invalid cell found: $poiCell").invalidNec
      // In hindsight, 'Cell' doesn't look like the right level for the following validations.
      // TODO The following should be a 'String'
      else if stringDate && invalidDate then
        IllegalArgument(s"Invalid cell found. Cell '$address' contains an invalid date: '${poiCell.getStringCellValue}'.").invalidNec
      else poiCell.validNec

    private def address: String = poiCell.getAddress.toString

    // TODO There is clearly a design flaw in the relationship between 'value' and 'type' methods.
    // Initially, I thought 'value' would use 'type' but this turned out to be not possible.
    // It looks like it will be the other way around but this looks like a major change that will be postponed for now.
    // Right now, 'value' and 'type' are being handled independently when the're a clear dependency between them. 
    // This dependency is demonstrated by the fact that every change to one entails a change to the other and that has to be carefully coordinated which is a big smell.
    private def value: String =
      if error then poiCell.getErrorCellString
      else if numericDate && !negative then DateTimeFormatter.ofPattern(PT_BR_DATE_FORMAT).format(poiCell.getLocalDateTimeCellValue)
      else if numericInteger || (numericDate && negative) then poiCell.getNumericCellValue.toInt.toString
      else if numericDouble || numericCurrency then poiCell.getNumericCellValue.toString
      else if stringDouble then poiCell.getStringCellValue.replace(",", ".")
      else if stringCurrency then currencyRegex.findFirstIn(poiCell.getStringCellValue)
        .map(_.split(raw"\p{Sc}")(1).trim.replace(",", "."))
        .getOrElse("0.0")
      else poiCell.getStringCellValue

    private def `type`: CellType = (isInteger || isDouble || isCurrency || isDate || isString).get

    private def mask: String =
      val mask = poiCell.getCellStyle.getDataFormatString

      if mask == "General" then "" else mask

    private def formula: String = if ofFormulaType then poiCell.getCellFormula else ""

    private def note: String = Option(poiCell.getCellComment).map(_.getString.toString).getOrElse("")

    private def fontColor: String =
      if value.isBlank then ""
      else Option(poiCell.getCellStyle.getFont.getXSSFColor)
        .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
        .getOrElse("0,0,0")

    private def backgroundColor: String = Option(poiCell.getCellStyle.getFillForegroundColorColor)
      .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
      .getOrElse("")

    private def `null`: Boolean = poiCell == null

    private def error: Boolean = poiCell.getCellType == FORMULA && poiCell.getCachedFormulaResultType == POI_ERROR

    private def negative: Boolean = poiCell.getNumericCellValue < 0

    private def isInteger: Option[CellType] =
      if numericInteger || stringInteger || (numericDate && negative) then Some(INTEGER)
      else None

    private def isDouble: Option[CellType] =
      if numericDouble || stringDouble then Some(DOUBLE)
      else None

    private def isCurrency: Option[CellType] =
      if numericCurrency || stringCurrency then Some(CURRENCY)
      else None

    private def isDate: Option[CellType] =
      if numericDate || stringDate then Some(DATE)
      else None

    private def isString: Option[CellType] =
      if ofBlankType || !(isInteger || isDouble || isCurrency || isDate) then Some(STRING)
      else None

    private def numericInteger: Boolean = numericCellWithAnInteger || numericFormulaCellWithAnInteger

    private def stringInteger: Boolean = stringCellWithAnInteger || stringFormulaCellWithAnInteger

    private def numericDouble: Boolean = numericCellWithADouble || numericFormulaCellWithADouble

    private def stringDouble: Boolean = stringCellWithADouble || stringFormulaCellWithADouble

    private def numericCurrency: Boolean = numericCellWithACurrency || numericFormulaCellWithACurrency

    private def stringCurrency: Boolean = stringCellContainingCurrencyValue || stringFormulaCellContainingCurrencyValue

    private def numericDate: Boolean = numericCellWithADate || numericFormulaCellWithADate

    private def stringDate: Boolean = stringCellContainingDateValue || stringFormulaCellContainingDateValue

    private def ofBlankType: Boolean = poiCell.getCellType == BLANK

    private def ofStringType: Boolean = poiCell.getCellType == POI_STRING

    private def stringCellWithAnInteger: Boolean =
      ofStringType && poiCell.getStringCellValue.toIntOption.isDefined

    private def stringCellWithADouble: Boolean = ofStringType &&
      poiCell.getStringCellValue.toIntOption.isEmpty &&
      poiCell.getStringCellValue.replace(",", ".").toDoubleOption.isDefined

    private def stringCellContainingCurrencyValue: Boolean =
      ofStringType && currencyRegex.findFirstIn(poiCell.getStringCellValue).isDefined

    private def stringCellContainingDateValue: Boolean =
      ofStringType && dateRegex.findFirstIn(poiCell.getStringCellValue).isDefined

    private def ofNumericType: Boolean = poiCell.getCellType == NUMERIC

    // TODO Add test for a cell that contains a double but is formatted with no decimal positions
    private def numericCellWithAnInteger: Boolean = ofNumericType &&
      (poiCell.getNumericCellValue.isValidInt /*|| doubleFormattedAsInt*/) &&
      !(CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat) ||
        SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat))

    // TODO Add test for a cell that contains an integer but is formatted with decimal positions
    private def numericCellWithADouble: Boolean = ofNumericType &&
      !poiCell.getNumericCellValue.isValidInt &&
      !(CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat) ||
        SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat))

    private def numericCellWithACurrency: Boolean =
      ofNumericType && CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat)

    private def numericCellWithADate: Boolean =
      ofNumericType && SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat)

    private def ofFormulaType: Boolean = poiCell.getCellType == FORMULA

    private def stringFormula: Boolean =
      ofFormulaType && poiCell.getCachedFormulaResultType == POI_STRING

    private def stringFormulaCellWithAnInteger: Boolean =
      stringFormula && poiCell.getStringCellValue.toIntOption.isDefined

    private def stringFormulaCellWithADouble: Boolean = stringFormula &&
      poiCell.getStringCellValue.toIntOption.isEmpty &&
      poiCell.getStringCellValue.replace(",", ".").toDoubleOption.isDefined

    private def stringFormulaCellContainingCurrencyValue: Boolean =
      stringFormula && currencyRegex.findFirstIn(poiCell.getStringCellValue).isDefined

    private def stringFormulaCellContainingDateValue: Boolean =
      stringFormula && dateRegex.findFirstIn(poiCell.getStringCellValue).isDefined

    private def numericFormula: Boolean =
      ofFormulaType && poiCell.getCachedFormulaResultType == NUMERIC

    private def numericFormulaCellWithAnInteger: Boolean = numericFormula &&
      poiCell.getNumericCellValue.isValidInt &&
      !(CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat) ||
        SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat))

    private def numericFormulaCellWithADouble: Boolean = numericFormula &&
      !poiCell.getNumericCellValue.isValidInt &&
      !(CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat) ||
        SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat))

    private def numericFormulaCellWithACurrency: Boolean =
      numericFormula && CURRENCY_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat)

    private def numericFormulaCellWithADate: Boolean =
      numericFormula && SHORT_DATE_FORMAT_IDS.contains(poiCell.getCellStyle.getDataFormat)

    private def invalidDate = !Try {
      LocalDate.parse(poiCell.getStringCellValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.toOption

    private def currencyRegex =
      raw"^([-\u00AD]?)(R?\p{Sc})(\s*)(([1-9]\d{0,2}([,.]\d{3})*)|(([1-9]\d*)?\d))([,.]\d\d)?".r

    private def dateRegex = raw"(\d{2}/\d{2}/\d{4})".r
    private def dateLikeRegex = raw"(\p{Alnum}{2}/\p{Alnum}{2}/\p{Alnum}{4})".r

  extension[T] (thisOption: Option[T])
    @targetName("or")
    private def ||(thatOption: Option[T]): Option[T] = thisOption.orElse(thatOption)
    @targetName("not")
    private def unary_! = thisOption.isEmpty