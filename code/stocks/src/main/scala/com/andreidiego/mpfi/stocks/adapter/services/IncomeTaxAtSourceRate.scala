package com.andreidiego.mpfi.stocks.adapter.services

import OperationalMode.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap

// TODO This will become a separate service soon
class IncomeTaxAtSourceRate private(val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]):

  import IncomeTaxAtSourceRate.*

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRate =
    IncomeTaxAtSourceRate(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRate =
    IncomeTaxAtSourceRate(
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

object IncomeTaxAtSourceRate:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.00005, DayTrade -> 0.01),
    LocalDate.parse("01/02/2017", dateFormatter) -> Map(Normal -> 0.00005, DayTrade -> 0.01)
  )

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRate = IncomeTaxAtSourceRate(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRate = IncomeTaxAtSourceRate(ratesHistory).at(tradingDate)

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)