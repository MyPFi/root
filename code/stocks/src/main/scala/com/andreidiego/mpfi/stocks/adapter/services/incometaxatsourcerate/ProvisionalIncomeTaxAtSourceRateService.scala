package com.andreidiego.mpfi.stocks.adapter.services.incometaxatsourcerate

import java.time.LocalDate
import scala.collection.SortedMap
import com.andreidiego.mpfi.stocks.adapter.services.operationalmodes.OperationalMode
import com.andreidiego.mpfi.stocks.adapter.services.operationalmodes.OperationalMode.{DayTrade, Normal}

class ProvisionalIncomeTaxAtSourceRateService private(val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]]) extends IncomeTaxAtSourceRateService:

  import ProvisionalIncomeTaxAtSourceRateService.*

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRateService =
    ProvisionalIncomeTaxAtSourceRateService(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[OperationalMode, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == operationalMode))
    )

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService =
    ProvisionalIncomeTaxAtSourceRateService(
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

object ProvisionalIncomeTaxAtSourceRateService extends IncomeTaxAtSourceRateService:
  import java.time.format.DateTimeFormatter
  
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[OperationalMode, Double]] = SortedMap(
    LocalDate.MIN -> Map(Normal -> 0.00005, DayTrade -> 0.01),
    LocalDate.parse("01/02/2017", dateFormatter) -> Map(Normal -> 0.00005, DayTrade -> 0.01)
  )

  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRateService = ProvisionalIncomeTaxAtSourceRateService(ratesHistory).forOperationalMode(operationalMode)

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService = ProvisionalIncomeTaxAtSourceRateService(ratesHistory).at(tradingDate)

  def value: Double = 0.0

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)