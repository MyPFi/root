package com.andreidiego.mpfi.stocks.deprecated

import java.time.{Clock, Duration, LocalDateTime, ZoneId}

object FixedClock {

  val REFERENCE_DATE_TIME: LocalDateTime = LocalDateTime.of(2017, 8, 23, 19, 23)

  val DEFAULT_ZONE: ZoneId = ZoneId.systemDefault

  implicit val FIXED_CLOCK: Clock = Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)

  FIXED_CLOCK.millis()

  Clock.offset(FIXED_CLOCK, Duration.ofMinutes(1)).millis()
}
