package com.pelikan.glyphhub.accuracy

object SmoothingFilters {
    fun lowPass(current: Float, target: Float, factor: Float): Float {
        val clampedFactor = factor.coerceIn(0f, 1f)
        return current + (target - current) * clampedFactor
    }

    fun lowPassDegrees(current: Float, target: Float, factor: Float): Float {
        val delta = ((target - current + 540f) % 360f) - 180f
        return normalizeDegrees(current + delta * factor.coerceIn(0f, 1f))
    }

    fun normalizeDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f
}