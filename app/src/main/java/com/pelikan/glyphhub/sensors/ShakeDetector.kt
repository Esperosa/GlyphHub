package com.pelikan.glyphhub.sensors

import kotlin.math.sqrt

class ShakeDetector(
    private val threshold: Float = 18f,
    private val cooldownMs: Long = 650L
) {
    private var lastShakeAt = 0L

    fun onAccelerometer(values: FloatArray, timestampMs: Long): Float? {
        if (values.size < 3) return null
        val force = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        if (force >= threshold && timestampMs - lastShakeAt > cooldownMs) {
            lastShakeAt = timestampMs
            return force
        }
        return null
    }
}
