package com.andreidiego.mpfi.stocks.adapter.files.readers.spreadsheets

import com.andreidiego.mpfi.stocks.adapter.services
import services.operationalmodes.OperationalMode
import services.operationalmodes.OperationalMode.{DayTrade, Normal}
import services.tradingperiods.TradingPeriod
import services.tradingperiods.TradingPeriod.{TRADING, PRE_OPENING}

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote(val operations: Seq[Operation], val financialSummary: FinancialSummary)

abstract sealed class Operation(val volume: String, val settlementFee: String, val tradingFees: String, val brokerage: String, val serviceTax: String, val incomeTaxAtSource: String, val total: String, tradingPeriod: TradingPeriod, operationalMode: OperationalMode):
  def isDayTrade: Boolean = operationalMode == DayTrade
  def isCarriedOutAt(tradingPeriod: TradingPeriod): Boolean = this.tradingPeriod == tradingPeriod

// TODO Add test for turning BuyingOperation into a case class
case class BuyingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String, tradingPeriod: TradingPeriod = TRADING, operationalMode: OperationalMode = Normal)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total, tradingPeriod, operationalMode)

// TODO Add test for turning SellingOperation into a case class
case class SellingOperation(override val volume: String, override val settlementFee: String, override val tradingFees: String, override val brokerage: String, override val serviceTax: String, override val incomeTaxAtSource: String, override val total: String, tradingPeriod: TradingPeriod = TRADING, operationalMode: OperationalMode = Normal)
  extends Operation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total, tradingPeriod, operationalMode)

// TODO Add test for turning FinancialSummary into a case class
case class FinancialSummary(volume: String, settlementFee: String, tradingFees: String, brokerage: String, serviceTax: String, incomeTaxAtSource: String, total: String)

// TODO Add a warning system: ErrorsOr[WarningsAnd[BrokerageNotesWorksheetReader]]
object BrokerageNotesWorksheetReader:
  import java.util.Locale
  import java.time.LocalDate
  import scala.annotation.targetName
  import cats.implicits.*
  import cats.kernel.Semigroup
  import cats.data.ValidatedNec
  import cats.syntax.validated.*
  import excel.poi.{Cell, Line, Worksheet}
  import services.averagestockprice.AverageStockPriceService
  import services.incometaxatsourcerate.IncomeTaxAtSourceRateService
  import services.servicetaxrate.ServiceTaxRateService
  import services.settlementfeerate.SettlementFeeRateService
  import services.tradingfeesrate.TradingFeesRateService

  enum BrokerageNotesReaderError(message: String):
    case RequiredValueMissing(message: String) extends BrokerageNotesReaderError(message)
    case UnexpectedContentValue(message: String) extends BrokerageNotesReaderError(message)
    case UnexpectedContentType(message: String) extends BrokerageNotesReaderError(message)
    case UnexpectedContentColor(message: String) extends BrokerageNotesReaderError(message)

  private enum GroupType:
    case Homogeneous, Heterogeneous

  import BrokerageNotesReaderError.*
  import BrokerageNotesWorksheetMessages.*
  import GroupType.*

  type Error = BrokerageNotesReaderError | Worksheet.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]
  type ServiceDependencies = (AverageStockPriceService, SettlementFeeRateService, TradingFeesRateService, ServiceTaxRateService, IncomeTaxAtSourceRateService)
  private type Group = Seq[Line]
  private type AttributeValidation = Cell => Int => String ?=> ErrorsOr[Cell]
  private type AttributeCheck = Cell ⇒ (String, Int) => String ?=> ErrorsOr[Cell]
  private type AttributesHarmonyCheck = (Cell, Cell) ⇒ String ?=> ErrorsOr[Cell]
  private type OperationValidation = Line ⇒ ServiceDependencies ?=> String ?=> ErrorsOr[Line]
  private type AttributeColorCheck = Operation => Line => String ?=> ErrorsOr[Cell]
  private type OperationsHarmonyCheck = (Line, Line) => String ?=> ErrorsOr[Line]
  private type BrokerageNoteValidation = Group ⇒ String ?=> ErrorsOr[Group]
  private type SummaryAttributeCheck = Cell ⇒ (Int, String, Int, Group) => String ?=> ErrorsOr[Cell]

  private val RED = "255,0,0"
  private val BLUE = "91,155,213"
  private val ORANGE = "252,228,214"
  private val UPPERCASE_A_ASCII = 65

  given comparisonPrecision: Double = 0.02

  given Semigroup[Group] = (x, y) => if x == y then x else x ++: y

  def from(worksheet: Worksheet)(using serviceDependencies: ServiceDependencies): ErrorsOr[BrokerageNotesWorksheetReader] = 
    given String = worksheet.name

    worksheet.groups.map(_.validatedWith(
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
    ).andThen(_.toBrokerageNote.accumulate))
    .reduce(_ combine _)
    .map(BrokerageNotesWorksheetReader(_))

  private def attributeValidations(attributeValidations: AttributeValidation*) = attributeValidations

  private def attributesHarmonyChecks(attributesHarmonyChecks: AttributesHarmonyCheck*) = attributesHarmonyChecks
  
  private def operationValidations(operationValidations: OperationValidation*) = operationValidations
  
  private def operationsHarmonyChecks(operationsHarmonyChecks: OperationsHarmonyCheck*) = operationsHarmonyChecks
  
  private def brokerageNoteValidations(brokerageNoteValidations: BrokerageNoteValidation*) = brokerageNoteValidations

  private def assertTradingDate(tradingDateChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "TradingDate", _.isTradingDate, tradingDateChecks: _*)(operationIndex)

  private def assertNoteNumber(noteNumberChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "NoteNumber", _.isNoteNumber, noteNumberChecks: _*)(operationIndex)

  private def assertTicker(tickerChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Ticker", _.isTicker, tickerChecks: _*)(operationIndex)

  private def assertQty(qtyChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Qty", _.isQty, qtyChecks: _*)(operationIndex)

  private def assertPrice(priceChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Price", _.isPrice, priceChecks: _*)(operationIndex)

  private def assertVolume(volumeChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Volume", _.isVolume, volumeChecks: _*)(operationIndex)

  private def assertSettlementFee(settlementFeeChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "SettlementFee", _.isSettlementFee, settlementFeeChecks: _*)(operationIndex)

  private def assertTradingFees(tradingFeesChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "TradingFees", _.isTradingFees, tradingFeesChecks: _*)(operationIndex)

  private def assertBrokerage(brokerageChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Brokerage", _.isBrokerage, brokerageChecks: _*)(operationIndex)

  private def assertServiceTax(serviceTaxChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "ServiceTax", _.isServiceTax, serviceTaxChecks: _*)(operationIndex)

  private def assertIncomeTaxAtSource(incomeTaxAtSourceChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "IncomeTaxAtSource", _.isIncomeTaxAtSource, incomeTaxAtSourceChecks: _*)(operationIndex)

  private def assertTotal(totalChecks: AttributeCheck*): AttributeValidation = attribute => operationIndex => 
    assertAttribute(attribute, "Total", _.isTotal, totalChecks: _*)(operationIndex)

  private def assertAttribute(attribute: Cell, attributeName: String, attributeGuard: Cell ⇒ Boolean, attributeChecks: AttributeCheck*)(operationIndex: Int)(using worksheetName: String): ErrorsOr[Cell] =
    given Semigroup[Cell] = (x, _) => x

    if attributeGuard(attribute) then
      attributeChecks
        .map(_ (attribute)(attributeName, operationIndex))
        .reduce(_ combine _)
    else attribute.validNec

  private def isPresent: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isNotEmpty then attribute.validNec
    else RequiredValueMissing(attributeMissing(attributeHeader, operationIndex)).invalidNec

  private def isAValidDate: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isEmpty || attribute.asLocalDate.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a date")
    ).invalidNec

  private def hasAValidFontColor: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isEmpty || attribute.fontColor.either(RED, BLUE) then attribute.validNec
    else UnexpectedContentColor(invalidAttributeColor(attribute.fontColor, attributeHeader, operationIndex)).invalidNec

  private def isNotNegative: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.asDouble.forall(_ >= 0.0) then attribute.validNec
    else UnexpectedContentValue(unexpectedNegativeAttribute(attributeHeader, attribute.value, operationIndex)).invalidNec

  private def isAValidInteger: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isEmpty || attribute.asInt.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("an integer number")
    ).invalidNec

  private def isAValidCurrency: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isEmpty || attribute.isCurrency then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a currency")
    ).invalidNec

  private def isAValidDouble: AttributeCheck = attribute => (attributeHeader, operationIndex) =>
    if attribute.isEmpty || attribute.asDouble.isDefined then attribute.validNec
    else UnexpectedContentType(
      unexpectedContentType(attribute.value, attributeHeader, operationIndex)("a double")
    ).invalidNec

  private def assertFontColorReflectsOperationType(attributeColorChecks: AttributeColorCheck*): OperationValidation = line ⇒
    line.toMostLikelyOperation
      .andThen{operation =>
        attributeColorChecks
          .map(_(operation)(line).map(Seq(_)))
          .reduce(_ combine _)
          .map(_=> line)
      }

  private def onTradingDate: AttributeColorCheck = operation => line => 
    onAttribute(line.cells.head)("TradingDate", operation, line.number)

  private def onNoteNumber: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(1))("NoteNumber", operation, line.number)

  private def onTicker: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(2))("Ticker", operation, line.number)

  private def onQty: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(3))("Qty", operation, line.number)

  private def onPrice: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(4))("Price", operation, line.number)

  private def onVolume: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(5))("Volume", operation, line.number)

  private def onSettlementFee: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(6))("SettlementFee", operation, line.number)

  private def onTradingFees: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(7))("TradingFees", operation, line.number)

  private def onBrokerage: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(8))("Brokerage", operation, line.number)

  private def onServiceTax: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(9))("ServiceTax", operation, line.number)

  private def onIncomeTaxAtSource: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(10))("IncomeTaxAtSource", operation, line.number)

  private def onTotal: AttributeColorCheck = operation => line => 
    onAttribute(line.cells(11))("Total", operation, line.number)

  private def onAttribute(attribute: Cell)(attributeHeader: String, operation: Operation, operationIndex: Int)(using worksheetName: String): ErrorsOr[Cell] =
    if attribute.isEmpty then attribute.validNec
    else
      val errorMessage: (String, String, String) => String = (currentOperation, attributeColor, operationDenoted) =>
        unexpectedAttributeColor(attributeColor, attributeHeader, operationIndex)(operationDenoted, currentOperation)
      operation match
        case _: SellingOperation =>
          if attribute.fontColor != RED then attribute.validNec
          else UnexpectedContentColor(errorMessage("Selling", s"red($RED)", "Buying")).invalidNec

        case _: BuyingOperation =>
          if attribute.fontColor != BLUE then attribute.validNec
          else UnexpectedContentColor(errorMessage("Buying", s"blue($BLUE)", "Selling")).invalidNec

  private def assertVolumeIsCalculatedCorrectly: OperationValidation = line ⇒
    val qtyCell = line.cells(3)
    val priceCell = line.cells(4)
    val volumeCell = line.cells(5)
    val qty = qtyCell.asInt.getOrElse(0)
    val price = priceCell.asDouble.getOrElse(0.0)

    val actualVolume = volumeCell.asDouble.getOrElse(0.0).withTwoDecimalPlaces
    val expectedVolume = (qty * price).withTwoDecimalPlaces

    if actualVolume != expectedVolume then UnexpectedContentValue(
      unexpectedVolume(actualVolume, line.number)(expectedVolume, qtyCell.value, price.withTwoDecimalPlaces)
    ).invalidNec
    else line.validNec

  // TODO Think about adding a reason to the errors for the cases where the requirements for the validation are not present. For instance, in the case below, any one of the following, if missing or having any other problem, would result in an impossible validation: tradingDate, volume and, actualSettlementFee. For now, we'll choose default values for them in case of problem and the validation will fail because of them. Hopefully, the original cause will be caught by other validation.
  private def assertSettlementFeeIsCalculatedCorrectly: OperationValidation = line ⇒
    given Semigroup[Line] = (x, _) => x
    val settlementFeeCell = line.cells(6)

    if settlementFeeCell.hasOrangeBackground then
      assertSettlementFeeCalculatedCorrectlyFor(DayTrade)(line)
    else assertSettlementFeeCalculatedCorrectlyFor(Normal)(line) findValid {
      assertSettlementFeeCalculatedCorrectlyFor(DayTrade)(line)
    }

  private def assertSettlementFeeCalculatedCorrectlyFor(operationalMode: OperationalMode)(line: Line)(using serviceDependencies: ServiceDependencies, worksheetName: String): ErrorsOr[Line] = 
    if line.settlementFeesCalculationSucceedsFor(operationalMode) then line.validNec
    else 
      val SettlementFeeRate = serviceDependencies(1)

      val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
      val volume = line.cells(5).asDouble.getOrElse(0.0)
      val actualSettlementFee = line.cells(6).asDouble.getOrElse(0.0)

      val settlementFeeRate = SettlementFeeRate.forOperationalMode(operationalMode).at(tradingDate).value
      val expectedSettlementFee = volume * settlementFeeRate
      
      UnexpectedContentValue(
        unexpectedSettlementFee(
          actualSettlementFee.withTwoDecimalPlaces, line.number
        )(
          expectedSettlementFee.withTwoDecimalPlaces, volume.withTwoDecimalPlaces, (settlementFeeRate * 100).percentageWithFourDecimalPlaces
        )
      ).invalidNec

  private def assertTradingFeesIsCalculatedCorrectly: OperationValidation = line ⇒
    given Semigroup[Line] = (x, _) => x
    val tradingFeesCell = line.cells(7)

    if tradingFeesCell.either(hasOrangeBackground, hasNote) then
      assertTradingFeesCalculatedCorrectlyFor(PRE_OPENING)(line)
    else assertTradingFeesCalculatedCorrectlyFor(TRADING)(line) findValid {
      assertTradingFeesCalculatedCorrectlyFor(PRE_OPENING)(line)
    }

  private def assertTradingFeesCalculatedCorrectlyFor(tradingPeriod: TradingPeriod)(line: Line)(using serviceDependencies: ServiceDependencies, worksheetName: String): ErrorsOr[Line] = 
    if line.tradingFeesCalculationSucceedsFor(tradingPeriod) then line.validNec
    else 
      val TradingFeesRate = serviceDependencies(2)

      val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
      val volume = line.cells(5).asDouble.getOrElse(0.0)
      val actualTradingFees = line.cells(7).asDouble.getOrElse(0.0)

      val tradingFeesRate = TradingFeesRate.at(tradingDate, tradingPeriod)
      val expectedTradingFees = volume * tradingFeesRate
      
      UnexpectedContentValue(
        unexpectedTradingFees(
          actualTradingFees.withTwoDecimalPlaces, line.number
        )(
          expectedTradingFees.withTwoDecimalPlaces, volume.withTwoDecimalPlaces, (tradingFeesRate * 100).percentageWithFourDecimalPlaces
        )
      ).invalidNec

  private def assertServiceTaxIsCalculatedCorrectly: OperationValidation = line ⇒
    given tolerance: Double = 0.01
    val serviceDependencies = summon[ServiceDependencies]
    val ServiceTaxRate = serviceDependencies(3)

    val brokerageCell = line.cells(8)
    val serviceTaxCell = line.cells(9)
    val tradingDate = line.cells.head.asLocalDate.getOrElse(LocalDate.MIN)
    val brokerage = brokerageCell.asDouble.getOrElse(0.0)
    val actualServiceTax = serviceTaxCell.asDouble.getOrElse(0.0)
    // TODO The city used to calculate the ServiceTax can be determined, in the future, by looking into the Broker information present in the brokerage note document.
    val serviceTaxRate = ServiceTaxRate.at(tradingDate).value
    val expectedServiceTax = brokerage * serviceTaxRate

    if actualServiceTax !~= expectedServiceTax then UnexpectedContentValue(
      unexpectedServiceTax(
        actualServiceTax.withTwoDecimalPlaces, line.number
      )(
        expectedServiceTax.withTwoDecimalPlaces, brokerage.withTwoDecimalPlaces, (serviceTaxRate * 100).percentageWithOneDecimalPlace
      )
    ).invalidNec
    else line.validNec

  private def assertIncomeTaxAtSourceIsCalculatedCorrectly: OperationValidation = line ⇒
    val serviceDependencies = summon[ServiceDependencies]
    val AverageStockPrice = serviceDependencies(0)
    val IncomeTaxAtSourceRate = serviceDependencies(4)

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
      val operationAverageCost = AverageStockPrice.forTicker(tickerCell.value) * qty
      // TODO When the ticker cannot be found in the portfolio, 0.0 is returned which should trigger an exception since I'm trying to sell something I do not posses. For now, I'll tweak TEST_SPREADSHEET so that all BuyingOperations refer to VALE5 and have the appropriate calculation for the IncomeTaxAtSource.
      val operationProfit = operationNetResult - operationAverageCost
      val incomeTaxAtSourceRate = IncomeTaxAtSourceRate.forOperationalMode(Normal).at(tradingDate).value
      val expectedIncomeTaxAtSource = operationProfit * incomeTaxAtSourceRate

      if actualIncomeTaxAtSource !~= expectedIncomeTaxAtSource then UnexpectedContentValue(
        unexpectedIncomeTaxAtSourceForSellings(
          actualIncomeTaxAtSource.withTwoDecimalPlaces, line.number
        )(
          expectedIncomeTaxAtSource.withTwoDecimalPlaces, operationProfit.withTwoDecimalPlaces, (incomeTaxAtSourceRate * 100).percentageWithFourDecimalPlaces
        )
      ).invalidNec
      else line.validNec
    else
      given Semigroup[Line] = (x, _) => x

      if incomeTaxAtSourceCell.isNotEmpty then {
        if !incomeTaxAtSourceCell.isCurrency then UnexpectedContentType(
          unexpectedIncomeTaxAtSourceForBuyings(incomeTaxAtSourceCell.value, line.number)
        ).invalidNec
        else line.validNec
      } combine {
        if actualIncomeTaxAtSource > 0.0 then UnexpectedContentValue(
          unexpectedIncomeTaxAtSourceForBuyings(incomeTaxAtSourceCell.value, line.number)
        ).invalidNec
        else line.validNec
      }
      else line.validNec

  private def assertTotalIsCalculatedCorrectly: OperationValidation = line ⇒
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
        unexpectedTotalForSellings(actualTotal.withTwoDecimalPlaces, line.number)(expectedTotal.withTwoDecimalPlaces)
      ).invalidNec
      else line.validNec
    else
      val expectedTotal = volume + settlementFee + tradingFees + brokerage + serviceTax

      if actualTotal !~= expectedTotal then UnexpectedContentValue(
        unexpectedTotalForBuyings(actualTotal.withTwoDecimalPlaces, line.number)(expectedTotal.withTwoDecimalPlaces)
      ).invalidNec
      else line.validNec

  private def assertOperationsInBrokerageNoteHaveSameTradingDate: OperationsHarmonyCheck = (first: Line, second: Line) =>
    val firstTradingDateCell = first.cells.head
    val secondTradingDateCell = second.cells.head

    if firstTradingDateCell.value != secondTradingDateCell.value then UnexpectedContentValue(
      conflictingTradingDate(
        secondTradingDateCell.address, second.cells.tail.head.value, secondTradingDateCell.value
      )(
        firstTradingDateCell.value, firstTradingDateCell.address
      )
    ).invalidNec
    else second.validNec

  private def assertOperationsInBrokerageNoteHaveSameNoteNumber: OperationsHarmonyCheck = (first: Line, second: Line) =>
    val firstNoteNumberCell = first.cells.tail.head
    val secondNoteNumberCell = second.cells.tail.head

    if firstNoteNumberCell.value != secondNoteNumberCell.value then UnexpectedContentValue(
      conflictingNoteNumber(
        secondNoteNumberCell.address, second.cells.tail.head.value, secondNoteNumberCell.value
      )(
        firstNoteNumberCell.value, firstNoteNumberCell.address
      )
    ).invalidNec
    else second.validNec

  private def assertVolumeSummary(volumeSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(5, "VolumeSummary", volumeSummaryChecks: _*)(group)

  private def assertSettlementFeeSummary(settlementFeeSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(6, "SettlementFeeSummary", settlementFeeSummaryChecks: _*)(group)

  private def assertTradingFeesSummary(tradingFeesSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(7, "TradingFeesSummary", tradingFeesSummaryChecks: _*)(group)

  private def assertBrokerageSummary(brokerageSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(8, "BrokerageSummary", brokerageSummaryChecks: _*)(group)

  private def assertServiceTaxSummary(serviceTaxSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(9, "ServiceTaxSummary", serviceTaxSummaryChecks: _*)(group)

  private def assertIncomeTaxAtSourceSummary(incomeTaxAtSourceSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(10, "IncomeTaxAtSourceSummary", incomeTaxAtSourceSummaryChecks: _*)(group)

  private def assertTotalSummary(totalSummaryChecks: SummaryAttributeCheck*): BrokerageNoteValidation = group ⇒ 
    assertSummaryAttribute(11, "TotalSummary", totalSummaryChecks: _*)(group)

  private def assertSummaryAttribute(attributeIndex: Int, attributeName: String, attributeSummaryChecks: SummaryAttributeCheck*)(group: Group)(using worksheetName: String): ErrorsOr[Group] =
    group.summary.map { summary ⇒
      given Semigroup[Cell] = (x, _) => x
      val summaryAttribute = summary.cells(attributeIndex)

      attributeSummaryChecks
        .map(_ (summaryAttribute)(attributeIndex, attributeName, summary.number, group))
        .reduce(_ combine _)
        .liftTo(group)(summary)
    }.getOrElse(group.validNec)

  @targetName("summaryAttributeIsPresent")
  private def isPresent: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _) =>
    isPresent(summaryAttribute)(attributeHeader, operationIndex)

  @targetName("summaryAttributeIsAValidCurrency")
  private def isAValidCurrency: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _) =>
    isAValidCurrency(summaryAttribute)(attributeHeader, operationIndex)

  @targetName("summaryAttributeIsAValidDouble")
  private def isAValidDouble: SummaryAttributeCheck = summaryAttribute => (_, attributeHeader, operationIndex, _) =>
    isAValidDouble(summaryAttribute)(attributeHeader, operationIndex)

  private def isCorrectlyCalculated: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, _, group) =>
    val expectedSummaryValue = group.nonSummaryLines.foldLeft(0.0)((acc, line) ⇒ acc + line.cells(attributeIndex).asDouble.getOrElse(0.0))
    val actualSummaryValue = summaryAttribute.asDouble.getOrElse(0.0)

    if actualSummaryValue !~= expectedSummaryValue then UnexpectedContentValue(
      unexpectedValueForCalculatedSummaryAttribute(
        actualSummaryValue.withTwoDecimalPlaces, attributeHeader, summaryAttribute.address
      )(
        expectedSummaryValue.withTwoDecimalPlaces, 
        s"the sum of all '${attributeHeader.replace("Summary", "")}'s in the 'Group' (${group.head.cells(attributeIndex).address}...${group.takeRight(2).head.cells(attributeIndex).address})"
      )
    ).invalidNec
    else summaryAttribute.validNec

  private def isOperationTypeAwareWhenCalculated: SummaryAttributeCheck = summaryAttribute => (attributeIndex, attributeHeader, operationIndex, group) =>
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
      unexpectedOperationTypeAwareAttributeSummary(
        actualSummaryValue.withTwoDecimalPlaces, attributeHeader, operationIndex
      )(
        expectedSummaryValue.withTwoDecimalPlaces, attributeLetter, formulaDescription
      )
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
    private def ||(thatAttributeCheck: AttributeCheck): AttributeCheck = cell ⇒ (s1, i) =>
      thisAttributeCheck(cell)(s1, i).findValid(thatAttributeCheck(cell)(s1, i))

  extension (thisSummaryAttributeCheck: SummaryAttributeCheck)
    private def or(thatSummaryAttributeCheck: SummaryAttributeCheck): SummaryAttributeCheck = cell ⇒ (i1, s1, i2, g) =>
      thisSummaryAttributeCheck(cell)(i1, s1, i2, g).findValid(thatSummaryAttributeCheck(cell)(i1, s1, i2, g))

  extension (group: Group)

    private def validatedWith(
      attributeValidations: Seq[AttributeValidation] = Seq(),
      attributesHarmonyChecks: Seq[AttributesHarmonyCheck] = Seq(),
      operationValidations: Seq[OperationValidation] = Seq(),
      operationsHarmonyChecks: Seq[OperationsHarmonyCheck] = Seq(),
      brokerageNoteValidations: Seq[BrokerageNoteValidation] = Seq()
    )(using serviceDependencies: ServiceDependencies, worksheetName: String): ErrorsOr[Group] =
      given Semigroup[Group] = (x, _) => x
      given Semigroup[Line] = (x, _) => x
      val appliedBrokerageNoteValidations: ErrorsOr[Group] = brokerageNoteValidations.map(_ (group)).reduce(_ combine _)
      val validateAndHarmonize: (Line => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.map(f).reduce(_ combine _)
      val applyHarmonyChecksToLineWindow: (Seq[Line] => ErrorsOr[Seq[Line]]) => ErrorsOr[Seq[Line]] = f => group.nonSummaryLines.sliding(2).map(f).reduce(_ combine _)
      val applyOperationsHarmonyChecksTo: (Line, Line) => ErrorsOr[Line] = (line1, line2) => operationsHarmonyChecks.map(_ (line1, line2)).reduce(_ combine _)

      appliedBrokerageNoteValidations.combine{
        validateAndHarmonize{ line =>
          line.validatedWith(operationValidations).accumulate.combine{
            line.harmonizedWith(attributesHarmonyChecks).accumulate.combine{
              line.withCellsValidated(attributeValidations).accumulate
            }
          }
        }.liftTo(group).combine{
          applyHarmonyChecksToLineWindow {
            case Seq(line1, line2) ⇒ applyOperationsHarmonyChecksTo(line1, line2).accumulate
            case Seq(line) ⇒ line.validNec.accumulate
          }
        }
      }

    private def toBrokerageNote(using serviceDependencies: ServiceDependencies, worksheetName: String): ErrorsOr[BrokerageNote] =
      nonSummaryLines
        .map(_.toOperation.map(Seq(_)))
        .reduce(_ combine _)
        .map(BrokerageNote(_, group.head.toFinancialSummary))

    private def nonSummaryLines: Seq[Line] = summary.map(_ => group.init).getOrElse(group)

    private def summary: Option[Line] = if group.size <= 1 then None else group.lastOption

    private def `type`(using worksheetName: String): GroupType = 
      nonSummaryLines.distinctBy(_.toMostLikelyOperation.map(_.getClass)).size match
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

    private def toMostLikelyOperation(using worksheetName: String): ErrorsOr[Operation] =
      val volume = cells(5).value
      val settlementFee = cells(6).value
      val tradingFees = cells(7).value
      val brokerage = cells(8).value
      val serviceTax = cells(9).value
      val incomeTaxAtSource = cells(10).value
      val total = cells(11).value

      if hasMostNonEmptyValidColoredCellsBlue then
        SellingOperation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total).validNec

      else if hasMostNonEmptyValidColoredCellsRed then
        BuyingOperation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total).validNec
      
      else UnexpectedContentColor(impossibleToDetermineMostLikelyOperationType(line.number)).invalidNec

    private def toOperation(using serviceDependencies: ServiceDependencies, worksheetName: String): ErrorsOr[Operation] =
      val volume = cells(5).value
      val settlementFee = cells(6).value
      val tradingFees = cells(7).value
      val brokerage = cells(8).value
      val serviceTax = cells(9).value
      val incomeTaxAtSource = cells(10).value
      val total = cells(11).value

      if allNonEmptyCellsAreBlue then
        SellingOperation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total, tradingPeriod, operationalMode).validNec

      else if allNonEmptyCellsAreRed then
        BuyingOperation(volume, settlementFee, tradingFees, brokerage, serviceTax, incomeTaxAtSource, total, tradingPeriod).validNec

      else UnexpectedContentColor(impossibleToDetermineOperationType(line.number)).invalidNec

    private def tradingPeriod: ServiceDependencies ?=> TradingPeriod =
      val tradingFeesCell = cells(7)

      if tradingFeesCell.hasOrangeBackground || tradingFeesCell.hasNote ||
        (tradingFeesCalculationFailsFor(TRADING) && tradingFeesCalculationSucceedsFor(PRE_OPENING)) then PRE_OPENING
      else TRADING

    private def tradingFeesCalculationFailsFor: TradingPeriod => ServiceDependencies ?=> Boolean = tradingPeriod =>
      !tradingFeesCalculationSucceedsFor(tradingPeriod)

    private def tradingFeesCalculationSucceedsFor(tradingPeriod: TradingPeriod)(using serviceDependencies: ServiceDependencies): Boolean =
      given tolerance: Double = 0.01
      val TradingFeesRate = serviceDependencies(2)

      val tradingDate = cells.head.asLocalDate.getOrElse(LocalDate.MIN)
      val volume = cells(5).asDouble.getOrElse(0.0)
      val actualTradingFees = cells(7).asDouble.getOrElse(0.0)

      val tradingFeesRate = TradingFeesRate.at(tradingDate, tradingPeriod)
      val expectedTradingFees = volume * tradingFeesRate

      actualTradingFees ~= expectedTradingFees

    private def operationalMode: ServiceDependencies ?=> OperationalMode =
      val settlementFeeCell = cells(6)

      if settlementFeeCell.hasOrangeBackground ||
        (settlementFeeCalculationFailsFor(Normal) && settlementFeesCalculationSucceedsFor(DayTrade)) then DayTrade
      else Normal

    private def settlementFeeCalculationFailsFor: OperationalMode => ServiceDependencies ?=> Boolean = operationalMode =>
      !settlementFeesCalculationSucceedsFor(operationalMode)

    private def settlementFeesCalculationSucceedsFor(operationalMode: OperationalMode)(using serviceDependencies: ServiceDependencies): Boolean =
      given tolerance: Double = 0.01
      val SettlementFeeRate = serviceDependencies(1)

      val tradingDate = cells.head.asLocalDate.getOrElse(LocalDate.MIN)
      val volume = cells(5).asDouble.getOrElse(0.0)
      val actualSettlementFee = cells(6).asDouble.getOrElse(0.0)

      val settlementFeeRate = SettlementFeeRate.forOperationalMode(operationalMode).at(tradingDate).value
      val expectedSettlementFee = volume * settlementFeeRate

      actualSettlementFee ~= expectedSettlementFee

    private def toFinancialSummary: FinancialSummary =
      FinancialSummary(cells(5).value, cells(6).value, cells(7).value, cells(8).value, cells(9).value, cells(10).value, cells(11).value)

    private def validatedWith: Seq[OperationValidation] => ServiceDependencies ?=> String ?=> ErrorsOr[Line] = operationValidations =>
      given Semigroup[Line] = (x, _) => x
      operationValidations.map(_ (line)).reduce(_ combine _)

    private def withCellsValidated: Seq[AttributeValidation] => String ?=> ErrorsOr[Line] = attributeValidations =>
      cells.map(_.validatedWith(attributeValidations)(line.number).accumulate).reduce(_ combine _).liftTo(line)

    private def harmonizedWith: Seq[AttributesHarmonyCheck] => String ?=> ErrorsOr[Line] = attributesHarmonyChecks =>
      given Semigroup[Cell] = (x, _) => x
      val applyAttributesHarmonyChecksTo: (Cell, Cell) => ErrorsOr[Cell] = (cell1, cell2) => attributesHarmonyChecks.map(_ (cell1, cell2)).reduce(_ combine _)
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

    private def validatedWith(attributeValidations: Seq[AttributeValidation])(operationIndex: Int)(using worksheetName: String): ErrorsOr[Cell] =
      given Semigroup[Cell] = (x, _) => x
      attributeValidations.map(_ (cell)(operationIndex)).reduce(_ combine _)

    private def index: Int = cell.address.charAt(0) - UPPERCASE_A_ASCII

    private def hasOrangeBackground: Boolean = cell.backgroundColor == ORANGE

    private def hasNote: Boolean = cell.note.nonEmpty

    private def either(left: Cell => Boolean, right: Cell => Boolean): Boolean =
      left(cell) || right(cell)

  extension (thisDouble: Double)

    @targetName("approximatelly")
    private def ~=(otherDouble: Double)(using precision: Double): Boolean = 
      (thisDouble - otherDouble).abs.roundAt(2) <= precision

    @targetName("differentBeyondPrecision")
    private def !~=(otherDouble: Double)(using precision: Double): Boolean = !(~=(otherDouble))

    private def roundAt(p: Int): Double =
      val s = math pow (10, p)
      (math round thisDouble * s) / s

    private def withTwoDecimalPlaces: String = formatted("%.2f")

    private def percentageWithOneDecimalPlace: String = formatted("%.1f%%")

    private def percentageWithFourDecimalPlaces: String = formatted("%.4f%%")

    /*
     This is necessary to compensate for a shortcoming of Java formatting that will look only to the first number
     to be discarded when deciding about rounding, resulting in, for example, a number like 3.1149, when formatted
     with 2 decimal places, showing up as 3.11 instead of the expected 3.12.
     TODO We should use the number of decimal places in the format string to generate the 'compensator'.
    */
    private def formatted(format: String): String =
      if format == "%.2f" then
        String.format(Locale.US, format, thisDouble + 0.0005)
      else
        String.format(Locale.US, format, thisDouble)

  extension (string: String)

    private def either(left: String, right: String): Boolean = string == left || string == right

object BrokerageNotesWorksheetMessages:
  private val RED = "255,0,0"
  private val BLUE = "91,155,213"

  def attributeMissing(attributeName: String, operationIndex: Int)(using worksheetName: String): String =
    s"A required attribute ('$attributeName') is missing on line '$operationIndex' of 'Worksheet' '$worksheetName'."

  def summaryLineMissing(noteNumber: String)(using worksheetName: String): String =
    s"An invalid 'Group' ('$noteNumber') was found on 'Worksheet' '$worksheetName'. 'MultilineGroup's must have a 'SummaryLine'."

  def unexpectedContentType(attributeValue: String, attributeName: String, operationIndex: Int)(expectedDataType: String)(using worksheetName: String) =
    s"'$attributeName' ('$attributeValue') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be interpreted as $expectedDataType."

  def invalidAttributeColor(attributeColor: String, attributeName: String, operationIndex: Int)(using worksheetName: String) =
    s"'$attributeName's font-color ('$attributeColor') on line '$operationIndex' of 'Worksheet' '$worksheetName' can only be red ('$RED') or blue ('$BLUE')."

  def unexpectedAttributeColor(attributeColor: String, attributeName: String, operationIndex: Int)(operationDenoted: String, currentOperation: String)(using worksheetName: String): String =
    s"The 'Operation' on line '$operationIndex' of 'Worksheet' '$worksheetName' looks like '$currentOperation' but, '$attributeName' has font-color '$attributeColor' which denotes '$operationDenoted'."

  private def impossibleToDetermineOperationType(operationIndex: Int, reason: String)(using worksheetName: String): String =
    s"Impossible to determine the type of 'Operation' on line '$operationIndex' of 'Worksheet' '$worksheetName' due to $reason."

  private val halfOfTheAttributesFromEachValidColor = "exactly half of the non-empty valid-colored 'Attribute's of each valid color"
  def impossibleToDetermineMostLikelyOperationType(operationIndex: Int)(using worksheetName: String): String =
    impossibleToDetermineOperationType(operationIndex, halfOfTheAttributesFromEachValidColor)
      
  private val attributesWithDivergentFontColors = "its 'Attribute's having divergent font colors"
  def impossibleToDetermineOperationType(operationIndex: Int)(using worksheetName: String): String =
    impossibleToDetermineOperationType(operationIndex, attributesWithDivergentFontColors)

  private def conflictingAttribute(attributeName: String, attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(using worksheetName: String) = 
    s"An invalid 'Group' ('$noteNumber') was found on 'Worksheet' '$worksheetName'. '$attributeName's should be the same for all 'Line's in a 'Group' in order to being able to turn it into a 'BrokerageNote' but, '$actualValue' in '$attributeAddress' is different from '$expectedValue' in '$targetAttributeAddress'."

  def conflictingTradingDate(attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(using worksheetName: String): String = 
    conflictingAttribute("TradingDate", attributeAddress, noteNumber, actualValue)(expectedValue, targetAttributeAddress)

  def conflictingNoteNumber(attributeAddress: String, noteNumber: String, actualValue: String)(expectedValue: String, targetAttributeAddress: String)(using worksheetName: String): String = 
    conflictingAttribute("NoteNumber", attributeAddress, noteNumber, actualValue)(expectedValue, targetAttributeAddress)

  def unexpectedNegativeAttribute(attributeName: String, attributeValue: String, operationIndex: Int)(using worksheetName: String): String =
    s"'$attributeName' ('$attributeValue') on line '$operationIndex' of 'Worksheet' '$worksheetName' cannot be negative."

  private def unexpectedAttributeValue(actualValue: String, attributeName: String, attributeAddress: String, attributeType: String)(expectation: String)(using worksheetName: String): String =
    s"An invalid calculated '$attributeType' ('$attributeAddress:$attributeName') was found on 'Worksheet' '$worksheetName'. It was supposed to $expectation but, it actually contained '$actualValue'."

  private val incomeTaxAtSourceForBuyingsExpectation = "be either empty or equal to '0.00'"
  def unexpectedIncomeTaxAtSourceForBuyings(actualValue: String, operationIndex: Int)(using worksheetName: String): String =
    unexpectedAttributeValue(
      actualValue, "IncomeTaxAtSource", s"K$operationIndex", "Cell"
    )(
      incomeTaxAtSourceForBuyingsExpectation
    )

  private def calculatedAttributeExpectation(expectedValue: String, formulaDescription: String) = s"contain '$expectedValue', which is equal to '$formulaDescription'"
  private def unexpectedValueForCalculatedAttribute(actualValue: String, attributeName: String, attributeAddress: String)(expectedValue: String, formulaDescription: String)(using worksheetName: String) =
    unexpectedAttributeValue(
      actualValue, attributeName, attributeAddress, "Cell"
    )(
      calculatedAttributeExpectation(expectedValue, formulaDescription)
    )

  private def volumeFormulaDescription(qty: String, price: String)(operationIndex: Int) = s"D$operationIndex:Qty * E$operationIndex:Price ($qty * $price)"
  def unexpectedVolume(actualValue: String, operationIndex: Int)(expectedValue: String, qty: String, price: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "Volume", s"F$operationIndex"
    )(
      expectedValue, volumeFormulaDescription(qty, price)(operationIndex)
    )

  private def settlementFeeFormulaDescription(volume: String, settlementFeeRate: String)(operationIndex: Int) =
    s"F$operationIndex:Volume * 'SettlementFeeRate' for the 'OperationalMode' at 'TradingDate' ($volume * $settlementFeeRate)"
  def unexpectedSettlementFee(actualValue: String, operationIndex: Int)(expectedValue: String, volume: String, settlementFeeRate: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "SettlementFee", s"G$operationIndex"
    )(
      expectedValue, settlementFeeFormulaDescription(volume, settlementFeeRate)(operationIndex)
    )

  private def tradingFeesFormulaDescription(volume: String, tradingFeesRate: String)(operationIndex: Int): String =
    s"F$operationIndex:Volume * 'TradingFeesRate' at 'TradingDateTime' ($volume * $tradingFeesRate)"
  def unexpectedTradingFees(actualValue: String, operationIndex: Int)(expectedValue: String, volume: String, tradingFeesRate: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "TradingFees", s"H$operationIndex"
    )(
      expectedValue, tradingFeesFormulaDescription(volume, tradingFeesRate)(operationIndex)
    )

  private def serviceTaxFormulaDescription(brokerage: String, serviceTaxRate: String)(operationIndex: Int): String =
    s"I$operationIndex:Brokerage * 'ServiceTaxRate' at 'TradingDate' in 'BrokerCity' ($brokerage * $serviceTaxRate)"
  def unexpectedServiceTax(actualValue: String, operationIndex: Int)(expectedValue: String, brokerage: String, serviceTaxRate: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "ServiceTax", s"J$operationIndex"
    )(
      expectedValue, serviceTaxFormulaDescription(brokerage, serviceTaxRate)(operationIndex)
    )

  private def incomeTaxAtSourceRateFormulaDescription(acquisitionCost: String, incomeTaxAtSourceRate: String)(operationIndex: Int): String =
    s"(('F$operationIndex:Volume' - 'G$operationIndex:SettlementFee' - 'H$operationIndex:TradingFees' - 'I$operationIndex:Brokerage' - 'J$operationIndex:ServiceTax') - ('AverageStockPrice' for the 'C$operationIndex:Ticker' * 'D$operationIndex:Qty')) * 'IncomeTaxAtSourceRate' for the 'OperationalMode' at 'TradingDate' ($acquisitionCost * $incomeTaxAtSourceRate)"
  def unexpectedIncomeTaxAtSourceForSellings(actualValue: String, operationIndex: Int)(expectedValue: String, acquisitionCost: String, incomeTaxAtSourceRate: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedAttribute(
      actualValue, "IncomeTaxAtSource", s"K$operationIndex"
    )(
      expectedValue, incomeTaxAtSourceRateFormulaDescription(acquisitionCost, incomeTaxAtSourceRate)(operationIndex)
    )

  private def unexpectedTotal(actualValue: String, operationIndex: Int)(expectedValue: String, formulaDescription: String)(using worksheetName: String) =
    unexpectedValueForCalculatedAttribute(
      actualValue, "Total", s"L$operationIndex"
    )(
      expectedValue, formulaDescription
    )

  private def buyingTotalFormulaDescription(operationIndex: Int) =
    s"'F$operationIndex:Volume' + 'G$operationIndex:SettlementFee' + 'H$operationIndex:TradingFees' + 'I$operationIndex:Brokerage' + 'J$operationIndex:ServiceTax'"
  def unexpectedTotalForBuyings(actualValue: String, operationIndex: Int)(expectedValue: String)(using worksheetName: String): String =
    unexpectedTotal(
      actualValue, operationIndex
    )(
      expectedValue, buyingTotalFormulaDescription(operationIndex)
    )

  private def sellingTotalFormulaDescription(operationIndex: Int) =
    s"'F$operationIndex:Volume' - 'G$operationIndex:SettlementFee' - 'H$operationIndex:TradingFees' - 'I$operationIndex:Brokerage' - 'J$operationIndex:ServiceTax'"
  def unexpectedTotalForSellings(actualValue: String, operationIndex: Int)(expectedValue: String)(using worksheetName: String): String =
    unexpectedTotal(
      actualValue, operationIndex
    )(
      expectedValue, sellingTotalFormulaDescription(operationIndex)
    )

  private def calculatedSummaryAttributeExpectation(expectedValue: String, formulaDescription: String) =
    s"contain '$expectedValue', which is $formulaDescription"
  def unexpectedValueForCalculatedSummaryAttribute(actualValue: String, attributeName: String, summaryAddress: String)(expectedValue: String, formulaDescription: String)(using worksheetName: String): String =
    unexpectedAttributeValue(
      actualValue, attributeName, summaryAddress, "SummaryCell"
    )(
      calculatedSummaryAttributeExpectation(expectedValue, formulaDescription)
    )

  def operationTypeAwareAttributeSummaryFormulaDescriptionForHomogeneousGroups(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all 'Operation's '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"

  def operationTypeAwareAttributeSummaryFormulaDescriptionForHeterogeneousGroups(attributeName: String, attributeLetter: Char, indexOfFirstOperation: Int, indexOfLastOperation: Int) =
    s"the sum of all 'SellingOperation's '$attributeName's minus the sum of all 'BuyingOperation's '$attributeName's in the 'Group' ($attributeLetter$indexOfFirstOperation...$attributeLetter$indexOfLastOperation)"
  def unexpectedOperationTypeAwareAttributeSummary(actualValue: String, attributeName: String, summaryIndex: Int)(expectedValue: String, attributeLetter: Char, formulaDescription: String)(using worksheetName: String): String =
    unexpectedValueForCalculatedSummaryAttribute(
      actualValue, attributeName, s"$attributeLetter$summaryIndex"
    )(
      expectedValue, formulaDescription
    )