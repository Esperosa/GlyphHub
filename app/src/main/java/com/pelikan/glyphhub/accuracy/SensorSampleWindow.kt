package com.pelikan.glyphhub.accuracy

import kotlin.math.pow
import kotlin.math.sqrt

class SensorSampleWindow(private val capacity: Int = 12) {
    private val samples = ArrayDeque<Float>()

    val size: Int
        get() = samples.size

    fun add(value: Float) {
        if (capacity <= 0) return
        if (samples.size >= capacity) {
            samples.removeFirst()
        }
        samples.addLast(value)
    }

    fun clear() {
        samples.clear()
    }

    fun average(): Float {
        if (samples.isEmpty()) return 0f
        return samples.sum() / samples.size.toFloat()
    }

    fun standardDeviation(): Float {
        if (samples.size < 2) return 0f
        val mean = average()
        val variance = samples.sumOf { (it - mean).toDouble().pow(2.0) } / samples.size.toDouble()
        return sqrt(variance).toFloat()
    }

    fun span(): Float {
        if (samples.isEmpty()) return 0f
        return (samples.maxOrNull() ?: 0f) - (samples.minOrNull() ?: 0f)
    }
}