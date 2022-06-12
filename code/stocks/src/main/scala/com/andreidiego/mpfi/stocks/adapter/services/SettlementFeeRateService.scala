package com.andreidiego.mpfi.stocks.adapter.services

import java.time.LocalDate
import OperationalMode.*

// TODO This will become a separate service soon
trait SettlementFeeRateService:
  def forOperationalMode(operationalMode: OperationalMode): SettlementFeeRateService

  def at(tradingDate: LocalDate): SettlementFeeRateService

  def value: Double