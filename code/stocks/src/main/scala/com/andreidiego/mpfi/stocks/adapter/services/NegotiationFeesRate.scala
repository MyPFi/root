package com.andreidiego.mpfi.stocks.adapter.services

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap

// TODO This will become a separate service soon
object NegotiationFeesRate:
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val PRE_OPENING: LocalTime = LocalTime.parse("09:45")
  val TRADING: LocalTime = LocalTime.parse("10:00")
  val CLOSING_CALL: LocalTime = LocalTime.parse("16:55")

  private val ratesHistory: SortedMap[LocalDate, SortedMap[LocalTime, Double]] = SortedMap(
    LocalDate.parse("01/01/0001", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.00007, CLOSING_CALL -> 0.00007),
    LocalDate.parse("26/11/2013", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.00005, CLOSING_CALL -> 0.00007),
    LocalDate.parse("28/10/2019", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00004, TRADING -> 0.000032, CLOSING_CALL -> 0.00004),
    LocalDate.parse("02/02/2021", dateFormatter) -> SortedMap(PRE_OPENING -> 0.00007, TRADING -> 0.00005, CLOSING_CALL -> 0.00007),
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