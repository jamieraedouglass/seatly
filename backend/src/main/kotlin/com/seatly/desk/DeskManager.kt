package com.seatly.desk

import jakarta.inject.Singleton
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Singleton
class DeskManager(
  private val deskRepository: DeskRepository,
  private val bookingRepository: BookingRepository,
  private val recurringBookingSeriesRepository: RecurringBookingSeriesRepository,
) {
  fun createDesk(command: CreateDeskCommand): DeskDto {
    val desk =
      Desk(
        name = command.name,
        location = command.location,
      )

    val savedDesk = deskRepository.save(desk)
    return DeskDto.from(savedDesk)
  }

  fun listDesks(): List<DeskDto> = deskRepository.findAll().map { DeskDto.from(it) }

  fun listDeskAvailability(
    deskId: Long,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
  ): List<AvailabilityDto> {
    require(!endAt.isBefore(startAt)) {
      "endAt must not be before startAt"
    }

    val normalizedRequestedStart = startAt.truncatedTo(ChronoUnit.MINUTES)
    val normalizedRequestedEnd = endAt.truncatedTo(ChronoUnit.MINUTES)

    val windowStart = normalizedRequestedStart.roundDownToHalfHour()
    val windowEnd = normalizedRequestedEnd.roundUpToHalfHour()

    if (!windowStart.isBefore(windowEnd)) {
      return emptyList()
    }

    val bookings =
      bookingRepository.findOverlappingBookings(
        deskId = deskId,
        startAt = windowStart,
        endAt = windowEnd,
      )

    val slots = mutableListOf<AvailabilityDto>()
    var slotStart = windowStart
    val slotMinutes = 30L

    while (slotStart.isBefore(windowEnd)) {
      val slotEnd = slotStart.plusMinutes(slotMinutes)

      val isBooked =
        bookings.any { booking ->
          booking.startAt.isBefore(slotEnd) && booking.endAt.isAfter(slotStart)
        }

      val status =
        if (isBooked) AvailabilityStatus.BOOKED else AvailabilityStatus.AVAILABLE

      slots.add(
        AvailabilityDto(
          startAt = slotStart,
          endAt = slotEnd,
          status = status,
        ),
      )

      slotStart = slotEnd
    }

    return slots
  }

  fun createBooking(command: CreateBookingCommand): BookingDto {
    require(command.startAt.isBefore(command.endAt)) {
      "startAt must be before endAt"
    }

    val normalizedStart = command.startAt.truncatedTo(ChronoUnit.MINUTES)
    val normalizedEnd = command.endAt.truncatedTo(ChronoUnit.MINUTES)

    if (bookingRepository.existsOverlappingBooking(command.deskId, normalizedStart, normalizedEnd)) {
      throw IllegalStateException("Desk is already booked for the given time range")
    }

    val booking =
      Booking(
        deskId = command.deskId,
        userId = command.userId,
        startAt = normalizedStart,
        endAt = normalizedEnd,
      )

    val savedBooking = bookingRepository.save(booking)
    return BookingDto.from(savedBooking)
  }

  fun createRecurringBooking(command: CreateRecurringBookingCommand): List<BookingDto> {
    require(command.startTime.isBefore(command.endTime)) {
      "startTime must be before endTime"
    }

    // using TemporalAdjusters here to find the first matching day of week on or after
    // startingFrom -- had to look this up, wasn't sure if there was a simpler way
    val firstDate = command.startingFrom.with(
      TemporalAdjusters.nextOrSame(DayOfWeek.of(command.dayOfWeek))
    )

    // build out all the date ranges first so we can check conflicts before saving anything
    // didn't want to save week 1 and 2 and then hit a conflict on week 3
    val ranges = (0 until command.occurrences).map { week ->
      val date = firstDate.plusWeeks(week)
      val startAt = date.atTime(command.startTime).truncatedTo(ChronoUnit.MINUTES)
      val endAt = date.atTime(command.endTime).truncatedTo(ChronoUnit.MINUTES)
      startAt to endAt
    }

    // validate all occurrences upfront
    for ((startAt, endAt) in ranges) {
      if (bookingRepository.existsOverlappingBooking(command.deskId, startAt, endAt)) {
        throw IllegalStateException("Desk is already booked for $startAt – $endAt")
      }
    }

    val series = recurringBookingSeriesRepository.save(
      RecurringBookingSeries(
        deskId = command.deskId,
        userId = command.userId,
        dayOfWeek = command.dayOfWeek,
        startTime = command.startTime,
        endTime = command.endTime,
        occurrences = command.occurrences,
      )
    )

    // save each booking tied to the series
    // not sure if there's a saveAll equivalent here that would be cleaner
    return ranges.map { (startAt, endAt) ->
      val saved = bookingRepository.save(
        Booking(
          deskId = command.deskId,
          userId = command.userId,
          startAt = startAt,
          endAt = endAt,
          seriesId = series.id,
        )
      )
      BookingDto.from(saved)
    }
  }
}

private fun LocalDateTime.roundDownToHalfHour(): LocalDateTime {
  val minute = if (this.minute < 30) 0 else 30
  return this
    .withMinute(minute)
    .withSecond(0)
    .withNano(0)
}

private fun LocalDateTime.roundUpToHalfHour(): LocalDateTime {
  val needsIncrement = this.minute % 30 != 0 || this.second != 0 || this.nano != 0
  val base =
    if (needsIncrement) this.plusMinutes(30 - (this.minute % 30).toLong()) else this
  val minute = if (base.minute < 30) 0 else 30
  return base
    .withMinute(minute)
    .withSecond(0)
    .withNano(0)
}

data class CreateDeskCommand(
  val name: String,
  val location: String?,
)

data class DeskDto(
  val id: Long,
  val name: String,
  val location: String?,
) {
  companion object {
    fun from(desk: Desk): DeskDto =
      DeskDto(
        id = desk.id!!,
        name = desk.name,
        location = desk.location,
      )
  }
}

data class AvailabilityDto(
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
  val status: AvailabilityStatus,
)

data class CreateBookingCommand(
  val deskId: Long,
  val userId: Long,
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
)

data class BookingDto(
  val id: Long,
  val deskId: Long,
  val userId: Long,
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
  val seriesId: Long? = null,
) {
  companion object {
    fun from(booking: Booking): BookingDto =
      BookingDto(
        id = booking.id!!,
        deskId = booking.deskId,
        userId = booking.userId,
        startAt = booking.startAt,
        endAt = booking.endAt,
        seriesId = booking.seriesId,
      )
  }
}

data class CreateRecurringBookingCommand(
  val deskId: Long,
  val userId: Long,
  val dayOfWeek: Int,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val occurrences: Int,
  val startingFrom: LocalDate,
)