package com.andreidiego.mpfi.stocks.deprecated

import com.andreidiego.mpfi.stocks.deprecated.July
import com.andreidiego.mpfi.stocks.deprecated.TimeSpan.*
import org.scalatest.freespec.AnyFreeSpec

import java.time.{Clock, LocalDateTime, ZoneId}
import scala.language.deprecated.symbolLiterals

class SalesTest extends AnyFreeSpec {
  "Sales" - {
    "can be selected for a given time span" in {
      val zoneId = ZoneId.systemDefault

      implicit var clock: Clock = Clock.fixed(LocalDateTime.of(2017, 7, 18, 10, 2).atZone(zoneId).toInstant, zoneId)
      val tpis3 = Sale(`this`(Minute()), 'TPIS3, 4900, 3.90)

      clock = Clock.fixed(LocalDateTime.of(2017, 7, 19, 13, 59).atZone(zoneId).toInstant, zoneId)
      val posi3 = Sale(`this`(Minute()), 'POSI3, 2900, 3.90)

      clock = Clock.fixed(LocalDateTime.of(2017, 7, 27, 12, 14).atZone(zoneId).toInstant, zoneId)
      val lame4 = Sale(`this`(Minute()), 'LAME4, 400, 15.50)

      assert(Sales.of(July).list == List(tpis3, posi3, lame4))
    }
  }
}