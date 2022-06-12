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

// TODO Add a warning system: ErrorsOr[WarningsAnd[BrokerageNotesWorksheetReader]]
object BrokerageNotesWorksheetReader:
  enum BrokerageNoteReaderError(message: String):
    case RequiredValueMissing(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentValue(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentType(message: String) extends BrokerageNoteReaderError(message)
    case UnexpectedContentColor(message: String) extends BrokerageNoteReaderError(message)

  enum GroupType:
    case Homogeneous, Heterogeneous

  import BrokerageNoteReaderError.*
  import BrokerageNotesWorksheetMessages.*
  import GroupType.*

  type Error = BrokerageNoteReaderError | Worksheet.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]
  type ServiceDependencies = (AverageStockPriceService, SettlementFeeRateService, TradingFeesRateService, ServiceTaxRateService, IncomeTaxAtSourceRateService)
  private type Group = Seq[Line]
  private type AttributeValidation = Cell => (String, Int) ⇒ ErrorsOr[Cell]
  private type AttributeCheck = Cell ⇒ (String, Int, String) ⇒ ErrorsOr[Cell]
  private type AttributesHarmonyCheck = (Cell, Cell) ⇒ String ⇒ ErrorsOr[Cell]
  private type OperationValidation = Line ⇒ ServiceDependencies => String => ErrorsOr[Line]
  private type OperationsHarmonyCheck = (Line, Line) => String ⇒ ErrorsOr[Line]
  private type BrokerageNoteValidation = Group ⇒ String => ErrorsOr[Group]
  private type SummaryAttributeCheck = Cell ⇒ (Int, String, Int, Group, String) ⇒ ErrorsOr[Cell]

  private val RED = "255,0,0"
  private val BLUE = "91,155,213"
  private val UPPERCASE_A_ASCII = 65

  given comparisonPrecision: Double = 0.02

  given Semigroup[Group] = (x, y) => if x == y then x else x ++: y

  def from(worksheet: Worksheet)(serviceDependencies: ServiceDependencies): ErrorsOr[BrokerageNotesWorksheetReader] = worksheet.groups
    .map(_.validatedWith(
      attributeValidations(
        assertTradingDate(isPresent, isAValidDate, hasAValidFontColor),
        assertNoteNumber(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor),
        assertTicker(isPresent, hasAValidFontColor),
        assertQty(isPresent, isNotNegative, isAValidInteger, hasAValidFontColor),
        assertPrice(isPresent, isNotNegative, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertVolume(isPresent, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertSettlementFee(isPresent, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertTradingFees(isPresent, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertBrokerage(isPresent, isNotNegative, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertServiceTax(isPresent, isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertIncomeTaxAtSource(isAValidCurrency || isAValidDouble, hasAValidFontColor),
        assertTotal(isPresent, isAValidCurrency || isAValidDouble, hasAValidFontColor),
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
        assertVolumeSummary(isPresent, isAValidCurrency or isAValidDouble, isOperationTypeAwareWhenCalculated),
        assertSettlementFeeSummary(isPresent, isAValidCurrency or isAValidDouble, isCorrectlyCalculated),
        assertTradingFeesSummary(isPresent, isAValidCurrency or isAValidDouble, isCorrectlyCalculated),
        assertBrokerageSummary(isPresent, isAValidCurrency or isAValidDouble, isCorrectlyCalculated),
        assertServiceTaxSummary(isPresent, isAValidCurrency or isAValidDouble, isCorrectlyCalculated),
        assertIncomeTaxAtSourceSummary(isAValidCurrency or isAValidDouble, isCorrectlyCalculated),
        assertTotalSummary(isPresent, isAValidCurrency or isAValidDouble, isOperationTypeAwareWhenCalculated)
      )
    )(serviceDependencies)(worksheet.name).andThen(_.toBrokerageNote(worksheet.name).accumulate))
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
    else RequiredValueMissing(attributeMissing(attributeHeader, operationIndex)(worksheetName)).invalidNec

  private def isAValidDate: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.isEmpty || attribute.asLocalDate.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a date")(worksheetName)
    ).invalidNec

  private def hasAValidFontColor: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.isEmpty || attribute.fontColor.either(RED, BLUE) then attribute.validNec
    else UnexpectedContentColor(invalidAttributeColor(attribute.fontColor, attributeHeader, operationIndex)(worksheetName)).invalidNec

  private def isNotNegative: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.asDouble.forall(_ >= 0.0) then attribute.validNec
    else UnexpectedContentValue(unexpectedNegativeAttribute(attributeHeader, attribute.value, operationIndex)(worksheetName)).invalidNec

  private def isAValidInteger: AttributeCheck = attribute => (attributeHeader, operationIndex, worksheetName) =>
    if attribute.isEmpty || attribute.asInt.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("an integer number")(worksheetName)
    ).invalidNec

  private def isAValidCurrency(attribute: Cell)(attributeHeader: String, operationIndex: Int, worksheetName: String): ErrorsOr[Cell] =
    if attribute.isEmpty || attribute.isCurrency then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a currency")(worksheetName)
    ).invalidNec

  private def isAValidDouble(attribute: Cell)(attributeHeader: String, operationIndex: Int, worksheetName: String): ErrorsOr[Cell] =
    if attribute.isEmpty || attribute.asDouble.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a double")(worksheetName)
    ).invalidNec

  private def assertFontColorReflectsOperationType(attributeColorChecks: Operation => (Line, String) => ErrorsOr[Cell]*): OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    line.toMostLikelyOperation(worksheetName)
      .andThen{operation =>
        attributeColorChecks
          .map(_(operation)(line, worksheetName).map(Seq(_)))
          .reduce(_ combine _)
          .map(_=> line)
      }

  private def onTradingDate(operation: Operation)(line: Line, worksheetName: String): ErrorsOr[Cell] =
    onAttribute(line.cells.head)("TradingDate", operation, line.number, worksheetName)

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
      val errorMessage: (String, String, String) => String = (currentOperation, attributeColor, operationDenoted) =>
        unexpectedAttributeColor(attributeColor, attributeHeader, operationIndex)(operationDenoted, currentOperation)(worksheetName)
      operation match
        case _: SellingOperation =>
          if attribute.fontColor != RED then attribute.validNec
          else UnexpectedContentColor(errorMessage("Selling", s"red($RED)", "Buying")).invalidNec

        case _: BuyingOperation =>
          if attribute.fontColor != BLUE then attribute.validNec
          else UnexpectedContentColor(errorMessage("Buying", s"blue($BLUE)", "Selling")).invalidNec

  private def assertVolumeIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    val qtyCell = line.cells(3)
    val priceCell = line.cells(4)
    val volumeCell = line.cells(5)
    val qty = qtyCell.asInt.getOrElse(0)
    val price = priceCell.asDouble.getOrElse(0.0)

    val actualVolume = volumeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    val expectedVolume = (qty * price).formatted("%.2f")

    if actualVolume != expectedVolume then UnexpectedContentValue(
      unexpectedVolume(actualVolume, line.number)(expectedVolume, qtyCell.value, price.formatted("%.2f"))(worksheetName)
    ).invalidNec
    else line.validNec

  // TODO Think about adding a reason to the errors for the cases where the requirements for the validation are not present. For instance, in the case below, any one of the following, if missing or having any other problem, would result in an impossible validation: tradingDate, volume and, actualSettlementFee. For now, we'll choose default values for them in case of problem and the validation will fail because of them. Hopefully, the original cause will be caught by other validation.
  private def assertSettlementFeeIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    val settlementFeeRateService = serviceDependencies(1)
    val volumeCell = line.cells(5)
    val settlementFeeCell = line.cells(6)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualSettlementFee = settlementFeeCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Actually detecting the correct 'OperationalMode' may prove challenging when creating a 'BrokerageNote', unless it happens in real-time, since the difference between 'Normal' and 'DayTrade' is actually time-related. A 'BrokerageNote' instance is supposed to be created when a brokerage note document is detected in the filesystem or is provided to the system by any other means. That document contains only the 'TradingDate' and not the time so, unless the system is provided with information about the brokerage note document as soon as an 'Order' gets executed (the moment that gives birth to a brokerage note), that won't be possible. It is important to note that, generally, brokerage notes are not made available by 'Broker's until the day after the fact ('Operation's for the whole day are grouped in a brokerage note, that's why). Maybe we should try a different try and error approach when ingesting a brokerage note document: First we try to check the calculation of the 'SettlementFee' assuming the 'Normal' 'OperationMode' and if that does not work, than we switch it to 'DayTrade' and try again. If that does not work, then we have found a problem with the calculation applied by the 'Broker'.
    val settlementFeeRate = settlementFeeRateService.forOperationalMode(Normal).at(tradingDate).value
    val expectedSettlementFee = (volume * settlementFeeRate).formatted("%.2f")

    if actualSettlementFee != expectedSettlementFee then UnexpectedContentValue(
      unexpectedSettlementFee(actualSettlementFee, line.number)(expectedSettlementFee, volume.formatted("%.2f"), (settlementFeeRate * 100).formatted("%.4f%%"))(worksheetName)
    ).invalidNec
    else line.validNec

  private def assertTradingFeesIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    val tradingFeesRateService = serviceDependencies(2)
    val volumeCell = line.cells(5)
    val tradingFeesCell = line.cells(7)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val tradingPeriod = TradingPeriod.TRADING
    val volume = volumeCell.asDouble.getOrElse(0.0)
    val actualTradingFees = tradingFeesCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO Same challenge here since 'TradingFees' is also dependent on the time of order execution which is not part of the brokerage note document.
    val tradingFeesRate = tradingFeesRateService.at(tradingDate, tradingPeriod)
    val expectedTradingFees = (volume * tradingFeesRate).formatted("%.2f")

    if actualTradingFees != expectedTradingFees then UnexpectedContentValue(
      unexpectedTradingFees(actualTradingFees, line.number)(expectedTradingFees, volume.formatted("%.2f"), (tradingFeesRate * 100).formatted("%.4f%%"))(worksheetName)
    ).invalidNec
    else line.validNec

  private def assertServiceTaxIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    val serviceTaxRateService = serviceDependencies(3)
    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val actualServiceTax = serviceTaxCell.asDouble.getOrElse(0.0).formatted("%.2f")
    // TODO The city used to calculate the ServiceTax can be determined, in the future, by looking into the Broker information present in the brokerage note document.
    val serviceTaxRate = serviceTaxRateService.at(tradingDate).value
    val expectedServiceTax = (brokerage * serviceTaxRate).formatted("%.2f")

    if actualServiceTax != expectedServiceTax then UnexpectedContentValue(
      unexpectedServiceTax(actualServiceTax, line.number)(expectedServiceTax, brokerage.formatted("%.2f"), (serviceTaxRate * 100).formatted("%.1f%%"))(worksheetName)
    ).invalidNec
    else line.validNec

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
    val averageStockPriceService = serviceDependencies(0)
    val incomeTaxAtSourceRateService = serviceDependencies(4)
    val incomeTaxAtSourceCell = line.cells(10)
    // TODO IncomeTaxAtSource can never be negative. It is not like I can restitute it if I have a loss. Restitutions do not occur at the source
    val actualIncomeTaxAtSource = incomeTaxAtSourceCell.asDouble.getOrElse(0.0)

    if line.hasMostNonEmptyValidColoredCellsBlue then
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
      val operationAverageCost = averageStockPriceService.forTicker(tickerCell.value) * qty
      // TODO When the ticker cannot be found in the portfolio, 0.0 is returned which should trigger an exception since I'm trying to sell something I do not posses. For now, I'll tweak TEST_SPREADSHEET so that all BuyingOperations refer to VALE5 and have the appropriate calculation for the IncomeTaxAtSource.
      val operationProfit = operationNetResult - operationAverageCost
      val incomeTaxAtSourceRate = incomeTaxAtSourceRateService.forOperationalMode(Normal).at(tradingDate).value
      val expectedIncomeTaxAtSource = operationProfit * incomeTaxAtSourceRate

      if actualIncomeTaxAtSource !~= expectedIncomeTaxAtSource then UnexpectedContentValue(
        unexpectedIncomeTaxAtSourceForSellings(actualIncomeTaxAtSource.formatted("%.2f"), line.number)(expectedIncomeTaxAtSource.formatted("%.2f"), operationProfit.formatted("%.2f"), (incomeTaxAtSourceRate * 100).formatted("%.4f%%"))(worksheetName)
      ).invalidNec
      else line.validNec
    else
      given Semigroup[Line] = (x, _) => x

      if incomeTaxAtSourceCell.isNotEmpty then {
        if !incomeTaxAtSourceCell.isCurrency then UnexpectedContentType(
          unexpectedIncomeTaxAtSourceForBuyings(incomeTaxAtSourceCell.value, line.number)(worksheetName)
        ).invalidNec
        else line.validNec
      } combine {
        if actualIncomeTaxAtSource > 0.0 then UnexpectedContentValue(
          unexpectedIncomeTaxAtSourceForBuyings(incomeTaxAtSourceCell.value, line.number)(worksheetName)
        ).invalidNec
        else line.validNec
      }
      else line.validNec

  private def assertTotalIsCalculatedCorrectly: OperationValidation = line ⇒ serviceDependencies => worksheetName =>
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

    if line.hasMostNonEmptyValidColoredCellsBlue then
      val expectedTotal = volume - settlementFee - tradingFees - brokerage - serviceTax

      if actualTotal !~= expectedTotal then UnexpectedContentValue(
        unexpectedTotalForSellings(actualTotal.formatted("%.2f"), line.number)(expectedTotal.formatted("%.2f"))(worksheetName)
      ).invalidNec
      else line.validNec
    else
      val expectedTotal = volume + settlementFee + tradingFees + brokerage + serviceTax

      if actualTotal !~= expectedTotal then UnexpectedContentValue(
        unexpectedTotalForBuyings(actualTotal.formatted("%.2f"), line.number)(expectedTotal.formatted("%.2f"))(worksheetName)
      ).invalidNec
      else line.validNec

  private def assertOperationsInBrokerageNoteHaveSameTradingDate: OperationsHarmonyCheck = (first: Line, second: Line) => worksheetName ⇒
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then UnexpectedContentValue(
      conflictingTradingDate(
        secondTradingDateCell.address, second.cells.tail.head.value, secondTradingDateCell.value
      )(
        firstTradingDateCell.value, firstTradingDateCell.address
      )(worksheetName)
    ).invalidNec
    else second.validNec

  private def assertOperationsInBrokerageNoteHaveSameNoteNumber: OperationsHarmonyCheck = (first: Line, second: Line) => worksheetName ⇒
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then UnexpectedContentValue(
      conflictingNoteNumber(
        secondNoteNumberCell.address, second.cells.tail.head.value, secondNoteNumberCell.value
      )(
        firstNoteNumberCell.value, firstNoteNumberCell.address
      )(worksheetName)
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

  private def isPresent: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _, worksheetName) =>
    isPresent(summaryAttribute)(attributeHeader, operationIndex, worksheetName)

  private def isAValidCurrency: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _, worksheetName) =>
    isAValidCurrency(summaryAttribute)(attributeHeader, operationIndex, worksheetName)

  private def isAValidDouble: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _, worksheetName) =>
    isAValidDouble(summaryAttribute)(attributeHeader, operationIndex, worksheetName)

  private def isCorrectlyCalculated: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, _, group, worksheetName) =>
    val expectedSummaryValue = group.nonSummaryLines.foldLeft(0.0)((acc, line) ⇒ acc + line.cells(attributeIndex).asDouble.getOrElse(0.0))
    val actualSummaryValue = summaryAttribute.asDouble.getOrElse(0.0)

    if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
      unexpectedValueForCalculatedSummaryAttribute(actualSummaryValue.formatted("%.2f"), attributeHeader, summaryAttribute.address)(expectedSummaryValue.formatted("%.2f"), s"the sum of all '${attributeHeader.replace("Summary", "")}'s in the 'Group' (${group.head.cells(attributeIndex).address}...${group.takeRight(2).head.cells(attributeIndex).address})")(worksheetName)
    ).invalidNec
    else summaryAttribute.validNec

  private def isOperationTypeAwareWhenCalculated: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, operationIndex, group, worksheetName) =>
    val valueToSummarize: Line => Double = _.cells(summaryAttribute.index).asDouble.getOrElse(0.0)
    val attributeName = attributeHeader.replace("Summary", "")
    val attributeLetter = (attributeIndex + 65).toChar
    val indexOfFirstOperation = group.head.number
    val indexOfLarstOperation = group.takeRight(2).head.number
    val (expectedSummaryValue, formulaDescription) = group.`type` match
      case Homogeneous => (group.nonSummaryLines.map(valueToSummarize).sum, operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups(attributeName, attributeLetter, indexOfFirstOperation, indexOfLarstOperation))
      case Heterogeneous => (group.nonSummaryLines.foldLeft(0.0) { (acc, operation) ⇒
        if operation.hasMostNonEmptyValidColoredCellsRed then
          acc - valueToSummarize(operation)
        else
          acc + valueToSummarize(operation)
      },
      operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups(attributeName, attributeLetter, indexOfFirstOperation, indexOfLarstOperation)
      )

    val actualSummaryValue = summaryAttribute.asDouble.getOrElse(0.0)

    if actualSummaryValue.abs !~= expectedSummaryValue.abs then UnexpectedContentValue(
      unexpectedOperationTypeAwareAttributeSummary(actualSummaryValue.formatted("%.2f"), attributeHeader, operationIndex)(expectedSummaryValue.formatted("%.2f"), attributeLetter, formulaDescription)(worksheetName)
    ).invalidNec
    else summaryAttribute.validNec

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

  extension (thisAttributeCheck: AttributeCheck)
    @targetName("orElse")
    private def ||(thatAttributeCheck: AttributeCheck): AttributeCheck = cell ⇒ (s1, i, s2) ⇒
      thisAttributeCheck(cell)(s1, i, s2).findValid(thatAttributeCheck(cell)(s1, i, s2))

  extension (thisSummaryAttributeCheck: SummaryAttributeCheck)
    private def or(thatSummaryAttributeCheck: SummaryAttributeCheck): SummaryAttributeCheck = cell ⇒ (i1, s1, i2, g, s3) ⇒
      thisSummaryAttributeCheck(cell)(i1, s1, i2, g, s3).findValid(thatSummaryAttributeCheck(cell)(i1, s1, i2, g, s3))

  extension (group: Group)

    private def validatedWith(
      attributeValidations: Seq[AttributeValidation] = Seq(),
      attributesHarmonyChecks: Seq[AttributesHarmonyCheck] = Seq(),
      operationValidations: Seq[OperationValidation] = Seq(),
      operationsHarmonyChecks: Seq[OperationsHarmonyCheck] = Seq(),
      brokerageNoteValidations: Seq[BrokerageNoteValidation] = Seq()
    )(serviceDependencies: ServiceDependencies)(worksheetName: String): ErrorsOr[Group] =
      given Semigroup[Group] = (x, _) => x
      given Semigroup[Line] = (x, _) => x
      val appliedBrokerageNoteValidations: ErrorsOr[Group] = brokerageNoteValidations.map(_ (group)(worksheetName)).reduce(_ combine _)
      val validateAndHarmonize: (Line => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.map(f).reduce(_ combine _)
      val applyHarmonyChecksToLineWindow: (Seq[Line] => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.sliding(2).map(f).reduce(_ combine _)
      val applyOperationsHarmonyChecksTo: (Line, Line) => ErrorsOr[Line] = (line1, line2) => operationsHarmonyChecks.map(_ (line1, line2)(worksheetName)).reduce(_ combine _)

      appliedBrokerageNoteValidations.combine{
        validateAndHarmonize{ line =>
          line.validatedWith(operationValidations)(serviceDependencies)(worksheetName).accumulate.combine{
            line.harmonizedWith(attributesHarmonyChecks)(worksheetName).accumulate.combine{
              line.withCellsValidated(attributeValidations)(worksheetName).accumulate
            }
          }
        }.liftTo(group).combine{
          applyHarmonyChecksToLineWindow {
            case Seq(line1, line2) ⇒ applyOperationsHarmonyChecksTo(line1, line2).accumulate
            case Seq(line) ⇒ line.validNec.accumulate
          }
        }
      }

    private def toBrokerageNote(worksheetName: String): ErrorsOr[BrokerageNote] =
      nonSummaryLines
        .map(_.toOperation(worksheetName).map(Seq(_)))
        .reduce(_ combine _)
        .map(BrokerageNote(_, group.head.toFinancialSummary))

    private def nonSummaryLines: Seq[Line] = summary.map(_ => group.init).getOrElse(group)

    private def summary: Option[Line] = if group.size <= 1 then None else group.lastOption

    private def `type`: GroupType = nonSummaryLines.distinctBy(_.toMostLikelyOperation("").map(_.getClass)).size match
      case 1 => Homogeneous
      case _ => Heterogeneous

  extension (line: Line)

    private def hasMostNonEmptyValidColoredCellsFromColor(fontColor: String): Boolean = 
      nonEmptyCells
        .filter(_.fontColor.either(RED, BLUE))
        .count(_.fontColor == fontColor) > nonEmptyCells.size / 2

    private def hasMostNonEmptyValidColoredCellsRed: Boolean = hasMostNonEmptyValidColoredCellsFromColor(RED)

    private def hasMostNonEmptyValidColoredCellsBlue: Boolean = hasMostNonEmptyValidColoredCellsFromColor(BLUE)

    private def allNonEmptyCellsHave(fontColor: String): Boolean = nonEmptyCells.forall(_.fontColor == fontColor)

    private def allNonEmptyCellsAreBlue: Boolean = allNonEmptyCellsHave(BLUE)

    private def allNonEmptyCellsAreRed: Boolean = allNonEmptyCellsHave(RED)

    private def nonEmptyCells: Seq[Cell] = cells.filter(_.isNotEmpty)

    private def cells: Seq[Cell] = line.cells

    private def toMostLikelyOperation(worksheetName: String): ErrorsOr[Operation] =
      if hasMostNonEmptyValidColoredCellsBlue then
        SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else if hasMostNonEmptyValidColoredCellsRed then
        BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else UnexpectedContentColor(
        impossibleToDetermineMostLikelyOperationType(line.number)(worksheetName)
      ).invalidNec

    private def toOperation(worksheetName: String): ErrorsOr[Operation] =
      if allNonEmptyCellsAreBlue then
        SellingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else if allNonEmptyCellsAreRed then
        BuyingOperation(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value).validNec
      else UnexpectedContentColor(
        impossibleToDetermineOperationType(line.number)(worksheetName)
      ).invalidNec

    private def toFinancialSummary: FinancialSummary =
      FinancialSummary(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)

    private def validatedWith(operationValidations: Seq[OperationValidation])(serviceDependencies: ServiceDependencies)(worksheetName: String): ErrorsOr[Line] =
      given Semigroup[Line] = (x, _) => x
      operationValidations.map(_ (line)(serviceDependencies)(worksheetName)).reduce(_ combine _)

    private def withCellsValidated(attributeValidations: Seq[AttributeValidation])(worksheetName: String): ErrorsOr[Line] =
      cells.map(_.validatedWith(attributeValidations)(worksheetName, line.number).accumulate).reduce(_ combine _).liftTo(line)

    private def harmonizedWith(attributesHarmonyChecks: Seq[AttributesHarmonyCheck])(worksheetName: String): ErrorsOr[Line] =
      given Semigroup[Cell] = (x, _) => x
      val applyAttributesHarmonyChecksTo: (Cell, Cell) => ErrorsOr[Cell] = (cell1, cell2) => attributesHarmonyChecks.map(_ (cell1, cell2)(worksheetName)).reduce(_ combine _)
      val harmonizedCells: (Seq[Cell] => ErrorsOr[Seq[Cell]]) => ErrorsOr[Seq[Cell]] = f ⇒ cells.sliding(2).map(f).reduce(_ combine _)

      if attributesHarmonyChecks.nonEmpty then
        harmonizedCells {
          case Seq(cell1, cell2) ⇒ applyAttributesHarmonyChecksTo(cell1, cell2).accumulate
          case Seq(cell) ⇒ cell.validNec.accumulate
        }.liftTo(line)
      else line.validNec

  extension (cell: Cell)

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

    private def either(left: String, right: String): Boolean = string == left || string == right

object BrokerageNotesWorksheetMessages:
  private val RED = "255,0,0"
  private val BLUE = "91,155,213"

  def attributeMissing(attributeName: String, operationIndex: Int)(worksheetName: String): String =
    s"A required attribute ('$attributeName') is missing on line '$operationIndex' of 'Worksheet' '$worksheetName'."

  def summaryLineMissing(noteNumber: String)(worksheetName: String): String =
    s"An invalid 'Group' ('$noteNumber') was found on 'Worksheet' '$worksheetName'. 'MultilineGroup's must have a 'SummaryLine'."

  def unexpectedContentType(attributeValue: String, attributeName: String, operationIndex: Int)(expectedDataType: String)(worksheetName: String) =
    s"'$attributeName' ('$attributeValue') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be interpreted as $expectedDataType."

  def invalidAttributeColor(attributeColor: String, attributeName: String, operationIndex: Int)(worksheetName: String) =
    s"'$attributeName's font-color ('$attributeColor') on line '$operationIndex' of 'Worksheet' '$worksheetName' can only be red ('$RED') or blue ('$BLUE')."

  def unexpectedAttributeColor(attributeColor: String, attributeName: String, operationIndex: Int)(operationDenoted: String, currentOperation: String)(worksheetName: String): String =
    s"The 'Operation' on line '$operationIndex' of 'Worksheet' '$worksheetName' looks like '$currentOperation' but, '$attributeName' has font-color '$attributeColor' which denotes '$operationDenoted'."

  private def impossibleToDetermineOperationType(operationIndex: Int, reason: String)(worksheetName: String): String =
    s"Impossible to determine the type of 'Operation' on line '$operationIndex' of 'Worksheet' '$worksheetName' due to $reason."

  private val halfOfTheAttributesFromEachValidColor = "exactly half of the non-empty valid-colored 'Attribute's of each valid color"
  def impossibleToDetermineMostLikelyOperationType(operationIndex: Int)(worksheetName: String): String =
    impossibleToDetermineOperationType(operationIndex, halfOfTheAttributesFromEachValidColor)(worksheetName)
      
  private val attributesWithDivergentFontColors = "its 'Attribute's having divergent font colors"
  def impossibleToDetermineOperationType(operationIndex: Int)(worksheetName: String): String =
    impossibleToDetermineOperationType(operationIndex, attributesWithDivergentFontColors)(worksheetName)

  private def conflictingAttribute(attributeName: String, attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(worksheetName: String) = 
    s"An invalid 'Group' ('$noteNumber') was found on 'Worksheet' '$worksheetName'. '$attributeName's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '$actualValue' in '$attributeAddress' is different from '$expectedValue' in '$targetAttributeAddress'."

  def conflictingTradingDate(attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(worksheetName: String): String = 
    conflictingAttribute("TradingDate", attributeAddress, noteNumber, actualValue)(expectedValue, targetAttributeAddress)(worksheetName)

  def conflictingNoteNumber(attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(worksheetName: String): String = 
    conflictingAttribute("NoteNumber", attributeAddress, noteNumber, actualValue)(expectedValue, targetAttributeAddress)(worksheetName)

  def unexpectedNegativeAttribute(attributeName: String, attributeValue: String, operationIndex: Int)(worksheetName: String): String =
    s"'$attributeName' ('$attributeValue') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be negative."

  private def unexpectedAttributeValue(actualValue: String, attributeName: String, attributeAddress: String, attributeType: String)(expectation: String)(worksheetName: String): String =
    s"An invalid calculated '$attributeType' ('$attributeAddress:$attributeName') was found on 'Worksheet' '$worksheetName'. It was supposed to $expectation but, it actually contained '$actualValue'."

  private val incomeTaxAtSourceForBuyingsExpectation = "be either empty or equal to '0.00'"
  def unexpectedIncomeTaxAtSourceForBuyings(actualValue: String, operationIndex: Int)(worksheetName: String): String =
    unexpectedAttributeValue(
      actualValue, "IncomeTaxAtSource", s"K$operationIndex", "Cell"
    )(
      incomeTaxAtSourceForBuyingsExpectation
    )(worksheetName)

  private def calculatedAttributeExpectation(expectedValue: String, formulaDescription: String) = s"contain '$expectedValue', which is equal to '$formulaDescription'"
  private def unexpectedValueForCalculatedAttribute(actualValue: String, attributeName: String, attributeAddress: String)(expectedValue: String, formulaDescription: String) (worksheetName: String) =
    unexpectedAttributeValue(
      actualValue, attributeName, attributeAddress, "Cell"
    )(
      calculatedAttributeExpectation(expectedValue, formulaDescription)
    )(worksheetName)

  private def volumeFormulaDescription(qty: String, price: String)(operationIndex: Int) = s"D$operationIndex:Qty * E$operationIndex:Price ($qty * $price)"
  def unexpectedVolume(actualValue: String, operationIndex: Int)(expectedValue: String, qty: String, price: String)(worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "Volume", s"F$operationIndex"
    )(
      expectedValue, volumeFormulaDescription(qty, price)(operationIndex)
    )(worksheetName)

  private def settlementFeeFormulaDescription(volume: String, settlementFeeRate: String)(operationIndex: Int) =
    s"F$operationIndex:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' ($volume * $settlementFeeRate)"
  def unexpectedSettlementFee(actualValue: String, operationIndex: Int)(expectedValue: String, volume: String, settlementFeeRate: String)(worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "SettlementFee", s"G$operationIndex"
    )(
      expectedValue, settlementFeeFormulaDescription(volume, settlementFeeRate)(operationIndex)
    )(worksheetName)

  private def tradingFeesFormulaDescription(volume: String, tradingFeesRate: String)(operationIndex: Int): String =
    s"F$operationIndex:Volume * 'TradingFeesRate' at 'TradingDateTime' ($volume * $tradingFeesRate)"
  def unexpectedTradingFees(actualValue: String, operationIndex: Int)(expectedValue: String, volume: String, tradingFeesRate: String)(worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "TradingFees", s"H$operationIndex"
    )(
      expectedValue, tradingFeesFormulaDescription(volume, tradingFeesRate)(operationIndex)
    )(worksheetName)

  private def serviceTaxFormulaDescription(brokerage: String, serviceTaxRate: String)(operationIndex: Int): String =
    s"I$operationIndex:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' ($brokerage * $serviceTaxRate)"
  def unexpectedServiceTax(actualValue: String, operationIndex: Int)(expectedValue: String, brokerage: String, serviceTaxRate: String)(worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "ServiceTax", s"J$operationIndex"
    )(
      expectedValue, serviceTaxFormulaDescription(brokerage, serviceTaxRate)(operationIndex)
    )(worksheetName)

  private def incomeTaxAtSourceRateFormulaDescription(acquisitionCost: String, incomeTaxAtSourceRate: String)(operationIndex: Int): String =
    s"(('F$operationIndex:Volume' - 'G$operationIndex:SettlementFee' - 'H$operationIndex:TradingFees' - 'I$operationIndex:Brokerage' - 'J$operationIndex:ServiceTax') - ('AverageStockPrice' for the 'C$operationIndex:Ticker' * 'D$operationIndex:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' ($acquisitionCost * $incomeTaxAtSourceRate)"
  def unexpectedIncomeTaxAtSourceForSellings(actualValue: String, operationIndex: Int) (expectedValue: String, acquisitionCost: String, incomeTaxAtSourceRate: String) (worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "IncomeTaxAtSource", s"K$operationIndex"
    )(
      expectedValue, incomeTaxAtSourceRateFormulaDescription(acquisitionCost, incomeTaxAtSourceRate)(operationIndex)
    )(worksheetName)

  private def unexpectedTotal(actualValue: String, operationIndex: Int)(expectedValue: String, formulaDescription: String)(worksheetName: String) =
    unexpectedValueForCalculatedAttribute(
      actualValue, "Total", s"L$operationIndex"
    )(
      expectedValue, formulaDescription
    )(worksheetName)

  private def buyingTotalFormulaDescription(operationIndex: Int) =
    s"'F$operationIndex:Volume' + 'G$operationIndex:SettlementFee' + 'H$operationIndex:TradingFees' + 'I$operationIndex:Brokerage' + 'J$operationIndex:ServiceTax'"
  def unexpectedTotalForBuyings(actualValue: String, operationIndex: Int)(expectedValue: String)(worksheetName: String): String =
    unexpectedTotal(
      actualValue, operationIndex
    )(
      expectedValue, buyingTotalFormulaDescription(operationIndex)
    )(worksheetName)

  private def sellingTotalFormulaDescription(operationIndex: Int) =
    s"'F$operationIndex:Volume' - 'G$operationIndex:SettlementFee' - 'H$operationIndex:TradingFees' - 'I$operationIndex:Brokerage' - 'J$operationIndex:ServiceTax'"
  def unexpectedTotalForSellings(actualValue: String, operationIndex: Int)(expectedValue: String)(worksheetName: String): String =
    unexpectedTotal(
      actualValue, operationIndex
    )(
      expectedValue, sellingTotalFormulaDescription(operationIndex)
    )(worksheetName)

  private def calculatedSummaryAttributeExpectation(expectedValue: String, formulaDescription: String) =
    s"contain '$expectedValue', which is $formulaDescription"
  def unexpectedValueForCalculatedSummaryAttribute(actualValue: String, attributeName: String, summaryAddress: String)(expectedValue: String, formulaDescription: String)(worksheetName: String): String =
    unexpectedAttributeValue(
      actualValue, attributeName, summaryAddress, "SummaryCell"
    )(
      calculatedSummaryAttributeExpectation(expectedValue, formulaDescription)
    )(worksheetName)

  def operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all 'Operation's '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"

  def operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all 'SellingOperation's '$attributeName's minus the sum of all 'BuyingOperation's '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"
  def unexpectedOperationTypeAwareAttributeSummary(actualValue: String, attributeName: String, summaryIndex: Int)(expectedValue: String, attributeLetter: Char, formulaDescription: String)(worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, attributeName, s"$attributeLetter$summaryIndex"
    )(
      expectedValue, formulaDescription
    )(worksheetName)