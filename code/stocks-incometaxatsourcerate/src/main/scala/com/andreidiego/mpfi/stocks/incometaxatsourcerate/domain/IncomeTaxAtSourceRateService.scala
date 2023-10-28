package com.andreidiego.mpfi.stocks.incometaxatsourcerate.domain

import java.time.LocalDate
import com.andreidiego.mpfi.stocks.common.OperationalMode

// TODO This will become a separate service soon
trait IncomeTaxAtSourceRateService:
  def forOperationalMode(
    operationalMode: OperationalMode
  ): IncomeTaxAtSourceRateService

  def at(tradingDate: LocalDate): IncomeTaxAtSourceRateService

  def value: Double