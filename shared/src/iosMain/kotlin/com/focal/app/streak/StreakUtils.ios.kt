package com.focal.app.streak

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun getDeviceTimeZoneOffsetMillis(): Long {
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate()) * 1000L
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
