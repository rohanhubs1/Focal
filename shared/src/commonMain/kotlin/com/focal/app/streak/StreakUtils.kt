package com.focal.app.streak

/**
 * Returns current device timezone offset in milliseconds from UTC.
 */
expect fun getDeviceTimeZoneOffsetMillis(): Long

/**
 * Returns real system epoch time in milliseconds.
 */
expect fun getDeviceCurrentTimeMillis(): Long

/**
 * Formats epoch milliseconds in device local timezone as "h:mm a".
 */
expect fun formatTimeLocal(epochMillis: Long): String

/**
 * Returns monotonic elapsed realtime in milliseconds (SystemClock.elapsedRealtime()).
 */
expect fun getDeviceElapsedRealtime(): Long

data class CurrentDeviceTime(
    val dayOfWeek: String, // "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    val minuteOfDay: Int,  // 0..1439
    val hour24: Int,
    val minute: Int,
    val dayOfYear: Int = 1
)

expect fun getDeviceCurrentTime(): CurrentDeviceTime

