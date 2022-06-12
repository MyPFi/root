package com.andreidiego.mpfi.stocks.adapter.services

import java.time.LocalDate

// TODO This will become a separate service soon
trait TradingFeesRateService:
  def at(tradingDate: LocalDate, tradingPeriod: TradingPeriod): Double