package com.focal.app.model

import kotlin.random.Random
import com.focal.app.streak.getDeviceCurrentTime

private var simTime = 1727200000000L

fun currentTimeMillis(): Long {
    simTime += Random.nextLong(10, 100)
    return simTime
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
