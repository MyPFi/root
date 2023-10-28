package com.andreidiego.mpfi.stocks.servicetaxrate.domain

import java.time.LocalDate

// TODO This will become a separate service soon
trait ServiceTaxRateService:
  def at(tradingDate: LocalDate): ServiceTaxRateService

  def in(city: City): ServiceTaxRateService

  def value: Double