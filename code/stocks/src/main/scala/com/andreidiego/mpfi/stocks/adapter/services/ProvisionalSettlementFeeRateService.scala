package com.andreidiego.mpfi.stocks.adapter.services

import java.time.LocalDate
import scala.collection.SortedMap

import OperationalMode.*

class ProvisionalSettlementFeeRateService private(val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]) extends SettlementFeeRateService:

  import ProvisionalSettlementFeeRateService.*

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRateService =
    ProvisionalSettlementFeeRateService(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): SettlementFeeRateService =
    ProvisionalSettlementFeeRateService(
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

object ProvisionalSettlementFeeRateService extends SettlementFeeRateService:
  import java.time.format.DateTimeFormatter
  
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.000079, DayTrade -> 0.000063),
    LocalDate.parse("04/05/2009", dateFormatter) -> Map(Normal -> 0.000060, DayTrade -> 0.000275),
    LocalDate.parse("26/11/2013", dateFormatter) -> Map(Normal -> 0.000275, DayTrade -> 0.000200),
    LocalDate.parse("02/02/2021", dateFormatter) -> Map(Normal -> 0.000250, DayTrade -> 0.000180)
  )

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRateService = ProvisionalSettlementFeeRateService(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): SettlementFeeRateService = ProvisionalSettlementFeeRateService(ratesHistory).at(tradingDate)

  def value: Double = 0.0

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)