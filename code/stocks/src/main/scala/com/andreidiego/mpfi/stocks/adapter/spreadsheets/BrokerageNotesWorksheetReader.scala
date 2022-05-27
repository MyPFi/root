package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import cats.data.ValidatedNec
import cats.implicits.*
import cats.kernel.Semigroup
import cats.syntax.validated.*
import com.andreidiego.mpfi.stocks.adapter.services.*
import com.andreidiego.mpfi.stocks.adapter.services.OperationalMode.Normal
import excel.poi.{Cell, Line, Worksheet}

import java.time.{LocalDate, LocalDateTime}
import java.util.Locale
import scala.annotation.targetName
import scala.math.Ordering.Implicits.*

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation], val financialSummary: FinancialSummary)

abstract sealed class Operation(val volume: String, val settlementFee: String, val tradingFees: String, val brokerage: String, val serviceTax: String, val incomeTaxAtSource: String, val total: String)

// TODO Add test for turning BuyingOperation into a case class
case class BuyingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total)

// TODO Add test for turning SellingOperation into a case class
case class SellingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total)

// TODO Add test for turning FinancialSummary into a case class
case class FinancialSummary(volume: String, settlementFee: String, tradingFees: String, brokerage: String, serviceTax: String, incomeTaxAtSource: String, total: String)

object BrokerageNotesWorksheetReader:
  enum BrokerageNoteReaderError(message: String):
    case RequiredValueMissing(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentValue(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentType(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentColor(message: String) extends BrokerageNoteReaderError(message)

  import BrokerageNoteReaderError.*

  type Error = BrokerageNoteReaderError | Worksheet.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]
  private type Group = Seq[Line]
  private type CellValidation = Cell => (String, Int) ⇒ ErrorsOr[Cell]
  private type CellCheck = Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]
  private type InterCellValidation = (Cell, Cell) ⇒ String ⇒ ErrorsOr[Cell]
  private type LineValidation = Line ⇒ String => ErrorsOr[Line]
  private type InterLineValidation = (Line, Line) => String ⇒ ErrorsOr[Line]
  private type GroupValidation = Group ⇒ String => ErrorsOr[Group]

  private val RED = "255,0,0"
  private val BLUE = "68,114,196"

  given comparisonPrecision: Double = 0.02

  given Semigroup[Group] = (x, y) => if x == y then x else x ++: y

  def from(worksheet: Worksheet): ErrorsOr[BrokerageNotesWorksheetReader] = worksheet.groups
    .map(_.validatedWith(
      cellValidations(
        assertTradingDate(isPresent, isAValidDate, hasAValidFontColor),
        assertNoteNumber(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor),
        assertTicker(isPresent, hasAValidFontColor),
        assertQty(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor),
        assertPrice(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor),
        assertVolume(isPresent, isAValidCurrency, hasAValidFontColor),
        assertSettlementFee(isPresent, isAValidCurrency, hasAValidFontColor),
        assertTradingFees(isPresent, isAValidCurrency, hasAValidFontColor),
        assertBrokerage(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor),
        assertServiceTax(isPresent, isNotNegative, isAValidCurrency, hasAValidFontColor),
        assertIncomeTaxAtSource(isAValidCurrency, hasAValidFontColor),
        assertTotal(isPresent, isAValidCurrency, hasAValidFontColor),
      ), 
      interCellValidations(
      ), 
      lineValidations(
        assertFontColorReflectsOperationType(
          onTradingDate, onNoteNumber, onTicker, onQty, onPrice, onVolume, onSettlementFee, onTradingFees, onBrokerage, onServiceTax, onIncomeTaxAtSource, onTotal
        ),
        assertVolumeIsCalculatedCorrectly,
        assertSettlementFeeIsCalculatedCorrectly,
        assertTradingFeesIsCalculatedCorrectly,
        assertServiceTaxIsCalculatedCorrectly,
        assertIncomeTaxAtSourceIsCalculatedCorrectly,
        assertTotalIsCalculatedCorrectly
      ), 
      interLineValidations(
        assertLinesInGroupHaveSameTradingDate,
        assertLinesInGroupHaveSameNoteNumber
      ), 
      groupValidations(
        assertVolumeSummary(isPresent),
        assertSettlementFeeSummary(isPresent),
        assertTradingFeesSummary(isPresent),
        assertBrokerageSummary(isPresent),
        assertServiceTaxSummary(isPresent),
        assertTotalSummary(isPresent),
        assertMultilineGroupHasSummary,
        assertSettlementFeeSummaryIsCalculatedCorrectly,
        assertTradingFeesSummaryIsCalculatedCorrectly,
        assertBrokerageSummaryIsCalculatedCorrectly,
        assertServiceTaxSummaryIsCalculatedCorrectly,
        assertIncomeTaxAtSourceSummaryIsCalculatedCorrectly,
        assertVolumeSummaryIsCalculatedCorrectly,
        assertTotalSummaryIsCalculatedCorrectly
      )
    )(worksheet.name).andThen(_.toBrokerageNote(worksheet.name).accumulate))
    .reduce(_ combine _)
    .map(BrokerageNotesWorksheetReader(_))

  private def cellValidations(cellValidation: CellValidation*) = cellValidation

  private def interCellValidations(interCellValidation: InterCellValidation*) = interCellValidation
  
  private def lineValidations(lineValidation: LineValidation*) = lineValidation
  
  private def interLineValidations(interLineValidation: InterLineValidation*) = interLineValidation
  
  private def groupValidations(groupValidation: GroupValidation*) = groupValidation

  private def assertTradingDate(tradingDateChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "TradingDate", _.isTradingDate, tradingDateChecks: _*)(worksheetName, lineNumber)

  private def assertNoteNumber(noteNumberChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "NoteNumber", _.isNoteNumber, noteNumberChecks: _*)(worksheetName, lineNumber)

  private def assertTicker(tickerChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Ticker", _.isTicker, tickerChecks: _*)(worksheetName, lineNumber)

  private def assertQty(qtyChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Qty", _.isQty, qtyChecks: _*)(worksheetName, lineNumber)

  private def assertPrice(priceChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Price", _.isPrice, priceChecks: _*)(worksheetName, lineNumber)

  private def assertVolume(volumeChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Volume", _.isVolume, volumeChecks: _*)(worksheetName, lineNumber)

  private def assertSettlementFee(settlementFeeChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "SettlementFee", _.isSettlementFee, settlementFeeChecks: _*)(worksheetName, lineNumber)

  private def assertTradingFees(tradingFeesChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "TradingFees", _.isTradingFees, tradingFeesChecks: _*)(worksheetName, lineNumber)

  private def assertBrokerage(brokerageChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Brokerage", _.isBrokerage, brokerageChecks: _*)(worksheetName, lineNumber)

  private def assertServiceTax(serviceTaxChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "ServiceTax", _.isServiceTax, serviceTaxChecks: _*)(worksheetName, lineNumber)

  private def assertIncomeTaxAtSource(incomeTaxAtSourceChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "IncomeTaxAtSource", _.isIncomeTaxAtSource, incomeTaxAtSourceChecks: _*)(worksheetName, lineNumber)

  private def assertTotal(totalChecks: CellCheck*): CellValidation = cell => (worksheetName, lineNumber) ⇒ 
    assertAttribute(cell, "Total", _.isTotal, totalChecks: _*)(worksheetName, lineNumber)

  private def assertAttribute(attribute: Cell, attributeName: String, attributeGuard: Cell ⇒ Boolean, attributeChecks: CellCheck*)(worksheetName: String, lineNumber: Int): ErrorsOr[Cell] =
    given Semigroup[Cell] = (x, _) => x

    if attributeGuard(attribute) then
      attributeChecks
        .map(_ (attribute)(attributeName, lineNumber, worksheetName))
        .reduce(_ combine _)
        // .map(_ ⇒ group)
    else attribute.validNec

  private def isPresent(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isNotEmpty then cell.validNec
    else RequiredValueMissing(
      s"A required attribute ('$cellHeader') is missing on line '$lineNumber' of 'Worksheet' '$worksheetName'."
    ).invalidNec

  private def isAValidDate(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.asLocalDate.isDefined then cell.validNec
    else UnexpectedContentType(
      s"'$cellHeader' ('${cell.value}') on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be interpreted as a date."
    ).invalidNec

  private def hasAValidFontColor(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isEmpty || Seq(RED, BLUE).contains(cell.fontColor) then cell.validNec
    else UnexpectedContentColor(
      s"'$cellHeader's font-color ('${cell.fontColor}') on line '$lineNumber' of 'Worksheet' '$worksheetName' can only be red ('$RED') or blue ('$BLUE')."
    ).invalidNec

  private def isNotNegative(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.asDouble.forall(_ >= 0.0) then cell.validNec
    else UnexpectedContentValue(
      s"'$cellHeader' (${cell.value}) on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be negative."
    ).invalidNec

  private def isAValidInteger(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.asInt.isDefined then cell.validNec
    else UnexpectedContentType(
      s"'$cellHeader' ('${cell.value}') on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be interpreted as an integer number."
    ).invalidNec

  private def isAValidCurrency(cell: Cell)(cellHeader: String, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] =
    if cell.isEmpty || cell.isCurrency then cell.validNec
    else UnexpectedContentType(
      s"'$cellHeader' ('${cell.value}') on line '$lineNumber' of 'Worksheet' '$worksheetName' cannot be interpreted as a currency."
    ).invalidNec

  private def assertFontColorReflectsOperationType(attributeColorChecks: (Operation) => (Line, String) => ErrorsOr[Cell]*): LineValidation = line ⇒ worksheetName =>
    line.toMostLikelyOperation(worksheetName)
      .andThen{operation => 
        attributeColorChecks
          .map(_(operation)(line, worksheetName).map(Seq(_)))
          .reduce(_ combine _)
          .map(_=> line)
      }
    
  private def onTradingDate(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(0))("TradingDate", operation, line.number, worksheetName)

  private def onNoteNumber(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(1))("NoteNumber", operation, line.number, worksheetName)

  private def onTicker(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(2))("Ticker", operation, line.number, worksheetName)

  private def onQty(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(3))("Qty", operation, line.number, worksheetName)

  private def onPrice(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(4))("Price", operation, line.number, worksheetName)

  private def onVolume(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(5))("Volume", operation, line.number, worksheetName)

  private def onSettlementFee(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(6))("SettlementFee", operation, line.number, worksheetName)

  private def onTradingFees(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(7))("TradingFees", operation, line.number, worksheetName)

  private def onBrokerage(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(8))("Brokerage", operation, line.number, worksheetName)

  private def onServiceTax(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(9))("ServiceTax", operation, line.number, worksheetName)

  private def onIncomeTaxAtSource(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(10))("IncomeTaxAtSource", operation, line.number, worksheetName)

  private def onTotal(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    inCell(line.cells(11))("Total", operation, line.number, worksheetName)

  private def inCell(cell: Cell)(cellHeader: String, operation: Operation, lineNumber: Int, worksheetName: String): ErrorsOr[Cell] = 
    if cell.isEmpty then cell.validNec
    else 
      val errorMessage: (String, String, String) => String = (lineOperation, cellFontColor, cellOperation) => s"The 'Operation' on line '$lineNumber' of 'Worksheet' '$worksheetName' looks like '$lineOperation' but, '$cellHeader' has font-color $cellFontColor which denotes '$cellOperation'."
      operation match
        case a: SellingOperation =>       
          if cell.fontColor != RED then cell.validNec
          else UnexpectedContentColor(errorMessage("Selling", s"red('$RED')", "Buying")).invalidNec

        case a: BuyingOperation =>
          if cell.fontColor != BLUE then cell.validNec
          else UnexpectedContentColor(errorMessage("Buying", s"blue('$BLUE')", "Selling")).invalidNec

  private def assertVolumeIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val qtyCell = line.cells(3)
    val priceCell = line.cells(4)
    val volumeCell = line.cells(5)
    val qty = qtyCell.asInt.getOrElse(0)
    val price = priceCell.asDouble.getOrElse(0.0)

    val actualVolume = volumeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    val expectedVolume = (qty * price).formatted("%.2f")

    if actualVolume != expectedVolume then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${volumeCell.address}:Volume') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedVolume', which is equal to '${qtyCell.address}:Qty * ${priceCell.address}:Price ($qty * ${price.formatted("%.2f")})' but, it actually contained '$actualVolume'."
    ).invalidNec
    else line.validNec

  // TODO Think about adding a reason to the errors for the cases where the requirements for the validation are not present. For instance, in the case below, any one of the following, if missing or having any other problem, would result in an impossible validation: tradingDate, volume and, actualSettlementFee. For now, we'll choose default values for them in case of problem and the validation will fail because of them. Hopefully, the original cause will be caught by other validation.
  private def assertSettlementFeeIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val volumeCell = line.cells(5)
    val settlementFeeCell = line.cells(6)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualSettlementFee = settlementFeeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Actually detecting the correct 'OperationalMode' may prove challenging when creating a 'BrokerageNote', unless it happens in real-time, since the difference between 'Normal' and 'DayTrade' is actually time-related. A 'BrokerageNote' instance is supposed to be created when a brokerage note document is detected in the filesystem or is provided to the system by any other means. That document contains only the 'TradingDate' and not the time so, unless the system is provided with information about the brokerage note document as soon as an 'Order' gets executed (the moment that gives birth to a brokerage note), that won't be possible. It is important to note that, generally, brokerage notes are not made available by 'Broker's until the day after the fact ('Operation's for the whole day are grouped in a brokerage note, that's why). Maybe we should try a different try and error approach when ingesting a brokerage note document: First we try to check the calculation of the 'SettlementFee' assuming the 'Normal' 'OperationMode' and if that does not work, than we switch it to 'DayTrade' and try again. If that does not work, then we have found a problem with the calculation applied by the 'Broker'.
    val settlementFeeRate = SettlementFeeRate.forOperationalMode(Normal).at(tradingDate).value
    val expectedSettlementFee = (volume * settlementFeeRate).formatted("%.2f")

    if actualSettlementFee != expectedSettlementFee then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${settlementFeeCell.address}:SettlementFee') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedSettlementFee', which is equal to '${volumeCell.address}:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' (${volume.formatted("%.2f")} * ${(settlementFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '$actualSettlementFee'."
    ).invalidNec
    else line.validNec

  private def assertTradingFeesIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val volumeCell = line.cells(5)
    val negotiationsFeeCell = line.cells(7)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val tradingTime = TradingFeesRate.TRADING
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualNegotiationsFee = negotiationsFeeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Same challenge here since 'TradingFees' is also dependent on the time of order execution which is not part of the brokerage note document.
    val negotiationsFeeRate = TradingFeesRate.at(LocalDateTime.of(tradingDate, tradingTime))
    val expectedNegotiationsFee = (volume * negotiationsFeeRate).formatted("%.2f")

    if actualNegotiationsFee != expectedNegotiationsFee then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${negotiationsFeeCell.address}:NegotiationsFee') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedNegotiationsFee', which is equal to '${volumeCell.address}:Volume * 'NegotiationsFeeRate' at 'TradingDateTime' (${volume.formatted("%.2f")} * ${(negotiationsFeeRate * 100).formatted("%.4f")}%)' but, it actually contained '$actualNegotiationsFee'."
    ).invalidNec
    else line.validNec

  private def assertServiceTaxIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val actualServiceTax = serviceTaxCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO The city used to calculate the ServiceTax can be determined, in the future, by looking into the Broker information present in the brokerage note document.
    val serviceTaxRate = ServiceTaxRate.at(tradingDate).value
    val expectedServiceTax = (brokerage * serviceTaxRate).formatted("%.2f")

    if actualServiceTax != expectedServiceTax then UnexpectedContentValue(
      s"An invalid calculated 'Cell' ('${serviceTaxCell.address}:ServiceTax') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '$expectedServiceTax', which is equal to '${brokerageCell.address}:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' (${brokerage.formatted("%.2f")} * ${(serviceTaxRate * 100).formatted("%.1f")}%)' but, it actually contained '$actualServiceTax'."
    ).invalidNec
    else line.validNec

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val incomeTaxAtSourceCell = line.cells(10)
    // TODO IncomeTaxAtSource can never be negative. It is not like I can restitute it if I have a loss. Restitutions do not occur at the source
    val actualIncomeTaxAtSource = incomeTaxAtSourceCell.asDouble.getOrElse(0.0)

    if line.hasMostCellsBlue then
      val tickerCell = line.cells(2)
      val qtyCell = line.cells(3)
      val volumeCell = line.cells(5)
      val settlementFeeCell = line.cells(6)
      val tradingFeesCell = line.cells(7)
      val brokerageCell = line.cells(8)
      val serviceTaxCell = line.cells(9)

      val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
      val qty = qtyCell.asInt.getOrElse(0)
      val volume = volumeCell.asDouble.getOrElse(0.0)
      val settlementFee = settlementFeeCell.asDouble.getOrElse(0.0)
      val tradingFees = tradingFeesCell.asDouble.getOrElse(0.0)
      val brokerage = brokerageCell.asDouble.getOrElse(0.0)
      val serviceTax = serviceTaxCell.asDouble.getOrElse(0.0)
      val operationNetResult = volume - settlementFee - tradingFees - brokerage - serviceTax
      val operationAverageCost = AverageStockPrice.forTicker(tickerCell.value) * qty
      // TODO When the ticker cannot be found in the portfolio, 0.0 is returned which should trigger an exception since I'm trying to sell something I do not posses. For now, I'll tweak TEST_SPREADSHEET so that all BuyingOperations refer to VALE5 and have the appropriate calculation for the IncomeTaxAtSource.
      val operationProfit = operationNetResult - operationAverageCost
      val incomeTaxAtSourceRate = IncomeTaxAtSourceRate.forOperationalMode(Normal).at(tradingDate).value
      val expectedIncomeTaxAtSource = operationProfit * incomeTaxAtSourceRate

      if actualIncomeTaxAtSource !~= expectedIncomeTaxAtSource then UnexpectedContentValue(
        s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedIncomeTaxAtSource.formatted("%.2f")}', which is equal to (('${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${tradingFeesCell.address}:TradingFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax') - ('AverageStockPrice' for the '${tickerCell.address}:Ticker' * '${qtyCell.address}:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' (${operationProfit.formatted("%.2f")} * ${(incomeTaxAtSourceRate * 100).formatted("%.4f")}%)' but, it actually contained '${actualIncomeTaxAtSource.formatted("%.2f")}'."
      ).invalidNec
      else line.validNec
    else
      given Semigroup[Line] = (x, _) => x

      if incomeTaxAtSourceCell.isNotEmpty then {
        if !incomeTaxAtSourceCell.isCurrency then UnexpectedContentType(
          s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to be either empty or equal to '0.00' but, it actually contained '${incomeTaxAtSourceCell.value}'."
        ).invalidNec
        else line.validNec
      } combine {
        if actualIncomeTaxAtSource > 0.0 then UnexpectedContentValue(
          s"An invalid calculated 'Cell' ('${incomeTaxAtSourceCell.address}:IncomeTaxAtSource') was found on 'Worksheet' '$worksheetName'. It was supposed to be either empty or equal to '0.00' but, it actually contained '${if incomeTaxAtSourceCell.isCurrency then actualIncomeTaxAtSource.formatted("%.2f") else incomeTaxAtSourceCell.value}'."
        ).invalidNec
        else line.validNec
      }
      else line.validNec

  private def assertTotalIsCalculatedCorrectly: LineValidation = line ⇒ worksheetName =>
    val volumeCell = line.cells(5)
    val settlementFeeCell = line.cells(6)
    val tradingFeesCell = line.cells(7)
    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val totalCell = line.cells(11)
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val settlementFee = settlementFeeCell.asDouble.getOrElse(0.0)
    val tradingFees = tradingFeesCell.asDouble.getOrElse(0.0)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val serviceTax = serviceTaxCell.asDouble.getOrElse(0.0)
    val actualTotal = totalCell.asDouble.getOrElse(0.0)

    if line.hasMostCellsBlue then
      val expectedTotal = volume - settlementFee - tradingFees - brokerage - serviceTax

      if actualTotal !~= expectedTotal then UnexpectedContentValue(
        s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' - '${settlementFeeCell.address}:SettlementFee' - '${tradingFeesCell.address}:TradingFees' - '${brokerageCell.address}:Brokerage' - '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
      ).invalidNec
      else line.validNec
    else
      val expectedTotal = volume + settlementFee + tradingFees + brokerage + serviceTax

      if actualTotal !~= expectedTotal then UnexpectedContentValue(
        s"An invalid calculated 'Cell' ('${totalCell.address}:Total') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedTotal.formatted("%.2f")}', which is equal to '${volumeCell.address}:Volume' + '${settlementFeeCell.address}:SettlementFee' + '${tradingFeesCell.address}:TradingFees' + '${brokerageCell.address}:Brokerage' + '${serviceTaxCell.address}:ServiceTax' but, it actually contained '${actualTotal.formatted("%.2f")}'."
      ).invalidNec
      else line.validNec

  private def assertLinesInGroupHaveSameTradingDate: InterLineValidation = (first: Line, second: Line) => worksheetName ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${second.cells.tail.head.value}') was found on 'Worksheet' '$worksheetName'. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondTradingDateCell.value}' in '${secondTradingDateCell.address}' is different from '${firstTradingDateCell.value}' in '${firstTradingDateCell.address}'."
    ).invalidNec
    else second.validNec

  private def assertLinesInGroupHaveSameNoteNumber: InterLineValidation = (first: Line, second: Line) => worksheetName ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${secondNoteNumberCell.value}') was found on 'Worksheet' '$worksheetName'. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondNoteNumberCell.value}' in '${secondNoteNumberCell.address}' is different from '${firstNoteNumberCell.value}' in '${firstNoteNumberCell.address}'."
    ).invalidNec
    else second.validNec

  private def assertVolumeSummary(volumeSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(5, "VolumeSummary", _.isVolume, volumeSummaryChecks: _*)(worksheetName, group)

  private def assertSettlementFeeSummary(settlementfeeSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(6, "SettlementFeeSummary", _.isSettlementFee, settlementfeeSummaryChecks: _*)(worksheetName, group)

  private def assertTradingFeesSummary(tradingfeesSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(7, "TradingFeesSummary", _.isTradingFees, tradingfeesSummaryChecks: _*)(worksheetName, group)

  private def assertBrokerageSummary(brokerageSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(8, "BrokerageSummary", _.isBrokerage, brokerageSummaryChecks: _*)(worksheetName, group)

  private def assertServiceTaxSummary(servicetaxSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(9, "ServiceTaxSummary", _.isServiceTax, servicetaxSummaryChecks: _*)(worksheetName, group)

  private def assertTotalSummary(totalSummaryChecks: CellCheck*): GroupValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(11, "TotalSummary", _.isTotal, totalSummaryChecks: _*)(worksheetName, group)

  private def assertSummaryAttribute(attributeIndex: Int, attributeName: String, attributeGuard: Cell ⇒ Boolean, attributeChecks: CellCheck*)(worksheetName: String, group: Group): ErrorsOr[Group] =
    group.summary.map { summary ⇒
      val summaryAttribute = summary.cells(attributeIndex)
      assertAttribute(summaryAttribute, attributeName, attributeGuard, attributeChecks: _*)(worksheetName, summary.number).accumulate.liftTo(summary).accumulate.liftTo(group)
    }.getOrElse(group.validNec)

  private def assertMultilineGroupHasSummary: GroupValidation = group ⇒ worksheetName =>
    group.nonSummaryLines match
      case Seq(_, _, _*) ⇒ group.summary match
        case None ⇒ group.summaryLikeLine match
          case Some(summaryLikeLine) ⇒
            val invalidSummaryCells = summaryLikeLine.nonEmptyCells
              .filter(!_.isFormula)
              .map(cell ⇒ s"${cell.address}:${cell.`type`}")
              .mkString("[", ",", "]")

            UnexpectedContentType(
              s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. All non-empty 'Cell's of a 'Group's 'Summary' are supposed to be formulas but, that's not the case with '$invalidSummaryCells'."
            ).invalidNec

          case _ ⇒ RequiredValueMissing(
            s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. 'MultilineGroup's must have a 'SummaryLine'."
          ).invalidNec
        case _ ⇒ group.validNec
      case _ ⇒ group.validNec

  private def assertSettlementFeeSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(6, "SettlementFee")(group, worksheetName)

  private def assertTradingFeesSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(7, "TradingFees")(group, worksheetName)

  private def assertBrokerageSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(8, "Brokerage")(group, worksheetName)

  private def assertServiceTaxSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(9, "ServiceTax")(group, worksheetName)

  private def assertIncomeTaxAtSourceSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationIndependentSummaryCellIsCalculatedCorrectly(10, "IncomeTaxAtSource")(group, worksheetName)

  private def assertOperationIndependentSummaryCellIsCalculatedCorrectly(cellIndex: Int, cellName: String)(group: Group, worksheetName: String): ErrorsOr[Group] =
    group.summary.map { summary ⇒
      val summaryCell = summary.cells(cellIndex)

      val expectedSummaryValue = group.nonSummaryLines.foldLeft(0.0)((acc, line) ⇒ acc + line.cells(cellIndex).asDouble.getOrElse(0.0))
      val actualSummaryValue = summaryCell.asDouble.getOrElse(0.0)

      if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
        s"An invalid calculated 'SummaryCell' ('${summaryCell.address}:${cellName}Summary') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all '$cellName's of the 'Group' (${group.head.cells(cellIndex).address}...${group.takeRight(2).head.cells(cellIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
      ).invalidNec
      else group.validNec
    }.getOrElse(group.validNec)

  private def assertVolumeSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationDependentSummaryCellIsCalculatedCorrectly(5, "Volume", _.volume.asDouble)(group, worksheetName)

  private def assertTotalSummaryIsCalculatedCorrectly: GroupValidation = group ⇒ worksheetName =>
    assertOperationDependentSummaryCellIsCalculatedCorrectly(11, "Total", _.total.asDouble)(group, worksheetName)

  private def assertOperationDependentSummaryCellIsCalculatedCorrectly(cellIndex: Int, cellName: String, valueToSummarizeFrom: Operation ⇒ Double)(group: Group, worksheetName: String) =
    group.summary.map { summary ⇒
      val summaryCell = summary.cells(cellIndex)

      group.nonSummaryLines
        .map(_.toOperation(worksheetName).map(Seq(_)))
        .reduce(_ combine _)
        .andThen{ operations => 
          val expectedSummaryValue = operations.foldLeft(0.0) { (acc, operation) ⇒
            operation match {
              case operation: BuyingOperation ⇒ acc - valueToSummarizeFrom(operation)
              case _ ⇒ acc + valueToSummarizeFrom(operation)
            }
          }
          val actualSummaryValue = summaryCell.asDouble.getOrElse(0.0)

          if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
            s"An invalid calculated 'SummaryCell' ('${summaryCell.address}:${cellName}Summary') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all 'SellingOperation's '$cellName's minus the sum of all 'BuyingOperation's '$cellName's of the 'Group' (${group.head.cells(cellIndex).address}...${group.takeRight(2).head.cells(cellIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
          ).invalidNec
          else group.validNec
        }
    }.getOrElse(group.validNec)

  extension (errorsOrBrokerageNote: ErrorsOr[BrokerageNote])

    @targetName("accumulateBrokerageNote")
    private def accumulate: ErrorsOr[Seq[BrokerageNote]] = errorsOrBrokerageNote.map(Seq(_))

  extension (errorsOrLine: ErrorsOr[Line])

    @targetName("accumulateLine")
    private def accumulate: ErrorsOr[Seq[Line]] = errorsOrLine.map(Seq(_))

  extension (errorsOrSeqOfLine: ErrorsOr[Seq[Line]])

    @targetName("liftSeqOfLineToGroup")
    private def liftTo(group: Group): ErrorsOr[Group] = errorsOrSeqOfLine.map(_ => group)

  extension (errorsOrCell: ErrorsOr[Cell])

    @targetName("accumulateCell")
    private def accumulate: ErrorsOr[Seq[Cell]] = errorsOrCell.map(Seq(_))

  extension (errorsOrSeqOfCell: ErrorsOr[Seq[Cell]])

    @targetName("liftSeqOfCellToLine")
    private def liftTo(line: Line): ErrorsOr[Line] = errorsOrSeqOfCell.map(_ => line)

  extension (group: Group)

    private def validatedWith(
      cellValidations: Seq[CellValidation] = Seq(),
      interCellValidations: Seq[InterCellValidation] = Seq(),
      lineValidations: Seq[LineValidation] = Seq(),
      interLineValidations: Seq[InterLineValidation] = Seq(),
      groupValidations: Seq[GroupValidation] = Seq()
    )(worksheetName: String): ErrorsOr[Group] =
      given Semigroup[Group] = (x, _) => x
      given Semigroup[Line] = (x, _) => x
      val appliedGroupValidations: ErrorsOr[Group] = groupValidations.map(_ (group)(worksheetName)).reduce(_ combine _)
      val validateAndHarmonize: (Line => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.map(f).reduce(_ combine _)
      val applyHarmonyChecksTo: (Seq[Line] => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.sliding(2).map(f).reduce(_ combine _)
      val applyLinesHarmonyChecksTo: (Line, Line) => ErrorsOr[Line] = (line1, line2) => interLineValidations.map(_ (line1, line2)(worksheetName)).reduce(_ combine _)

      appliedGroupValidations.combine{
        validateAndHarmonize{ line => 
          line.validatedWith(lineValidations)(worksheetName).accumulate.combine{
            line.harmonizedWith(interCellValidations)(worksheetName).accumulate.combine{
              line.withCellsValidated(cellValidations)(worksheetName).accumulate
            }
          }
        }.liftTo(group).combine{
          applyHarmonyChecksTo{ lineWindow ⇒
            lineWindow match {
              case Seq(line1, line2) ⇒ applyLinesHarmonyChecksTo(line1, line2).accumulate
              case Seq(line) ⇒ line.validNec.accumulate
            }
          }
        }
      } 

    private def toBrokerageNote(worksheetName: String): ErrorsOr[BrokerageNote] = 
      nonSummaryLines
        .map(_.toOperation(worksheetName).map(Seq(_)))
        .reduce(_ combine _)
        .map(BrokerageNote(_, group.head.toFinancialSummary))

    private def nonSummaryLines: Seq[Line] = group.filter(line => !line.isSummary && !line.isSummaryLikeLine)

    private def summary: Option[Line] = Option(group.last).filter(_.isSummary)

    private def summaryLikeLine: Option[Line] = Option(group.last).filter(_.isSummaryLikeLine)

  extension (line: Line)

    private def hasMostCells(fontColor: String): Boolean = nonEmptyCells.count(_.fontColor == fontColor) > nonEmptyCells.size / 2
    
    private def hasMostCellsRed: Boolean = hasMostCells(RED)

    private def hasMostCellsBlue: Boolean = hasMostCells(BLUE)
    
    private def allNonEmpty(fontColor: String): Boolean = nonEmptyCells.forall(_.fontColor == fontColor)

    private def allNonEmptyBlue: Boolean = allNonEmpty(BLUE)
    
    private def allNonEmptyRed: Boolean = allNonEmpty(RED)

    private def nonEmptyCells: Seq[Cell] = cells.filter(_.isNotEmpty)

    private def cells: Seq[Cell] = line.cells

    private def isSummary: Boolean = nonEmptyCells.forall(isFormula)

    private def toMostLikelyOperation(worksheetName: String): ErrorsOr[Operation] = 
      if hasMostCellsBlue then
        SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else if hasMostCellsRed then
        BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else UnexpectedContentColor(
        s"Impossible to determine the type of 'Operation' on line '${line.number}' of 'Worksheet' '$worksheetName' due to exactly half of the non-empty 'Attribute's of each valid color."
      ).invalidNec

    private def toOperation(worksheetName: String): ErrorsOr[Operation] = 
      if allNonEmptyBlue then
        SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else if allNonEmptyRed then
        BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else UnexpectedContentColor(
        s"Impossible to determine the type of 'Operation' on line '${line.number}' of 'Worksheet' '$worksheetName' due to its 'Attribute's having divergent font colors."
      ).invalidNec

    private def toFinancialSummary: FinancialSummary =
      FinancialSummary(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)

    private def isSummaryLikeLine: Boolean = nonEmptyCells.count(_.isFormula) > nonEmptyCells.size / 2
    
    private def validatedWith(lineValidations: Seq[LineValidation])(worksheetName: String): ErrorsOr[Line] = 
      given Semigroup[Line] = (x, _) => x
      lineValidations.map(_ (line)(worksheetName)).reduce(_ combine _)

    private def withCellsValidated(cellValidations: Seq[CellValidation])(worksheetName: String): ErrorsOr[Line] = 
      cells.map(_.validatedWith(cellValidations)(worksheetName, line.number).accumulate).reduce(_ combine _).liftTo(line)

    private def harmonizedWith(cellsHarmonyChecks: Seq[InterCellValidation])(worksheetName: String): ErrorsOr[Line] = 
      given Semigroup[Cell] = (x, _) => x
      val applyCellsHarmonyChecksTo: (Cell, Cell) => ErrorsOr[Cell] = (cell1, cell2) => cellsHarmonyChecks.map(_ (cell1, cell2)(worksheetName)).reduce(_ combine _)
      val harmonized: (Seq[Cell] => ErrorsOr[Seq[Cell]]) => ErrorsOr[Seq[Cell]] = f ⇒ cells.sliding(2).map(f).reduce(_ combine _)
      
      if !cellsHarmonyChecks.isEmpty then
        harmonized{ cells ⇒
          cells match {
            case Seq(cell1, cell2) ⇒ applyCellsHarmonyChecksTo(cell1, cell2).accumulate
            case Seq(cell) ⇒ cell.validNec.accumulate
          }
        }.liftTo(line)
      else line.validNec

  extension (cell: Cell)

    private def isFormula: Boolean = cell.formula.nonEmpty

    private def isTradingDate: Boolean = cell.address.startsWith("A")

    private def isNoteNumber: Boolean = cell.address.startsWith("B")

    private def isTicker: Boolean = cell.address.startsWith("C")

    private def isQty: Boolean = cell.address.startsWith("D")

    private def isPrice: Boolean = cell.address.startsWith("E")

    private def isVolume: Boolean = cell.address.startsWith("F")

    private def isSettlementFee: Boolean = cell.address.startsWith("G")

    private def isTradingFees: Boolean = cell.address.startsWith("H")

    private def isBrokerage: Boolean = cell.address.startsWith("I")

    private def isServiceTax: Boolean = cell.address.startsWith("J")

    private def isIncomeTaxAtSource: Boolean = cell.address.startsWith("K")

    private def isTotal: Boolean = cell.address.startsWith("L")
    
    private def validatedWith(cellValidations: Seq[CellValidation])(worksheetName: String, lineNumber: Int): ErrorsOr[Cell] =
      given Semigroup[Cell] = (x, _) => x
      cellValidations.map(_ (cell)(worksheetName, lineNumber)).reduce(_ combine _)

  extension (double: Double)

    @targetName("differentBeyondPrecision")
    private def !~=(other: Double)(using precision: Double): Boolean = (double - other).abs > precision

    private def formatted(format: String): String = String.format(Locale.US, format, double)

  extension (string: String)

    private def asDouble: Double = string.replace(",", ".").toDouble