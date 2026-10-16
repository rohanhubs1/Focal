package com.focal.app.streak

import com.focal.app.model.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * End reason for a focus session used in Streak calculations.
 */
enum class StreakEndReason {
    TIMER_FINISHED,         // Counted: Timer ran to completion
    SCHEDULE_ENDED,         // Counted: Scheduled window finished to completion
    PARENT_REMOTE_ENDED,    // Counted: Parent ended the session remotely
    CARD_EARLY,             // NOT counted: Ended with NFC card before end time
    EMERGENCY_EXIT,         // NOT counted: Emergency exit override
    CANCELLED               // NOT counted: Cancelled early
}

/**
 * Focus session history record for streak tracking.
 */
data class StreakSessionRecord(
    val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val targetDurationMinutes: Int,
    val endReason: StreakEndReason,
    val remainingSecondsAtEnd: Int = 0
) {
    /**
     * A day counts if the child completed at least one focus session that day that was
     * NOT ended early: the timer finished, a schedule ran to its end, or the parent
     * ended it remotely. Sessions ended by emergency exit or by card before the end time do not count.
     */
    fun isCounted(): Boolean {
        return when (endReason) {
            StreakEndReason.TIMER_FINISHED,
            StreakEndReason.SCHEDULE_ENDED,
            StreakEndReason.PARENT_REMOTE_ENDED -> true
            StreakEndReason.CARD_EARLY,
            StreakEndReason.EMERGENCY_EXIT,
            StreakEndReason.CANCELLED -> false
        }
    }
}

/**
 * Civil date representation (year, month, day).
 */
data class StreakDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<StreakDate> {
    override fun compareTo(other: StreakDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    override fun toString(): String {
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return "$year-$m-$d"
    }
}

object StreakCalculator {
    const val MILLIS_PER_DAY = 86400000L

    /**
     * Returns the epoch day (days since 1970-01-01) for the given timestamp in the specified timezone offset.
     */
    fun epochDayFromMillis(millis: Long, offsetMillis: Long): Long {
        val localMillis = millis + offsetMillis
        return if (localMillis >= 0) {
            localMillis / MILLIS_PER_DAY
        } else {
            (localMillis - MILLIS_PER_DAY + 1) / MILLIS_PER_DAY
        }
    }

    /**
     * Converts epoch day to a StreakDate (year, month, day).
     */
    fun epochDayToDate(epochDay: Long): StreakDate {
        val z = epochDay + 719468L
        val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1028 + doe / 1461 - doe / 36524) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (if (m <= 2) y + 1 else y).toInt()
        return StreakDate(year, m, d)
    }

    /**
     * Computes the set of epoch days that received focus credit from the given sessions.
     * Sessions crossing midnight credit all calendar days they were active across.
     */
    fun computeCountedDays(
        sessions: List<StreakSessionRecord>,
        offsetMillis: Long,
        maxDay: Long? = null
    ): Set<Long> {
        val countedDays = mutableSetOf<Long>()
        for (session in sessions) {
            if (session.isCounted()) {
                val startDay = epochDayFromMillis(session.startTimeMillis, offsetMillis)
                val endDay = epochDayFromMillis(session.endTimeMillis, offsetMillis)
                for (day in startDay..endDay) {
                    if (maxDay == null || day <= maxDay) {
                        countedDays.add(day)
                    }
                }
            }
        }
        return countedDays
    }

    /**
     * Calculates the streak count:
     * - Number of consecutive counted days up to today.
     * - If no session has completed yet today, show the streak up to yesterday.
     * - A day with no counted session resets the streak to 0.
     */
    fun calculateStreak(
        sessions: List<StreakSessionRecord>,
        nowMillis: Long,
        offsetMillis: Long
    ): Int {
        val today = epochDayFromMillis(nowMillis, offsetMillis)
        val countedDays = computeCountedDays(sessions, offsetMillis, maxDay = today)

        if (countedDays.isEmpty()) return 0

        val startEvaluationDay = if (countedDays.contains(today)) {
            today
        } else {
            val yesterday = today - 1L
            if (countedDays.contains(yesterday)) {
                yesterday
            } else {
                return 0
            }
        }

        var streak = 0
        var current = startEvaluationDay
        while (countedDays.contains(current)) {
            streak++
            current--
        }
        return streak
    }
}

/**
 * Streak Repository interface.
 */
interface StreakRepository {
    /**
     * Observable flow of the child's current streak.
     */
    val currentStreak: Flow<Int>

    /**
     * Current synchronous streak value.
     */
    fun getStreak(): Int

    /**
     * Add a newly completed focus session record.
     */
    fun addSession(session: StreakSessionRecord)

    /**
     * Set the entire session history.
     */
    fun setSessions(sessions: List<StreakSessionRecord>)

    /**
     * Get the current session history list.
     */
    fun getSessions(): List<StreakSessionRecord>

    /**
     * Get all counted dates that have completed focus sessions.
     */
    fun getCountedDates(): Set<StreakDate>

    companion object {
        val instance: StreakRepository by lazy {
            val now = currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val seedSessions = listOf(
                StreakSessionRecord("seed_1", now - 2 * dayMs, now - 2 * dayMs + 1800000L, 30, StreakEndReason.TIMER_FINISHED),
                StreakSessionRecord("seed_2", now - 1 * dayMs, now - 1 * dayMs + 2400000L, 40, StreakEndReason.TIMER_FINISHED)
            )
            DefaultStreakRepository(initialSessions = seedSessions)
        }
    }
}

class DefaultStreakRepository(
    initialSessions: List<StreakSessionRecord> = emptyList(),
    private val timeProvider: () -> Long = { currentTimeMillis() },
    private val timeZoneOffsetProvider: () -> Long = { getDeviceTimeZoneOffsetMillis() }
) : StreakRepository {

    private val _sessions = MutableStateFlow(initialSessions)

    override val currentStreak: Flow<Int> = _sessions.map { sessions ->
        StreakCalculator.calculateStreak(
            sessions = sessions,
            nowMillis = timeProvider(),
            offsetMillis = timeZoneOffsetProvider()
        )
    }

    override fun getStreak(): Int {
        return StreakCalculator.calculateStreak(
            sessions = _sessions.value,
            nowMillis = timeProvider(),
            offsetMillis = timeZoneOffsetProvider()
        )
    }

    override fun addSession(session: StreakSessionRecord) {
        _sessions.value = _sessions.value + session
    }

    override fun setSessions(sessions: List<StreakSessionRecord>) {
        _sessions.value = sessions
    }

    override fun getSessions(): List<StreakSessionRecord> {
        return _sessions.value
    }

    override fun getCountedDates(): Set<StreakDate> {
        val epochDays = StreakCalculator.computeCountedDays(
            sessions = _sessions.value,
            offsetMillis = timeZoneOffsetProvider()
        )
        return epochDays.map { StreakCalculator.epochDayToDate(it) }.toSet()
    }
}
