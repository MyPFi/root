package com.andreidiego.mpfi.stocks.tradingfeesrate.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap
import TradingPeriod.{CLOSING_CALL, PRE_OPENING, TRADING}

object ProvisionalTradingFeesRateService extends TradingFeesRateService:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, SortedMap[TradingPeriod, Double]] = SortedMap(
    LocalDate.MIN -> SortedMap(
      PRE_OPENING   -> 0.000205,
      TRADING       -> 0.000270,
      CLOSING_CALL  -> 0.000205
    ),
    LocalDate.parse("04/05/2009", dateFormatter) -> SortedMap(
      PRE_OPENING   -> 0.000070,
      TRADING       -> 0.000285,
      CLOSING_CALL  -> 0.000070
    ),
    LocalDate.parse("26/11/2013", dateFormatter) -> SortedMap(
      PRE_OPENING   -> 0.000070,
      TRADING       -> 0.000050,
      CLOSING_CALL  -> 0.000070
    ),
    LocalDate.parse("15/03/2020", dateFormatter) -> SortedMap(
      PRE_OPENING   -> 0.000050,
      TRADING       -> 0.000032,
      CLOSING_CALL  -> 0.000050
    ),
    LocalDate.parse("12/03/2021", dateFormatter) -> SortedMap(
      PRE_OPENING   -> 0.000050,
      TRADING       -> 0.000050,
      CLOSING_CALL  -> 0.000050
    )
  )

  def at(tradingDate: LocalDate, tradingPeriod: TradingPeriod): Double =
    ratesHistory
      .filter(_._1.isNotAfter(tradingDate))
      .last
      ._2
      .filter(_._1 == tradingPeriod)
      .last
      ._2

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean =
      date.isBefore(other) || date.equals(other)