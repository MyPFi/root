package com.andreidiego.mpfi.stocks.adapter.services.servicetaxrate

import java.time.LocalDate
import com.andreidiego.mpfi.stocks.adapter.services.cities.City

// TODO This will become a separate service soon
trait ServiceTaxRateService:
  def at(tradingDate: LocalDate): ServiceTaxRateService

  def in(city: City): ServiceTaxRateService

  def value: Double