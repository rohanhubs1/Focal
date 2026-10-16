package com.focal.app.model

import com.focal.app.streak.getDeviceCurrentTime
import com.focal.app.streak.getDeviceCurrentTimeMillis

fun currentTimeMillis(): Long {
    return getDeviceCurrentTimeMillis()
}

fun getLocalGreeting(): String {
    val hour = try {
        getDeviceCurrentTime().hour24
    } catch (_: Throwable) {
        10
    }
    return when (hour) {
        in 4..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

fun generateUuid(): String {
    val chars = "0123456789abcdef"
    val sb = StringBuilder()
    for (i in 0 until 36) {
        when (i) {
            8, 13, 18, 23 -> sb.append('-')
            14 -> sb.append('4')
            19 -> {
                val r = (0..15).random()
                sb.append(chars[(r and 0x3) or 0x8])
            }
            else -> sb.append(chars[(0..15).random()])
        }
    }
    return sb.toString()
}
