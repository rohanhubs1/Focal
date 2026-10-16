package com.focal.app.streak

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun getDeviceTimeZoneOffsetMillis(): Long {
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate()) * 1000L
}

private val iosMonotonicTimeSource = kotlin.time.TimeSource.Monotonic
private val iosMonotonicStartMark = iosMonotonicTimeSource.markNow()

actual fun getDeviceCurrentTimeMillis(): Long {
    // NSTimeIntervalSince1970 = 978307200.0 (seconds from Jan 1 1970 to Jan 1 2001)
    val secondsSince1970 = NSDate().timeIntervalSinceReferenceDate + 978307200.0
    return (secondsSince1970 * 1000.0).toLong()
}

actual fun formatTimeLocal(epochMillis: Long): String {
    val date = NSDate(epochMillis / 1000.0)
    val formatter = platform.Foundation.NSDateFormatter().apply {
        dateFormat = "h:mm a"
    }
    return formatter.stringFromDate(date)
}

actual fun getDeviceElapsedRealtime(): Long {
    return iosMonotonicStartMark.elapsedNow().inWholeMilliseconds
}

actual fun getDeviceCurrentTime(): CurrentDeviceTime {
    return CurrentDeviceTime(
        dayOfWeek = "Mon",
        minuteOfDay = 10 * 60 + 9, // 10:09 AM
        hour24 = 10,
        minute = 9,
        dayOfYear = 286
    )
}
