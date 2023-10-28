package com.andreidiego.extractionguide

sealed trait Instruction

case class RegularInstruction(
  fieldName: FieldName,
  matrixCoordinate: MatrixCoordinate,
  fieldType: FieldType,
) extends Instruction

enum FieldName:
  case NoteNumber, TradingDate, BankInstitutionNumber, TransitNumber, AccountNumber, OperationType,
    Ticker, Qty, Price, Volume, SellingsTotal, BuyingsTotal, OperationsTotal, SettlementFee,
    TradingFees, Brokerage, ServiceTax, IncomeTaxAtSource, Total, SettlementDate

case class MatrixCoordinate(lineNumber: LineNumber, columnNumber: ColumnNumber)

case class LineNumber(number: PositiveInteger):
  def to(end: Int): Range.Inclusive = Range.inclusive(number, end)

object LineNumber:
  given Conversion[LineNumber, Int] = _.number.toInt

case class ColumnNumber(number: PositiveInteger)

object ColumnNumber:
  given Conversion[ColumnNumber, Int] = _.number.toInt

class PositiveInteger(val from: Int):
  require(from > 0)
  override def toString: String = from.toString
  def toInt: Int = from

object PositiveInteger:
  given Conversion[PositiveInteger, Int] = _.from

case class RepetitionBlock(
  lineRange: LineRange,
  repeatedItems: Iterable[RepeatedItem],
  jumpLine: Boolean
) extends Instruction

case class LineRange(start: LineNumber, end: LineNumber)

object LineRange:
  given Conversion[LineRange, Int] = _.start

case class RepeatedItem(
  fieldName: FieldName,
  tokenCoordinate: TokenCoordinate,
  fieldType: FieldType,
)

sealed trait TokenCoordinate
case class FixedTokenCoordinate(tokenIndex: PositiveInteger) extends TokenCoordinate
case class RelativeTokenCoordinate(
  reference: String,
  direction: String,
  adjustingFactor: PositiveInteger
) extends TokenCoordinate

import scala.util.matching.Regex
enum FieldType(val regex: Regex):
  case Number     extends FieldType(raw"([-\u00AD]?)(\d+)".r)
  case Character  extends FieldType(raw"(^[a-zA-Z0-9])".r)
  case String     extends FieldType(raw"(^[a-zA-Z0-9]+)".r)
  case Date       extends FieldType(raw"(\d{2})/(\d{2})/(\d{4})".r)
  case Currency   extends FieldType(raw"^([-\u00AD]?)(\p{Sc}?)(([1-9]\d{0,2}(\.\d{3})*)|(([1-9]\d*)?\d))(,\d\d)?".r)