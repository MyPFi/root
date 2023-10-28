package com.andreidiego.mpfi.stocks.tradingfeesrate.domain

import java.time.LocalDate

// TODO This will become a separate service soon
trait TradingFeesRateService:
  def at(tradingDate: LocalDate, tradingPeriod: TradingPeriod): Double