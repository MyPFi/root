package com.andreidiego.mpfi.stocks.adapter.services.settlementfeerate

import java.time.LocalDate
import scala.collection.SortedMap
import com.andreidiego.mpfi.stocks.adapter.services.operationalmodes.OperationalMode
import com.andreidiego.mpfi.stocks.adapter.services.operationalmodes.OperationalMode.{Normal, DayTrade}

class DummySettlementFeeRateService private(
  val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]
) extends SettlementFeeRateService:

  import DummySettlementFeeRateService.isNotAfter

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRateService =
    DummySettlementFeeRateService(
      ratesHistory
        .map { (rateRecord: (LocalDate, Map[OperationalMode, Double])) =>
          rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode)
        }
    )

  def at(tradingDate: LocalDate): SettlementFeeRateService =
    DummySettlementFeeRateService(
      SortedMap(
        ratesHistory
          .filter(_._1.isNotAfter(tradingDate))
          .last
      )
    )

  def value: Double =
    val ratesByOperationalModes: Map[OperationalMode, Double] = ratesHistory.last._2

    if ratesByOperationalModes.size == 1 then ratesByOperationalModes.last._2
    else ratesByOperationalModes.getOrElse(Normal, 0.0)

object DummySettlementFeeRateService extends SettlementFeeRateService:
  import java.time.format.DateTimeFormatter
  
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.000079, DayTrade -> 0.000063),
    LocalDate.parse("04/05/2009", dateFormatter) -> Map(Normal -> 0.00006, DayTrade -> 0.000275),
    LocalDate.parse("30/12/2009", dateFormatter) -> Map(Normal -> 0.000275, DayTrade -> 0.0002),
    LocalDate.parse("02/02/2021", dateFormatter) -> Map(Normal -> 0.00025, DayTrade -> 0.00018)
  )

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRateService = 
    DummySettlementFeeRateService(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): SettlementFeeRateService = 
    DummySettlementFeeRateService(ratesHistory).at(tradingDate)

  def value: Double = 0.0

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = 
      date.isBefore(other) || date.equals(other)