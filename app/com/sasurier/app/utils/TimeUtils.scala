package com.sasurier.app.utils

import com.google.inject.ImplementedBy
import java.time._

@ImplementedBy(classOf[SystemTimeClock])
trait CommonClock {
  def now(): Instant
  def nowInMillis(): Long

  //TODO Move this stuff to a time utility class since it's not really part of the clock.

  val EUROPE_TIME_ZONE = ZoneId.of("Europe/Paris")

  // https://www.redcort.com/us-federal-bank-holidays
  val ES_BANK_HOLIDAYS = Array(LocalDate.of(2017, 12, 25))

  def businessDaysAgo(
    days: Int,
    startDay: LocalDate = LocalDate.now(EUROPE_TIME_ZONE),
    holidays: Array[LocalDate] = ES_BANK_HOLIDAYS): LocalDate

  def businessDaysFrom(
    days: Int,
    startDay: LocalDate = LocalDate.now(EUROPE_TIME_ZONE),
    holidays: Array[LocalDate] = ES_BANK_HOLIDAYS): LocalDate

  def businessDaysFromNow(
    days: Int,
    startTime: LocalDateTime = LocalDateTime.now(EUROPE_TIME_ZONE),
    holidays: Array[LocalDate] = ES_BANK_HOLIDAYS): LocalDate

  def localDateAtNoonToUTC(local: LocalDate, timeZone: ZoneId = EUROPE_TIME_ZONE): Instant

  def isBusinessDay(day: LocalDate, holidays: Array[LocalDate] = ES_BANK_HOLIDAYS): Boolean

}

class SystemTimeClock extends CommonClock {
  override def now(): Instant = Instant.now
  override def nowInMillis(): Long = System.currentTimeMillis()

  override def businessDaysAgo(numDays: Int, startDay: LocalDate, holidays: Array[LocalDate]): LocalDate = {
    var day = startDay
    var dayCount = numDays
    while (dayCount > 0) {
      if (day.getDayOfWeek != DayOfWeek.SATURDAY &&
        day.getDayOfWeek != DayOfWeek.SUNDAY &&
        ! holidays.contains(day)) {
        dayCount = dayCount - 1
      }
      day = day.minusDays(1)
    }
    day
  }

  override def isBusinessDay(day: LocalDate, holidays: Array[LocalDate]) = {
    day.getDayOfWeek != DayOfWeek.SATURDAY &&
      day.getDayOfWeek != DayOfWeek.SUNDAY &&
      ! holidays.contains(day)
  }

  override def businessDaysFromNow(numDays: Int, now: LocalDateTime, holidays: Array[LocalDate]) = {
    val today = if (now.getHour < 12) now.toLocalDate else now.plusHours(24).toLocalDate
    businessDaysFrom(numDays, today, holidays)
  }

  override def businessDaysFrom(numDays: Int, startDay: LocalDate, holidays: Array[LocalDate]) = {
    var day = startDay
    var dayCount = 0
    while(!isBusinessDay(day, holidays)) day = day.plusDays(1)
    while (dayCount < numDays) {
      day = day.plusDays(1)
      if (isBusinessDay(day, holidays)) {
        dayCount = dayCount + 1
      }
    }
    day
  }

  override def localDateAtNoonToUTC(local: LocalDate, timeZone: ZoneId) = local.atTime(12, 0).atZone(timeZone).toInstant

}

