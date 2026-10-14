package com.focal.app.data

enum class InsightsRange(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    LIFETIME("Lifetime")
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val iconFallbackLetter: String = appName.firstOrNull()?.uppercase() ?: "A"
)

data class PairedBarData(
    val label: String,
    val focusMinutes: Int,
    val screenTimeMinutes: Int
)

data class InsightsData(
    val range: InsightsRange,
    val totalFocusMinutes: Int,
    val dailyAverageFocusMinutes: Int,
    val dailyAverageScreenTimeMinutes: Int?, // null if usage access not granted
    val blockedAttemptsCount: Int,
    val pairedBars: List<PairedBarData>,
    val topApps: List<AppUsageInfo>,
    val hasUsageAccess: Boolean
)

expect class ScreenTimeProvider() {
    fun isUsageAccessGranted(): Boolean
    fun getInsightsData(
        range: InsightsRange,
        childTodayFocusMinutes: Int,
        totalBlockedAttempts: Int
    ): InsightsData
}

object ScreenTimeRepository {
    private val provider = ScreenTimeProvider()

    fun isUsageAccessGranted(): Boolean = provider.isUsageAccessGranted()

    fun getInsightsData(
        range: InsightsRange,
        childTodayFocusMinutes: Int = 155,
        totalBlockedAttempts: Int = 14
    ): InsightsData = provider.getInsightsData(range, childTodayFocusMinutes, totalBlockedAttempts)
}
