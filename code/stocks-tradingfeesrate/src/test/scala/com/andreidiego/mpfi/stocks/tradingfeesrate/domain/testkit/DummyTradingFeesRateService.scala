package com.andreidiego.mpfi.stocks.tradingfeesrate.domain.testkit

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap
import com.andreidiego.mpfi.stocks.tradingfeesrate.domain
import domain.{TradingFeesRateService, TradingPeriod}
import domain.TradingPeriod.{PRE_OPENING, TRADING, CLOSING_CALL}

object DummyTradingFeesRateService extends TradingFeesRateService:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, SortedMap[TradingPeriod, Double]] = SortedMap(
    LocalDate.MIN -> SortedMap(PRE_OPENING -> 0.000205, TRADING -> 0.00027, CLOSING_CALL -> 0.000205),
    LocalDate.parse("04/05/2009", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.000285, CLOSING_CALL -> 0.00007),
    LocalDate.parse("26/11/2013", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.00005, CLOSING_CALL -> 0.00007),
    LocalDate.parse("15/03/2020", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00005, TRADING -> 0.000032, CLOSING_CALL -> 0.00005),
    LocalDate.parse("12/05/2021", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00005, TRADING -> 0.00005, CLOSING_CALL -> 0.00005)
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
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)