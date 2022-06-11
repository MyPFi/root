package com.andreidiego.mpfi.stocks.adapter.services

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap

// TODO This will become a separate service soon
object TradingFeesRate:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val PRE_OPENING: LocalTime = LocalTime.parse("09:45")
  val TRADING: LocalTime = LocalTime.parse("10:00")
  val CLOSING_CALL: LocalTime = LocalTime.parse("16:55")

  private val ratesHistory: SortedMap[LocalDate, SortedMap[LocalTime, Double]] = SortedMap(
    LocalDate.MIN -> SortedMap(PRE_OPENING -> 0.000205, TRADING -> 0.00027, CLOSING_CALL -> 0.000205),
    LocalDate.parse("04/05/2009", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.000285, CLOSING_CALL -> 0.00007),
    LocalDate.parse("26/11/2013", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.00005, CLOSING_CALL -> 0.00007),
    LocalDate.parse("15/03/2020", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00005, TRADING -> 0.000032, CLOSING_CALL -> 0.00005),
    LocalDate.parse("12/05/2021", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00005, TRADING -> 0.00005, CLOSING_CALL -> 0.00005)
  )

  def at(tradingDateTime: LocalDateTime): Double = ratesHistory
    .filter(_._1.isNotAfter(tradingDateTime.toLocalDate))
    .last
    ._2
    .filter(_._1.isNotAfter(tradingDateTime.toLocalTime))
    .last
    ._2

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)

  extension (time: LocalTime)
    private def isNotAfter(other: LocalTime): Boolean = time.isBefore(other) || time.equals(other)