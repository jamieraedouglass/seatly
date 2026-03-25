package com.seatly.desk

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Column
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

// JPA needs default values here but these never get used
@Entity
@Table(name = "recurring_booking_series")
data class RecurringBookingSeries(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "desk_id", nullable = false)
    val deskId: Long = -1,
    @Column(name = "user_id", nullable = false)
    val userId: Long = -1,
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int = 1,
    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime = LocalTime.MIDNIGHT,
    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime = LocalTime.MIDNIGHT,
    @Column(name = "occurrences", nullable = false)
    val occurrences: Int = 1,
)

@Repository
interface RecurringBookingSeriesRepository : JpaRepository<RecurringBookingSeries, Long>