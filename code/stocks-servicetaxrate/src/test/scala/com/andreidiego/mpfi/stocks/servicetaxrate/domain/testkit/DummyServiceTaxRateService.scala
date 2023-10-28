package com.andreidiego.mpfi.stocks.servicetaxrate.domain.testkit

import java.time.LocalDate
import scala.collection.SortedMap
import com.andreidiego.mpfi.stocks.servicetaxrate.domain.{City, ServiceTaxRateService}
import City.{RIO_DE_JANEIRO, SAO_PAULO}

class DummyServiceTaxRateService private(
  val ratesHistory: SortedMap[LocalDate, Map[City, Double]]
) extends ServiceTaxRateService:

  import DummyServiceTaxRateService.isNotAfter

  def at(tradingDate: LocalDate): ServiceTaxRateService =
    DummyServiceTaxRateService(
      SortedMap(
        ratesHistory
          .filter(_._1.isNotAfter(tradingDate))
          .last
      )
    )

  def in(city: City): ServiceTaxRateService =
    DummyServiceTaxRateService(
      ratesHistory
        .map { (rateRecord: (LocalDate, Map[City, Double])) =>
          rateRecord._1 → rateRecord._2.filter(_._1 == city)
        }
    )

  def value: Double =
    val ratesByCity: Map[City, Double] = ratesHistory.last._2

    if ratesByCity.size == 1 then ratesByCity.last._2
    else ratesByCity.getOrElse(RIO_DE_JANEIRO, 0.0)

object DummyServiceTaxRateService extends ServiceTaxRateService:
  import java.time.format.DateTimeFormatter

  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  private val ratesHistory: SortedMap[LocalDate, Map[City, Double]] = SortedMap(
    LocalDate.MIN -> Map(RIO_DE_JANEIRO -> 0.05, SAO_PAULO -> 0.05),
    LocalDate.parse("18/08/2019", dateFormatter) -> Map(RIO_DE_JANEIRO -> 0.065, SAO_PAULO -> 0.065)
  )

  def at(tradingDate: LocalDate): ServiceTaxRateService =
    DummyServiceTaxRateService(ratesHistory).at(tradingDate)

  def in(city: City): ServiceTaxRateService =
    DummyServiceTaxRateService(ratesHistory).in(city)

  def value: Double = 0.0

  extension (date: LocalDate)
    private def isNotAfter(other: LocalDate): Boolean =
      date.isBefore(other) || date.equals(other)