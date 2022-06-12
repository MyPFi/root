package com.andreidiego.mpfi.stocks.adapter.services

import java.time.LocalDate
import OperationalMode.*

// TODO This will become a separate service soon
trait IncomeTaxAtSourceRateService:
  def forOperationalMode(operationalMode: OperationalMode): IncomeTaxAtSourceRateService

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService

  def value: Double