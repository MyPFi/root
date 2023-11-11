package com.andreidiego.mpfi.stocks.incometaxatsourcerate.domain.textkit

import java.time.LocalDate
import scala.collection.SortedMap
import com.andreidiego.mpfi.stocks
import stocks.common.OperationalMode
import stocks.common.OperationalMode.{DayTrade, Normal}
import stocks.incometaxatsourcerate.domain.IncomeTaxAtSourceRateService

class DummyIncomeTaxAtSourceRateService private(
  val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]
) extends IncomeTaxAtSourceRateService:

  import DummyIncomeTaxAtSourceRateService.isNotAfter

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRateService =
    DummyIncomeTaxAtSourceRateService(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService =
    DummyIncomeTaxAtSourceRateService(
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

object DummyIncomeTaxAtSourceRateService extends IncomeTaxAtSourceRateService:
  import java.time.format.DateTimeFormatter

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.00005, DayTrade -> 0.01),
    LocalDate.parse("01/02/2017", dateFormatter) -> Map(Normal -> 0.00005, DayTrade -> 0.01)
  )

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRateService =
    DummyIncomeTaxAtSourceRateService(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService =
    DummyIncomeTaxAtSourceRateService(ratesHistory).at(tradingDate)

  def value: Double = 0.0

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean =
      date.isBefore(other) || date.equals(other)