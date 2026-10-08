package com.focal.app.model

import kotlin.random.Random

private var simTime = 1727200000000L

fun currentTimeMillis(): Long {
    simTime += Random.nextLong(10, 100)
    return simTime
}
