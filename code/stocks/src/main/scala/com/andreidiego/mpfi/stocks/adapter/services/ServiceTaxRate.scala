package com.andreidiego.mpfi.stocks.adapter.services

import com.andreidiego.mpfi.stocks.adapter.services.ServiceTaxRate.City
import com.andreidiego.mpfi.stocks.adapter.services.ServiceTaxRate.City.{RioDeJaneiro, SaoPaulo}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.SortedMap

// TODO This will become a separate service soon
class ServiceTaxRate private(val ratesHistory: SortedMap[LocalDate, Map[City, Double]]):

  import ServiceTaxRate.*

  def at(tradingDate: LocalDate): ServiceTaxRate =
    ServiceTaxRate(
      SortedMap(
        ratesHistory
          .filter(_._1.isNotAfter(tradingDate))
          .last
      )
    )

  def in(city: City): ServiceTaxRate =
    ServiceTaxRate(
      ratesHistory
        .map((rateRecord: (LocalDate, Map[City, Double])) ⇒ rateRecord._1 → rateRecord._2.filter(_._1 == city))
    )

  def value: Double =
    val ratesByCity: Map[City, Double] = ratesHistory.last._2

    if ratesByCity.size == 1 then ratesByCity.last._2
    else ratesByCity.getOrElse(RioDeJaneiro, 0.0)

object ServiceTaxRate:
  enum City:
    case RioDeJaneiro, SaoPaulo

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[City, Double]] = SortedMap(
    LocalDate.parse("01/01/0001", dateFormatter) -> Map(RioDeJaneiro -> 0.05, SaoPaulo -> 0.05),
    LocalDate.parse("30/05/2017", dateFormatter) -> Map(RioDeJaneiro -> 0.05, SaoPaulo -> 0.05),
    LocalDate.parse("18/08/2019", dateFormatter) -> Map(RioDeJaneiro -> 0.065, SaoPaulo -> 0.065)
  )

  def at(tradingDate: LocalDate): ServiceTaxRate = ServiceTaxRate(ratesHistory).at(tradingDate)

  def in(city: City): ServiceTaxRate = ServiceTaxRate(ratesHistory).in(city)

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean = date.isBefore(other) || date.equals(other)