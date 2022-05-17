package com.andreidiego.mpfi.stocks.adapter.services

import OperationalMode.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap

// TODO This will become a separate service soon
class SettlementFeeRate private(val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]):

  import SettlementFeeRate.*

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRate =
    SettlementFeeRate(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): SettlementFeeRate =
    SettlementFeeRate(
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

object SettlementFeeRate:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.000275, DayTrade -> 0.0002),
    LocalDate.parse("30/12/2009", dateFormatter) -> Map(Normal -> 0.000275, DayTrade -> 0.0002),
    LocalDate.parse("02/02/2021", dateFormatter) -> Map(Normal -> 0.00025, DayTrade -> 0.00018)
  )

  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRate = SettlementFeeRate(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): SettlementFeeRate = SettlementFeeRate(ratesHistory).at(tradingDate)

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)