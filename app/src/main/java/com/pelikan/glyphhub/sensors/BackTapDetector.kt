package com.pelikan.glyphhub.sensors

import kotlin.math.abs

class BackTapDetector(
    var enabled: Boolean = false,
    private val threshold: Float = 24f,
    private val cooldownMs: Long = 900L
) {
    private var lastTapAt = 0L

    fun onAccelerometer(values: FloatArray, timestampMs: Long): Boolean {
        if (!enabled || values.size < 3) return false
        val impulse = abs(values[2])
        if (impulse > threshold && timestampMs - lastTapAt > cooldownMs) {
            lastTapAt = timestampMs
            return true
        }
        return false
    }
}
