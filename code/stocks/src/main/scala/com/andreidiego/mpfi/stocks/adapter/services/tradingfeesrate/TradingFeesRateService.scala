package com.andreidiego.mpfi.stocks.adapter.services.tradingfeesrate

import java.time.LocalDate
import com.andreidiego.mpfi.stocks.adapter.services.tradingperiods.TradingPeriod

// TODO This will become a separate service soon
trait TradingFeesRateService:
  def at(tradingDate: LocalDate, tradingPeriod: TradingPeriod): Double