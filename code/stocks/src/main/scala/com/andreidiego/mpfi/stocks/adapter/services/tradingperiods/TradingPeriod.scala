package com.andreidiego.mpfi.stocks.adapter.services.tradingperiods

import java.time.LocalTime

enum TradingPeriod(val begin: LocalTime, val end: LocalTime):
  case ORDER_CANCELLING extends TradingPeriod(LocalTime.parse("09:30:00"), LocalTime.parse("09:44:59"))
  case PRE_OPENING extends TradingPeriod(LocalTime.parse("09:45:00"), LocalTime.parse("09:59:59"))
  case TRADING extends TradingPeriod(LocalTime.parse("10:00:00"), LocalTime.parse("16:54:59"))
  case CLOSING_CALL extends TradingPeriod(LocalTime.parse("16:55:00"), LocalTime.parse("17:00:59"))
  case AFTER_MARKET_PRE_OPENING extends TradingPeriod(LocalTime.parse("17:25:00"), LocalTime.parse("17:29:59"))
  case AFTER_MARKET_TRADING extends TradingPeriod(LocalTime.parse("17:30:00"), LocalTime.parse("17:59:59"))
  case AFTER_MARKET_ORDER_CANCELLING extends TradingPeriod(LocalTime.parse("18:30:00"), LocalTime.parse("18:44:59"))

  import TradingPeriod.*
  
  def encompasses(time: LocalTime) = time.isNotBefore(begin) && time.isNotAfter(end)

object TradingPeriod:
  given Ordering[TradingPeriod] = (x, y) => x.ordinal.compareTo(y.ordinal)

  def of(time: LocalTime) = values.find(_.encompasses(time))


  extension (thisTime: LocalTime)
    private def between(otherTime: LocalTime): Boolean = thisTime.isBefore(otherTime) || thisTime.equals(otherTime)

    private def isNotBefore(otherTime: LocalTime): Boolean = thisTime.isAfter(otherTime) || thisTime.equals(otherTime)

    private def isNotAfter(otherTime: LocalTime): Boolean = thisTime.isBefore(otherTime) || thisTime.equals(otherTime)