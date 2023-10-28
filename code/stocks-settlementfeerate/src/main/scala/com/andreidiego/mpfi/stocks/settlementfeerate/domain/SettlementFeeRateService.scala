package com.andreidiego.mpfi.stocks.settlementfeerate.domain

import java.time.LocalDate
import com.andreidiego.mpfi.stocks.common.OperationalMode

// TODO This will become a separate service soon
trait SettlementFeeRateService:
  def forOperationalMode(
    operationalMode: OperationalMode
  ): SettlementFeeRateService

  def at(tradingDate: LocalDate): SettlementFeeRateService

  def value: Double