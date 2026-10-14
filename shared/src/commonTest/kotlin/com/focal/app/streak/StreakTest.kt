package com.focal.app.streak

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakTest {

    private val dayMillis = 86400000L

    @Test
    fun testConsecutiveDays() {
        // Day 100, Day 101, Day 102 each have completed sessions
        val sessions = listOf(
            StreakSessionRecord(
                id = "s1",
                startTimeMillis = 100 * dayMillis + 10 * 3600000L,
                endTimeMillis = 100 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            ),
            StreakSessionRecord(
                id = "s2",
                startTimeMillis = 101 * dayMillis + 14 * 3600000L,
                endTimeMillis = 101 * dayMillis + 15 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            ),
            StreakSessionRecord(
                id = "s3",
                startTimeMillis = 102 * dayMillis + 16 * 3600000L,
                endTimeMillis = 102 * dayMillis + 17 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            )
        )

        val now = 102 * dayMillis + 18 * 3600000L // Today is Day 102 evening
        val streak = StreakCalculator.calculateStreak(sessions, now, offsetMillis = 0L)
        assertEquals(3, streak, "3 consecutive days should yield streak of 3")
    }

    @Test
    fun testGapResetsToZero() {
        // Sessions on Day 100 and Day 101, but Day 102 was missed
        val sessions = listOf(
            StreakSessionRecord(
                id = "s1",
                startTimeMillis = 100 * dayMillis + 10 * 3600000L,
                endTimeMillis = 100 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            ),
            StreakSessionRecord(
                id = "s2",
                startTimeMillis = 101 * dayMillis + 10 * 3600000L,
                endTimeMillis = 101 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            )
        )

        // Today is Day 103 (Day 102 was a gap, and no session today)
        val nowDay103 = 103 * dayMillis + 12 * 3600000L
        val streakDay103 = StreakCalculator.calculateStreak(sessions, nowDay103, offsetMillis = 0L)
        assertEquals(0, streakDay103, "A gap day should reset streak to 0")

        // Now child completes a session on Day 103
        val sessionsWithDay103 = sessions + StreakSessionRecord(
            id = "s3",
            startTimeMillis = 103 * dayMillis + 13 * 3600000L,
            endTimeMillis = 103 * dayMillis + 14 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )
        val streakRestarted = StreakCalculator.calculateStreak(sessionsWithDay103, nowDay103 + 2 * 3600000L, offsetMillis = 0L)
        assertEquals(1, streakRestarted, "Completing session after gap restarts streak at 1")
    }

    @Test
    fun testEarlyEndedSessionDoesNotCount() {
        // Day 100: Completed normally (counts)
        val sessionDay100 = StreakSessionRecord(
            id = "s1",
            startTimeMillis = 100 * dayMillis + 10 * 3600000L,
            endTimeMillis = 100 * dayMillis + 11 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )
        assertTrue(sessionDay100.isCounted())

        // Day 101: Ended early by card before end time (does NOT count)
        val cardEarly = StreakSessionRecord(
            id = "s2_card",
            startTimeMillis = 101 * dayMillis + 10 * 3600000L,
            endTimeMillis = 101 * dayMillis + 10 * 3600000L + 20 * 60000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.CARD_EARLY,
            remainingSecondsAtEnd = 2400
        )
        assertFalse(cardEarly.isCounted())

        // Day 101: Ended early by emergency exit (does NOT count)
        val emergencyEarly = StreakSessionRecord(
            id = "s2_emerg",
            startTimeMillis = 101 * dayMillis + 14 * 3600000L,
            endTimeMillis = 101 * dayMillis + 14 * 3600000L + 15 * 60000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.EMERGENCY_EXIT,
            remainingSecondsAtEnd = 2700
        )
        assertFalse(emergencyEarly.isCounted())

        // Day 101: Cancelled early (does NOT count)
        val cancelled = StreakSessionRecord(
            id = "s2_cancel",
            startTimeMillis = 101 * dayMillis + 16 * 3600000L,
            endTimeMillis = 101 * dayMillis + 16 * 3600000L + 5 * 60000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.CANCELLED,
            remainingSecondsAtEnd = 3300
        )
        assertFalse(cancelled.isCounted())

        // With only early-ended sessions on Day 101, evaluated on Day 102
        val sessionsWithEarly = listOf(sessionDay100, cardEarly, emergencyEarly, cancelled)
        val nowDay102 = 102 * dayMillis + 10 * 3600000L
        val streakDay102 = StreakCalculator.calculateStreak(sessionsWithEarly, nowDay102, offsetMillis = 0L)
        assertEquals(0, streakDay102, "Early-ended sessions on Day 101 must not count, resetting streak on Day 102")

        // Valid completion reasons:
        // Schedule ended normally -> counts
        val scheduleEnded = StreakSessionRecord(
            id = "s_sched",
            startTimeMillis = 101 * dayMillis + 10 * 3600000L,
            endTimeMillis = 101 * dayMillis + 11 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.SCHEDULE_ENDED
        )
        assertTrue(scheduleEnded.isCounted())

        // Parent remote ended -> counts
        val parentRemote = StreakSessionRecord(
            id = "s_parent",
            startTimeMillis = 101 * dayMillis + 10 * 3600000L,
            endTimeMillis = 101 * dayMillis + 10 * 3600000L + 45 * 60000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.PARENT_REMOTE_ENDED
        )
        assertTrue(parentRemote.isCounted())

        val sessionsWithParentEnd = listOf(sessionDay100, parentRemote)
        val streakWithParent = StreakCalculator.calculateStreak(sessionsWithParentEnd, nowDay102, offsetMillis = 0L)
        assertEquals(2, streakWithParent, "Parent remote end counts as completed, keeping streak at 2")
    }

    @Test
    fun testTodayNotYetCompletedKeepsYesterdayStreak() {
        // Sessions completed on Day 100, Day 101, Day 102
        val sessions = listOf(
            StreakSessionRecord(
                id = "s1",
                startTimeMillis = 100 * dayMillis + 10 * 3600000L,
                endTimeMillis = 100 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            ),
            StreakSessionRecord(
                id = "s2",
                startTimeMillis = 101 * dayMillis + 10 * 3600000L,
                endTimeMillis = 101 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            ),
            StreakSessionRecord(
                id = "s3",
                startTimeMillis = 102 * dayMillis + 10 * 3600000L,
                endTimeMillis = 102 * dayMillis + 11 * 3600000L,
                targetDurationMinutes = 60,
                endReason = StreakEndReason.TIMER_FINISHED
            )
        )

        // Today is Day 103 morning (no session completed yet today)
        val nowDay103Morning = 103 * dayMillis + 8 * 3600000L
        val streak = StreakCalculator.calculateStreak(sessions, nowDay103Morning, offsetMillis = 0L)
        assertEquals(3, streak, "If no session has completed yet today, should show streak up to yesterday (3)")

        // If child then completes a session on Day 103
        val sessionsWithDay103 = sessions + StreakSessionRecord(
            id = "s4",
            startTimeMillis = 103 * dayMillis + 14 * 3600000L,
            endTimeMillis = 103 * dayMillis + 15 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )
        val streakAfterCompletion = StreakCalculator.calculateStreak(
            sessionsWithDay103,
            nowDay103Morning + 8 * 3600000L,
            offsetMillis = 0L
        )
        assertEquals(4, streakAfterCompletion, "After today's session completes, streak increments to 4")
    }

    @Test
    fun testSessionsCrossingMidnight() {
        // Day 99: completed session
        val sDay99 = StreakSessionRecord(
            id = "s99",
            startTimeMillis = 99 * dayMillis + 10 * 3600000L,
            endTimeMillis = 99 * dayMillis + 11 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )

        // Session starts Day 100 at 23:30 (11:30 PM) and finishes Day 101 at 00:30 (12:30 AM)
        val sMidnight = StreakSessionRecord(
            id = "s_midnight",
            startTimeMillis = 100 * dayMillis + 23 * 3600000L + 30 * 60000L,
            endTimeMillis = 101 * dayMillis + 30 * 60000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )

        val sessions = listOf(sDay99, sMidnight)

        // Counted days should include Day 99, Day 100, AND Day 101
        val countedDays = StreakCalculator.computeCountedDays(sessions, offsetMillis = 0L)
        assertTrue(countedDays.contains(99L), "Day 99 is counted")
        assertTrue(countedDays.contains(100L), "Day 100 is counted because session was active before midnight")
        assertTrue(countedDays.contains(101L), "Day 101 is counted because session finished after midnight")

        // Evaluated on Day 101 afternoon
        val nowDay101 = 101 * dayMillis + 14 * 3600000L
        val streak = StreakCalculator.calculateStreak(sessions, nowDay101, offsetMillis = 0L)
        assertEquals(3, streak, "Session crossing midnight seamlessly links Day 99, Day 100, and Day 101 into streak of 3")
    }

    @Test
    fun testDeviceTimezone() {
        // A session takes place at UTC timestamp 100 * dayMillis + 20 * 3600000L (20:00 UTC)
        val session = StreakSessionRecord(
            id = "s_tz",
            startTimeMillis = 100 * dayMillis + 19 * 3600000L,
            endTimeMillis = 100 * dayMillis + 20 * 3600000L,
            targetDurationMinutes = 60,
            endReason = StreakEndReason.TIMER_FINISHED
        )

        // In UTC (offset = 0):
        val utcOffset = 0L
        val countedUtc = StreakCalculator.computeCountedDays(listOf(session), utcOffset)
        assertTrue(countedUtc.contains(100L), "Under UTC, session counts for Day 100")
        assertFalse(countedUtc.contains(101L))

        // In India Standard Time (IST, UTC+5:30 = +19,800,000 ms):
        // 20:00 UTC + 5:30 = 01:30 AM next day (Day 101)
        val istOffset = 5 * 3600000L + 30 * 60000L // +19800000
        val countedIst = StreakCalculator.computeCountedDays(listOf(session), istOffset)
        assertTrue(countedIst.contains(101L), "Under IST, 20:00 UTC is 01:30 Day 101, so Day 101 is credited")

        // In New York (EST, UTC-5:00 = -18,000,000 ms):
        // 20:00 UTC - 5:00 = 15:00 Day 100
        val estOffset = -5 * 3600000L
        val countedEst = StreakCalculator.computeCountedDays(listOf(session), estOffset)
        assertTrue(countedEst.contains(100L), "Under EST, session counts for Day 100")
    }
}
