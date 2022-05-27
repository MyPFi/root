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
  private type AttributeValidation = Cell => (String, Int) ⇒ ErrorsOr[Cell]
  private type AttributeCheck = Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]
  private type AttributesHarmonyCheck = (Cell, Cell) ⇒ String ⇒ ErrorsOr[Cell]
  private type OperationValidation = Line ⇒ String => ErrorsOr[Line]
  private type OperationsHarmonyCheck = (Line, Line) => String ⇒ ErrorsOr[Line]
  private type BrokerageNoteValidation = Group ⇒ String => ErrorsOr[Group]
  private type SummaryAttributeCheck = Cell ⇒ (Int, String, Int, Group, String) ⇒ ErrorsOr[Cell]

  private val RED = "255,0,0"
  private val BLUE = "68,114,196"
  private val UPPERCASE_A_ASCII = 65

  given comparisonPrecision: Double = 0.02

  given Semigroup[Group] = (x, y) => if x == y then x else x ++: y

  def from(worksheet: Worksheet): ErrorsOr[BrokerageNotesWorksheetReader] = worksheet.groups
    .map(_.validatedWith(
      attributeValidations(
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
      attributesHarmonyChecks(
      ), 
      operationValidations(
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
      operationsHarmonyChecks(
        assertOperationsInBrokerageNoteHaveSameTradingDate,
        assertOperationsInBrokerageNoteHaveSameNoteNumber
      ), 
      brokerageNoteValidations(
        assertVolumeSummary(isPresent, isOperationSensitive),
        assertSettlementFeeSummary(isPresent, isCalculatedCorrectly),
        assertTradingFeesSummary(isPresent, isCalculatedCorrectly),
        assertBrokerageSummary(isPresent, isCalculatedCorrectly),
        assertServiceTaxSummary(isPresent, isCalculatedCorrectly),
        assertIncomeTaxAtSourceSummary(isCalculatedCorrectly),
        assertTotalSummary(isPresent, isOperationSensitive),
        assertMultilineGroupHasSummary
      )
    )(worksheet.name).andThen(_.toBrokerageNote(worksheet.name).accumulate))
    .reduce(_ combine _)
    .map(BrokerageNotesWorksheetReader(_))

  private def attributeValidations(attributeValidations: AttributeValidation*) = attributeValidations

  private def attributesHarmonyChecks(attributesHarmonyChecks: AttributesHarmonyCheck*) = attributesHarmonyChecks
  
  private def operationValidations(operationValidations: OperationValidation*) = operationValidations
  
  private def operationsHarmonyChecks(operationsHarmonyChecks: OperationsHarmonyCheck*) = operationsHarmonyChecks
  
  private def brokerageNoteValidations(brokerageNoteValidations: BrokerageNoteValidation*) = brokerageNoteValidations

  private def assertTradingDate(tradingDateChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "TradingDate", _.isTradingDate, tradingDateChecks: _*)(worksheetName, operationIndex)

  private def assertNoteNumber(noteNumberChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "NoteNumber", _.isNoteNumber, noteNumberChecks: _*)(worksheetName, operationIndex)

  private def assertTicker(tickerChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Ticker", _.isTicker, tickerChecks: _*)(worksheetName, operationIndex)

  private def assertQty(qtyChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Qty", _.isQty, qtyChecks: _*)(worksheetName, operationIndex)

  private def assertPrice(priceChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Price", _.isPrice, priceChecks: _*)(worksheetName, operationIndex)

  private def assertVolume(volumeChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Volume", _.isVolume, volumeChecks: _*)(worksheetName, operationIndex)

  private def assertSettlementFee(settlementFeeChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "SettlementFee", _.isSettlementFee, settlementFeeChecks: _*)(worksheetName, operationIndex)

  private def assertTradingFees(tradingFeesChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "TradingFees", _.isTradingFees, tradingFeesChecks: _*)(worksheetName, operationIndex)

  private def assertBrokerage(brokerageChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Brokerage", _.isBrokerage, brokerageChecks: _*)(worksheetName, operationIndex)

  private def assertServiceTax(serviceTaxChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "ServiceTax", _.isServiceTax, serviceTaxChecks: _*)(worksheetName, operationIndex)

  private def assertIncomeTaxAtSource(incomeTaxAtSourceChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "IncomeTaxAtSource", _.isIncomeTaxAtSource, incomeTaxAtSourceChecks: _*)(worksheetName, operationIndex)

  private def assertTotal(totalChecks: AttributeCheck*): AttributeValidation = attribute => (worksheetName, operationIndex) ⇒ 
    assertAttribute(attribute, "Total", _.isTotal, totalChecks: _*)(worksheetName, operationIndex)

  private def assertAttribute(attribute: Cell, attributeName: String, attributeGuard: Cell ⇒ Boolean, attributeChecks: AttributeCheck*)(worksheetName: String, operationIndex: Int): ErrorsOr[Cell] =
    given Semigroup[Cell] = (x, _) => x

    if attributeGuard(attribute) then
      attributeChecks
        .map(_ (attribute)(attributeName, operationIndex, worksheetName))
        .reduce(_ combine _)
        // .map(_ ⇒ group)
    else attribute.validNec

  private def isPresent(attribute: Cell)(attributeHeader: String, operationIndex: Int, worksheetName: String): ErrorsOr[Cell] =
    if attribute.isNotEmpty then attribute.validNec
    else RequiredValueMissing(
      s"A required attribute ('$attributeHeader') is missing on line '$operationIndex' of 'Worksheet' '$worksheetName'."
    ).invalidNec

  private def isAValidDate: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.asLocalDate.isDefined then attribute.validNec
    else UnexpectedContentType(
      s"'$attributeHeader' ('${attribute.value}') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be interpreted as a date."
    ).invalidNec

  private def hasAValidFontColor: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.isEmpty || Seq(RED, BLUE).contains(attribute.fontColor) then attribute.validNec
    else UnexpectedContentColor(
      s"'$attributeHeader's font-color ('${attribute.fontColor}') on line '$operationIndex' of 'Worksheet' '$worksheetName' can only be red ('$RED') or blue ('$BLUE')."
    ).invalidNec

  private def isNotNegative: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.asDouble.forall(_ >= 0.0) then attribute.validNec
    else UnexpectedContentValue(
      s"'$attributeHeader' (${attribute.value}) on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be negative."
    ).invalidNec

  private def isAValidInteger: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.asInt.isDefined then attribute.validNec
    else UnexpectedContentType(
      s"'$attributeHeader' ('${attribute.value}') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be interpreted as an integer number."
    ).invalidNec

  private def isAValidCurrency: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.isEmpty || attribute.isCurrency then attribute.validNec
    else UnexpectedContentType(
      s"'$attributeHeader' ('${attribute.value}') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be interpreted as a currency."
    ).invalidNec

  private def assertFontColorReflectsOperationType(attributeColorChecks: Operation => (Line, String) => ErrorsOr[Cell]*): OperationValidation = line ⇒ worksheetName =>
    line.toMostLikelyOperation(worksheetName)
      .andThen{operation => 
        attributeColorChecks
          .map(_(operation)(line, worksheetName).map(Seq(_)))
          .reduce(_ combine _)
          .map(_=> line)
      }
    
  private def onTradingDate(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(0))("TradingDate", operation, line.number, worksheetName)

  private def onNoteNumber(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(1))("NoteNumber", operation, line.number, worksheetName)

  private def onTicker(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(2))("Ticker", operation, line.number, worksheetName)

  private def onQty(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(3))("Qty", operation, line.number, worksheetName)

  private def onPrice(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(4))("Price", operation, line.number, worksheetName)

  private def onVolume(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(5))("Volume", operation, line.number, worksheetName)

  private def onSettlementFee(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(6))("SettlementFee", operation, line.number, worksheetName)

  private def onTradingFees(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(7))("TradingFees", operation, line.number, worksheetName)

  private def onBrokerage(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(8))("Brokerage", operation, line.number, worksheetName)

  private def onServiceTax(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(9))("ServiceTax", operation, line.number, worksheetName)

  private def onIncomeTaxAtSource(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(10))("IncomeTaxAtSource", operation, line.number, worksheetName)

  private def onTotal(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] = 
    onAttribute(line.cells(11))("Total", operation, line.number, worksheetName)

  private def onAttribute(attribute: Cell)(attributeHeader: String, operation: Operation, operationIndex: Int, worksheetName: String): ErrorsOr[Cell] = 
    if attribute.isEmpty then attribute.validNec
    else 
      val errorMessage: (String, String, String) => String = (lineOperation, cellFontColor, cellOperation) => s"The 'Operation' on line '$operationIndex' of 'Worksheet' '$worksheetName' looks like '$lineOperation' but, '$attributeHeader' has font-color $cellFontColor which denotes '$cellOperation'."
      operation match
        case a: SellingOperation =>       
          if attribute.fontColor != RED then attribute.validNec
          else UnexpectedContentColor(errorMessage("Selling", s"red('$RED')", "Buying")).invalidNec

        case a: BuyingOperation =>
          if attribute.fontColor != BLUE then attribute.validNec
          else UnexpectedContentColor(errorMessage("Buying", s"blue('$BLUE')", "Selling")).invalidNec

  private def assertVolumeIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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
  private def assertSettlementFeeIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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

  private def assertTradingFeesIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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

  private def assertServiceTaxIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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

  private def assertTotalIsCalculatedCorrectly: OperationValidation = line ⇒ worksheetName =>
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

  private def assertOperationsInBrokerageNoteHaveSameTradingDate: OperationsHarmonyCheck = (first: Line, second: Line) => worksheetName ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${second.cells.tail.head.value}') was found on 'Worksheet' '$worksheetName'. 'TradingDate's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondTradingDateCell.value}' in '${secondTradingDateCell.address}' is different from '${firstTradingDateCell.value}' in '${firstTradingDateCell.address}'."
    ).invalidNec
    else second.validNec

  private def assertOperationsInBrokerageNoteHaveSameNoteNumber: OperationsHarmonyCheck = (first: Line, second: Line) => worksheetName ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then UnexpectedContentValue(
      s"An invalid 'Group' ('${secondNoteNumberCell.value}') was found on 'Worksheet' '$worksheetName'. 'NoteNumber's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '${secondNoteNumberCell.value}' in '${secondNoteNumberCell.address}' is different from '${firstNoteNumberCell.value}' in '${firstNoteNumberCell.address}'."
    ).invalidNec
    else second.validNec

  private def assertVolumeSummary(volumeSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(5, "VolumeSummary", volumeSummaryChecks: _*)(worksheetName, group)

  private def assertSettlementFeeSummary(settlementFeeSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(6, "SettlementFeeSummary", settlementFeeSummaryChecks: _*)(worksheetName, group)

  private def assertTradingFeesSummary(tradingFeesSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(7, "TradingFeesSummary", tradingFeesSummaryChecks: _*)(worksheetName, group)

  private def assertBrokerageSummary(brokerageSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(8, "BrokerageSummary", brokerageSummaryChecks: _*)(worksheetName, group)

  private def assertServiceTaxSummary(serviceTaxSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(9, "ServiceTaxSummary", serviceTaxSummaryChecks: _*)(worksheetName, group)

  private def assertIncomeTaxAtSourceSummary(incomeTaxAtSourceSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(10, "IncomeTaxAtSourceSummary", incomeTaxAtSourceSummaryChecks: _*)(worksheetName, group)

  private def assertTotalSummary(totalSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ worksheetName =>
    assertSummaryAttribute(11, "TotalSummary", totalSummaryChecks: _*)(worksheetName, group)

  private def assertSummaryAttribute(attributeIndex: Int, attributeName: String, attributeSummaryChecks: SummaryAttributeCheck*)(worksheetName: String, group: Group): ErrorsOr[Group] =
    group.summary.map { summary ⇒
      given Semigroup[Cell] = (x, _) => x
      val summaryAttribute = summary.cells(attributeIndex)

      attributeSummaryChecks
        .map(_ (summaryAttribute)(attributeIndex, attributeName, summary.number, group, worksheetName))
        .reduce(_ combine _)
        .liftTo(group)(summary)
    }.getOrElse(group.validNec)

  private def isPresent: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, operationIndex, group, worksheetName) =>
    isPresent(summaryAttribute)(attributeHeader, operationIndex, worksheetName)

  private def isCalculatedCorrectly: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, operationIndex, group, worksheetName) =>
    val expectedSummaryValue = group.nonSummaryLines.foldLeft(0.0)((acc, line) ⇒ acc + line.cells(attributeIndex).asDouble.getOrElse(0.0))
    val actualSummaryValue = summaryAttribute.asDouble.getOrElse(0.0)

    if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
      s"An invalid calculated 'SummaryCell' ('${summaryAttribute.address}:${attributeHeader}') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all '${attributeHeader.replace("Summary", "")}'s in the 'Group' (${group.head.cells(attributeIndex).address}...${group.takeRight(2).head.cells(attributeIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
    ).invalidNec
    else summaryAttribute.validNec

  private def isOperationSensitive: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, operationIndex, group, worksheetName) =>
    val expectedSummaryValue = group.nonSummaryLines
      .foldLeft(0.0) { (acc, operation) ⇒
        val valueToSummarize = operation.cells(summaryAttribute.index).asDouble.getOrElse(0.0)
        if operation.hasMostCellsRed then
          acc - valueToSummarize
        else 
          acc + valueToSummarize
      }

    val actualSummaryValue = summaryAttribute.asDouble.getOrElse(0.0)

    if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
      s"An invalid calculated 'SummaryCell' ('${summaryAttribute.address}:${attributeHeader}') was found on 'Worksheet' '$worksheetName'. It was supposed to contain '${expectedSummaryValue.formatted("%.2f")}', which is the sum of all 'SellingOperation's '${attributeHeader.replace("Summary", "")}'s minus the sum of all 'BuyingOperation's '${attributeHeader.replace("Summary", "")}'s in the 'Group' (${group.head.cells(attributeIndex).address}...${group.takeRight(2).head.cells(attributeIndex).address}) but, it actually contained '${actualSummaryValue.formatted("%.2f")}'."
    ).invalidNec
    else summaryAttribute.validNec

  private def assertMultilineGroupHasSummary: BrokerageNoteValidation = group ⇒ worksheetName =>
    group.nonSummaryLines match
      case Seq(_, _, _*) ⇒ group.summary match
        case None ⇒ group.summaryLikeLine match
          case Some(summaryLikeLine) ⇒
            val invalidSummaryCells = summaryLikeLine.nonEmptyCells
              .filter(!_.isFormula)
              .map(attribute ⇒ s"${attribute.address}:${attribute.`type`}")
              .mkString("[", ",", "]")

            UnexpectedContentType(
              s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. All non-empty 'Cell's of a 'Group's 'Summary' are supposed to be formulas but, that's not the case with '$invalidSummaryCells'."
            ).invalidNec

          case _ ⇒ RequiredValueMissing(
            s"An invalid 'Group' ('${group.head.cells(1).value}') was found on 'Worksheet' '$worksheetName'. 'MultilineGroup's must have a 'SummaryLine'."
          ).invalidNec
        case _ ⇒ group.validNec
      case _ ⇒ group.validNec

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

    private def liftTo(group: Group)(line: Line): ErrorsOr[Group] = errorsOrCell.accumulate.liftTo(line).accumulate.liftTo(group)

  extension (errorsOrSeqOfCell: ErrorsOr[Seq[Cell]])

    @targetName("liftSeqOfCellToLine")
    private def liftTo(line: Line): ErrorsOr[Line] = errorsOrSeqOfCell.map(_ => line)

  extension (group: Group)

    private def validatedWith(
      attributeValidations: Seq[AttributeValidation] = Seq(),
      attributesHarmonyChecks: Seq[AttributesHarmonyCheck] = Seq(),
      operationValidations: Seq[OperationValidation] = Seq(),
      operationsHarmonyChecks: Seq[OperationsHarmonyCheck] = Seq(),
      brokerageNoteValidations: Seq[BrokerageNoteValidation] = Seq()
    )(worksheetName: String): ErrorsOr[Group] =
      given Semigroup[Group] = (x, _) => x
      given Semigroup[Line] = (x, _) => x
      val appliedBrokerageNoteValidations: ErrorsOr[Group] = brokerageNoteValidations.map(_ (group)(worksheetName)).reduce(_ combine _)
      val validateAndHarmonize: (Line => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.map(f).reduce(_ combine _)
      val applyHarmonyChecksTo: (Seq[Line] => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.sliding(2).map(f).reduce(_ combine _)
      val applyOperationsHarmonyChecksTo: (Line, Line) => ErrorsOr[Line] = (line1, line2) => operationsHarmonyChecks.map(_ (line1, line2)(worksheetName)).reduce(_ combine _)

      appliedBrokerageNoteValidations.combine{
        validateAndHarmonize{ line => 
          line.validatedWith(operationValidations)(worksheetName).accumulate.combine{
            line.harmonizedWith(attributesHarmonyChecks)(worksheetName).accumulate.combine{
              line.withCellsValidated(attributeValidations)(worksheetName).accumulate
            }
          }
        }.liftTo(group).combine{
          applyHarmonyChecksTo{ lineWindow ⇒
            lineWindow match {
              case Seq(line1, line2) ⇒ applyOperationsHarmonyChecksTo(line1, line2).accumulate
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
    
    private def validatedWith(operationValidations: Seq[OperationValidation])(worksheetName: String): ErrorsOr[Line] = 
      given Semigroup[Line] = (x, _) => x
      operationValidations.map(_ (line)(worksheetName)).reduce(_ combine _)

    private def withCellsValidated(attributeValidations: Seq[AttributeValidation])(worksheetName: String): ErrorsOr[Line] = 
      cells.map(_.validatedWith(attributeValidations)(worksheetName, line.number).accumulate).reduce(_ combine _).liftTo(line)

    private def harmonizedWith(attributesHarmonyChecks: Seq[AttributesHarmonyCheck])(worksheetName: String): ErrorsOr[Line] = 
      given Semigroup[Cell] = (x, _) => x
      val applyAttributesHarmonyChecksTo: (Cell, Cell) => ErrorsOr[Cell] = (cell1, cell2) => attributesHarmonyChecks.map(_ (cell1, cell2)(worksheetName)).reduce(_ combine _)
      val harmonized: (Seq[Cell] => ErrorsOr[Seq[Cell]]) => ErrorsOr[Seq[Cell]] = f ⇒ cells.sliding(2).map(f).reduce(_ combine _)
      
      if !attributesHarmonyChecks.isEmpty then
        harmonized{ cells ⇒
          cells match {
            case Seq(cell1, cell2) ⇒ applyAttributesHarmonyChecksTo(cell1, cell2).accumulate
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
    
    private def validatedWith(attributeValidations: Seq[AttributeValidation])(worksheetName: String, operationIndex: Int): ErrorsOr[Cell] =
      given Semigroup[Cell] = (x, _) => x
      attributeValidations.map(_ (cell)(worksheetName, operationIndex)).reduce(_ combine _)
      
    private def index: Int = cell.address.charAt(0) - UPPERCASE_A_ASCII

  extension (double: Double)

    @targetName("differentBeyondPrecision")
    private def !~=(other: Double)(using precision: Double): Boolean = (double - other).abs > precision

    private def formatted(format: String): String = String.format(Locale.US, format, double)

  extension (string: String)

    private def asDouble: Double = string.replace(",", ".").toDouble