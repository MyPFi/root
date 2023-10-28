package com.andreidiego.mpfi.stocks.deprecated

import SingleTimeUnit.*
import TimeSpan.`this`
import org.scalatest.freespec.AnyFreeSpec

import java.time.{Clock, Duration, LocalDateTime, ZoneId}

class TimeSpanTest extends AnyFreeSpec {

  "A TimeSpan, when configured with" - {
    val REFERENCE_DATE_TIME: LocalDateTime = LocalDateTime.of(2017, 8, 23, 19, 23)
    val DEFAULT_ZONE: ZoneId = ZoneId.systemDefault
    implicit val FIXED_CLOCK: Clock = Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
    "`this`, may represent the current" - {
      "minute," in {
        // Date set with the implicit clock: 2017-8-23 19:23

        assert(`this`(MINUTE) == Minute(23))
        assert(`this`(MINUTE)(Clock.offset(FIXED_CLOCK, Duration.ofMinutes(1))) == Minute(24))
      }
      "hour," in {
        assert(`this`(HOUR) == Hour(19))
        assert(`this`(HOUR)(Clock.offset(FIXED_CLOCK, Duration.ofHours(1))) == Hour(20))
      }
      "day," in {
        assert(`this`(DAY) == Day(23))
        assert(`this`(DAY)(Clock.offset(FIXED_CLOCK, Duration.ofDays(1))) == Day(24))
      }
      "week," in {
        assert(`this`(WEEK) == Week(4))
        assert(`this`(WEEK)(Clock.offset(FIXED_CLOCK, Duration.ofDays(7))) == Week(5))
      }
      "month," in {
        assert(`this`(MONTH) == Month(8))
        assert(`this`(MONTH)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusMonths(1).toInstant, DEFAULT_ZONE)) == Month(9))
      }
      "bimester," in {
        assert(`this`(BIMESTER) == Bimester(4))
        assert(`this`(BIMESTER)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusMonths(2).toInstant, DEFAULT_ZONE)) == Bimester(5))
      }
      "trimester," ignore {
        //  TODO Research about aliases. How to give two names for the same concept/class
        assert(`this`(TRIMESTER) == Trimester(3) && `this`(TRIMESTER) == Quarter(3))
        assert(`this`(TRIMESTER)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusMonths(3).toInstant, DEFAULT_ZONE)) == Trimester(4))
      }
      "quarter," ignore {
        assert(`this`(QUARTER) == Quarter(3) && `this`(QUARTER) == Trimester(3))
      }
      "semester," in {
        assert(`this`(SEMESTER) == Semester(2))
        assert(`this`(SEMESTER)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusMonths(6).toInstant, DEFAULT_ZONE)) == Semester(1))
      }
      "year," in {
        assert(`this`(YEAR) == Year(2017))
        assert(`this`(YEAR)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusYears(1).toInstant, DEFAULT_ZONE)) == Year(2018))
      }
      "and, decade" in {
        assert(`this`(DECADE) == Decade(2010))
        assert(`this`(DECADE)(Clock.fixed(REFERENCE_DATE_TIME.atZone(DEFAULT_ZONE).plusYears(10).toInstant, DEFAULT_ZONE)) == Decade(2020))
      }
    }
    "last" ignore {
    }
    "a specific time unit, should obey the natural limits for that unit" - {
      "minutes should be between 0 and 59" ignore {
        assertThrows[IllegalArgumentException] {
          Minute(-1)
        }
        assert(Minute(0).minute == 0)
        assert(Minute(59).minute == 59)
        assertThrows[IllegalArgumentException] {
          Minute(60)
        }
      }
      "hours should be between 0 and 23" in {
        assertThrows[IllegalArgumentException] {
          Hour(-1)
        }
        assert(Hour(0).hour == 0)
        assert(Hour(23).hour == 23)
        assertThrows[IllegalArgumentException] {
          Hour(24)
        }
      }
      "days should be between" - {
        "1, and" in {
          assertThrows[IllegalArgumentException] {
            Day(0)
          }
          assert(Day(1).day == 1)
        }
        "depending on the month," - {
          "28, on February of non-leap years," in {
            val clock = Clock.fixed(LocalDateTime.of(2017, 2, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
            assert(Day(28)(clock).day == 28)
            assertThrows[IllegalArgumentException] {
              Day(29)(clock)
            }
          }
          "29, on February of leap years," in {
            val clock = Clock.fixed(LocalDateTime.of(2016, 2, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
            assert(Day(29)(clock).day == 29)
            assertThrows[IllegalArgumentException] {
              Day(30)(clock)
            }
          }
          "30, on " - {
            "April," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 4, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(30)(clock).day == 30)
              assertThrows[IllegalArgumentException] {
                Day(31)(clock)
              }
            }
            "June," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 6, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(30)(clock).day == 30)
              assertThrows[IllegalArgumentException] {
                Day(31)(clock)
              }
            }
            "September" in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 9, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(30)(clock).day == 30)
              assertThrows[IllegalArgumentException] {
                Day(31)(clock)
              }
            }
            "and November," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 11, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(30)(clock).day == 30)
              assertThrows[IllegalArgumentException] {
                Day(31)(clock)
              }
            }
          }
          "and, 31 on" - {
            "January," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 1, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "March," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 3, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "May," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 5, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "July," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 7, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "August," in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 8, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "October" in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 10, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
            "and, December" in {
              val clock = Clock.fixed(LocalDateTime.of(2017, 12, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
              assert(Day(31)(clock).day == 31)
              assertThrows[IllegalArgumentException] {
                Day(32)(clock)
              }
            }
          }
        }
      }
      "weeks should be between" - {
        "1" in {
          assertThrows[IllegalArgumentException] {
            Week(0)
          }
          assert(Week(1).week == 1)
        }
        "and, depending on the month," - {
          "4" in {
            val clock = Clock.fixed(LocalDateTime.of(2015, 2, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
            assert(Week(4)(clock).week == 4)
            assertThrows[IllegalArgumentException] {
              Week(5)(clock)
            }
          }
          "5" in {
            val clock = Clock.fixed(LocalDateTime.of(2017, 8, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
            assert(Week(5)(clock).week == 5)
            assertThrows[IllegalArgumentException] {
              Week(6)(clock)
            }
          }
          "or even, 6" in {
            val clock = Clock.fixed(LocalDateTime.of(2017, 12, 23, 0, 0).atZone(DEFAULT_ZONE).toInstant, DEFAULT_ZONE)
            assert(Week(6)(clock).week == 6)
            assertThrows[IllegalArgumentException] {
              Week(7)(clock)
            }
          }
        }
      }
      "months should be between 1 and 12" in {
        assertThrows[IllegalArgumentException] {
          Month(0)
        }
        assert(Month(1).month == 1)
        assert(Month(12).month == 12)
        assertThrows[IllegalArgumentException] {
          Month(13)
        }
      }
      "bimesters should be between 1 and 6" in {
        assertThrows[IllegalArgumentException] {
          Bimester(0)
        }
        assert(Bimester(1).bimester == 1)
        assert(Bimester(6).bimester == 6)
        assertThrows[IllegalArgumentException] {
          Bimester(7)
        }
      }
      "trimesters should be between 1 and 4" in {
        assertThrows[IllegalArgumentException] {
          Trimester(0)
        }
        assert(Trimester(1).trimester == 1)
        assert(Trimester(4).trimester == 4)
        assertThrows[IllegalArgumentException] {
          Trimester(5)
        }
      }
      "quarters should be between 1 and 4" in {
        assertThrows[IllegalArgumentException] {
          Quarter(0)
        }
        assert(Quarter(1).quarter == 1)
        assert(Quarter(4).quarter == 4)
        assertThrows[IllegalArgumentException] {
          Quarter(5)
        }
      }
      "semesters should be between 1 and 2" in {
        assertThrows[IllegalArgumentException] {
          Semester(0)
        }
        assert(Semester(1).semester == 1)
        assert(Semester(2).semester == 2)
        assertThrows[IllegalArgumentException] {
          Semester(3)
        }
      }
    }
    "-" ignore {
      "is able to tell its precision," ignore {
        /*
                assert(`this`(MINUTE).precision == MINUTE)
                assert(`this`(HOUR).precision == HOUR)
                assert(`this`(DAY).precision == DAY)
                assert(`this`(WEEK).precision == WEEK)
                assert(`this`(MONTH).precision == MONTH)
                assert(`this`(BIMESTER).precision == BIMESTER)
                assert(`this`(TRIMESTER).precision == TRIMESTER || `this`(TRIMESTER).precision == QUARTER)
                assert(`this`(QUARTER).precision == QUARTER || `this`(QUARTER).precision == TRIMESTER)
                assert(`this`(SEMESTER).precision == SEMESTER)
                assert(`this`(YEAR).precision == YEAR)
                assert(`this`(DECADE).precision == DECADE)
        */
      }
      "when it starts," ignore {
        // Date set with the implicit clock: 2017-8-23 19:23
        /*
                assert(`this`(MINUTE).startsAt == 23)
                assert(`this`(HOUR).startsAt == 19)
                assert(`this`(DAY).startsAt == 23)
                assert(`this`(WEEK).startsAt == 4)
                assert(`this`(MONTH).startsAt == 8)
                assert(`this`(BIMESTER).startsAt == 4)
                assert(`this`(TRIMESTER).startsAt == 3)
                assert(`this`(QUARTER).startsAt == 3)
                assert(`this`(SEMESTER).startsAt == 2)
                assert(`this`(YEAR).startsAt == 2017)
                assert(`this`(DECADE).startsAt == 2010)
        */
      }
      "and for how long it lasts" ignore {
        /*
                assert(`this`(MINUTE).span == 1)
                assert(`this`(HOUR).span == 1)
                assert(`this`(DAY).span == 1)
                assert(`this`(WEEK).span == 1)
                assert(`this`(MONTH).span == 1)
                assert(`this`(BIMESTER).span == 1)
                assert(`this`(TRIMESTER).span == 1)
                assert(`this`(QUARTER).span == 1)
                assert(`this`(SEMESTER).span == 1)
                assert(`this`(YEAR).span == 1)
                assert(`this`(DECADE).span == 1)
        */
      }
    }
  }
}