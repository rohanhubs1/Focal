package com.focal.app.data

actual class ScreenTimeProvider actual constructor() {

    actual fun isUsageAccessGranted(): Boolean {
        return false
    }

    actual fun getInsightsData(
        range: InsightsRange,
        childTodayFocusMinutes: Int,
        totalBlockedAttempts: Int
    ): InsightsData {
        val rangeFocusMinutes = when (range) {
            InsightsRange.WEEK -> (childTodayFocusMinutes * 4.8f).toInt().coerceAtLeast(120)
            InsightsRange.MONTH -> (childTodayFocusMinutes * 18f).toInt().coerceAtLeast(600)
            InsightsRange.LIFETIME -> (childTodayFocusMinutes * 120f).toInt().coerceAtLeast(3600)
        }
        val dailyAvgFocusMinutes = (rangeFocusMinutes / (if (range == InsightsRange.WEEK) 7 else if (range == InsightsRange.MONTH) 30 else 180)).coerceAtLeast(60)

        val weekLabels = listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon")
        val pairedBars = weekLabels.map { label ->
            PairedBarData(label, childTodayFocusMinutes, 0)
        }

        return InsightsData(
            range = range,
            totalFocusMinutes = rangeFocusMinutes,
            dailyAverageFocusMinutes = dailyAvgFocusMinutes,
            dailyAverageScreenTimeMinutes = null,
            blockedAttemptsCount = totalBlockedAttempts,
            pairedBars = pairedBars,
            topApps = emptyList(),
            hasUsageAccess = false
        )
    }
}
