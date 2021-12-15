package com.andreidiego.mpfi.stocks.deprecated

import java.time.temporal._
import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import java.util.Locale

object TimeSpan {
  //  TODO Return a TimeSpan
  //  TODO Research why I am being allowed to use a non-exhaustive match
  //  TODO Research why is compiling not failing since I am not providing an implicit definition of clock
  def `this`[T <: TimeSpan](timeSpan: T)(implicit clock: Clock): T = timeSpan

  def `this`(timeUnit: SingleTimeUnit.Value)(implicit clock: Clock): TimeSpan = {
    val now = LocalDateTime.now(clock)

    timeUnit match {
      case SingleTimeUnit.MINUTE =>
        Minute(now.getMinute)

      case SingleTimeUnit.HOUR =>
        Hour(now.getHour)

      case SingleTimeUnit.DAY =>
        Day(now.getDayOfMonth)

      case SingleTimeUnit.WEEK =>
        val firstDayOfWeekInMonth = now.`with`(TemporalAdjusters.firstDayOfMonth()).getDayOfWeek.getValue
        val todaysPositionInCalendar = now.getDayOfMonth + firstDayOfWeekInMonth
        val todayIsTheLastDayOfWeek = todaysPositionInCalendar % 7 == 0
        val k: Int = todaysPositionInCalendar / 7
        val week = if (todayIsTheLastDayOfWeek) k else k + 1
        Week(week)

      case SingleTimeUnit.MONTH =>
        Month(now.getMonthValue)

      case SingleTimeUnit.BIMESTER =>
        val month = now.getMonthValue
        Bimester((month / 2) + (month % 2))

      case SingleTimeUnit.TRIMESTER =>
        Trimester(now.get(IsoFields.QUARTER_OF_YEAR))

      case SingleTimeUnit.QUARTER =>
        Quarter(now.get(IsoFields.QUARTER_OF_YEAR))

      case SingleTimeUnit.SEMESTER =>
        Semester(((now.getMonthValue - 1) / 6) + 1)

      case SingleTimeUnit.YEAR =>
        Year(now.getYear)

      case SingleTimeUnit.DECADE =>
        Decade(now.getYear / 10 * 10)
    }
  }
}

//  TODO Create a TimeSpan sealed hierarchy
sealed trait TimeSpan {

  def dateTime: LocalDateTime

  def month: Int = dateTime.getMonthValue

  def includes(moment: TimeSpan): Boolean

}

case class Minute(minute: Int)(implicit clock: Clock) extends TimeSpan {

  private var dt = LocalDateTime.now(clock).withMinute(minute)
  require(minute >= 0 && minute <= 59, s"Minute should be between 0 and 59 but it is $minute")

  override def dateTime: LocalDateTime = dt

  private def this()(implicit clock: Clock) = this(LocalDateTime.now(clock).getMinute)(clock)

  private def this(localDateTime: LocalDateTime) = {
    this(localDateTime.getMinute)(Clock.fixed(localDateTime.atZone(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault()))
    dt = localDateTime
  }

  override def includes(moment: TimeSpan) = true
}

object Minute {
  def fromDateTime(dateTime: LocalDateTime): Minute = new Minute(dateTime)

  def apply()(implicit clock: Clock): Minute = new Minute()(clock)

  def apply(minute: Int)(implicit clock: Clock): Minute = new Minute(minute)(clock)
}

case class Hour(hour: Int) extends TimeSpan {
  require(hour >= 0 && hour <= 23, s"Hour should be between 0 and 23 but it is $hour")

  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Day(day: Int)(implicit clock: Clock) extends TimeSpan {
  private val now = LocalDate.now(clock)
  override val month: Int = now.getMonthValue


  val limit: Int = month match {
    case 2 => if (now.isLeapYear) 29 else 28
    case 4 | 6 | 9 | 11 => 30
    case 1 | 3 | 5 | 7 | 8 | 10 | 12 => 31
  }
  require(day >= 1 && day <= limit, s"Day should be between 1 and $limit but, it is $day")


  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()

}

case class Week(week: Int)(implicit clock: Clock) extends TimeSpan {
  override def dateTime: LocalDateTime = LocalDateTime.now()

  private val today = LocalDate.now(clock)

  private val firstDayOfMonth = today.`with`(TemporalAdjusters.firstDayOfMonth())
  private val lastDayOfMonth = today.`with`(TemporalAdjusters.lastDayOfMonth())
  private var oneWeekForward = firstDayOfMonth

  private var limit = 1

  while (oneWeekForward.isBefore(lastDayOfMonth)) {
    oneWeekForward = oneWeekForward.plusDays(7)
    limit += 1
  }
  private val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()


  limit = if (lastDayOfMonth.get(woy) == oneWeekForward.get(woy)) limit else limit - 1

  require(week >= 1 && week <= limit, s"Week should be between 1 and $limit but, it is $week")

  override def includes(moment: TimeSpan): Boolean = ???

}

case class Month(override val month: Int) extends TimeSpan {
  require(month >= 1 && month <= 12, s"Month should be between 1 and 12 but it is $month")

  override def includes(moment: TimeSpan): Boolean = moment match {
    case Week(_) | Day(_) | Hour(_) | Minute(_) => moment.month == month
    case _ => false
  }

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

object January extends Month(1)

object Jan extends Month(1)

object February extends Month(2)

object Feb extends Month(2)

object March extends Month(3)

object Mar extends Month(3)

object April extends Month(4)

object Apr extends Month(4)

object May extends Month(5)

object June extends Month(6)

object Jun extends Month(6)

object July extends Month(7)

object Jul extends Month(7)

object August extends Month(8)

object Aug extends Month(8)

object September extends Month(9)

object Sep extends Month(9)

object October extends Month(10)

object Oct extends Month(10)

object November extends Month(11)

object Nov extends Month(11)

object December extends Month(12)

object Dec extends Month(12)

case class Bimester(bimester: Int) extends TimeSpan {
  require(bimester >= 1 && bimester <= 6, s"Bimester should be between 1 and 6 but it is $bimester")

  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Trimester(trimester: Int) extends TimeSpan {
  require(trimester >= 1 && trimester <= 4, s"Trimester should be between 1 and 4 but it is $trimester")

  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Quarter(quarter: Int) extends TimeSpan {
  require(quarter >= 1 && quarter <= 4, s"Quarter should be between 1 and 4 but it is $quarter")

  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Semester(semester: Int) extends TimeSpan {
  require(semester >= 1 && semester <= 2, s"Semester should be between 1 and 2 but it is $semester")

  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Year(year: Int) extends TimeSpan {
  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}

case class Decade(decade: Int) extends TimeSpan {
  override def includes(moment: TimeSpan) = true

  override def dateTime: LocalDateTime = LocalDateTime.now()
}