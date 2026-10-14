package com.focal.app.streak

import java.util.Calendar
import java.util.TimeZone

actual fun getDeviceTimeZoneOffsetMillis(): Long {
    return TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
}

actual fun getDeviceCurrentTime(): CurrentDeviceTime {
    val cal = Calendar.getInstance()
    val dayStr = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        Calendar.SUNDAY -> "Sun"
        else -> "Mon"
    }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return CurrentDeviceTime(
        dayOfWeek = dayStr,
        minuteOfDay = h * 60 + m,
        hour24 = h,
        minute = m,
        dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    )
}
