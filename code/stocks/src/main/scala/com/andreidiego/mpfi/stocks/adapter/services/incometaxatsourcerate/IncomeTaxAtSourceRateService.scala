package com.andreidiego.mpfi.stocks.adapter.services.incometaxatsourcerate

import com.andreidiego.mpfi.stocks.adapter.services.operationalmodes.OperationalMode

import java.time.LocalDate

// TODO This will become a separate service soon
trait IncomeTaxAtSourceRateService:
  def forOperationalMode(
    operationalMode: OperationalMode
  ): IncomeTaxAtSourceRateService

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService

  def value: Double