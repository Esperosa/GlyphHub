package com.pelikan.glyphhub.glyph.validation

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

object GlyphCircularLayoutEngine {
    const val size: Int = GlyphFrame.MATRIX_SIZE
    const val centerX: Int = 6
    const val centerY: Int = 6
    const val fullRadius: Double = 5.55

    val centerPoint: Pair<Int, Int> = centerX to centerY

    fun isInSafeCircle(x: Int, y: Int): Boolean =
        GlyphMatrixLayout.isPhysicalLed(x, y)

    fun radius(x: Int, y: Int): Double =
        hypot(x - centerX.toDouble(), y - centerY.toDouble())

    fun angleDegrees(x: Int, y: Int): Double {
        val degrees = Math.toDegrees(atan2(y - centerY.toDouble(), x - centerX.toDouble())) + 90.0
        return normalizeDegrees(degrees)
    }

    fun pointAt(angleDegrees: Double, radius: Double = fullRadius): Pair<Int, Int> {
        val radians = Math.toRadians(angleDegrees - 90.0)
        val x = (centerX + cos(radians) * radius).roundToInt().coerceIn(0, size - 1)
        val y = (centerY + sin(radians) * radius).roundToInt().coerceIn(0, size - 1)
        return x to y
    }

    fun eightDirectionPoints(radius: Double = fullRadius): Map<String, Pair<Int, Int>> =
        linkedMapOf(
            "N" to pointAt(0.0, radius),
            "NE" to pointAt(45.0, radius),
            "E" to pointAt(90.0, radius),
            "SE" to pointAt(135.0, radius),
            "S" to pointAt(180.0, radius),
            "SW" to pointAt(225.0, radius),
            "W" to pointAt(270.0, radius),
            "NW" to pointAt(315.0, radius)
        )

    fun clockPositions(radius: Double = fullRadius): Map<Int, Pair<Int, Int>> =
        (1..12).associateWith { hour -> pointAt(hour * 30.0, radius) }

    fun ringPoints(radius: Double, thickness: Double = 0.55): Set<Pair<Int, Int>> =
        buildSet {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (isInSafeCircle(x, y) && kotlin.math.abs(radius(x, y) - radius) <= thickness) {
                        add(x to y)
                    }
                }
            }
        }

    fun orbitPoints(count: Int, radius: Double = fullRadius, startDegrees: Double = 0.0): List<Pair<Int, Int>> {
        if (count <= 0) return emptyList()
        return (0 until count).map { index ->
            pointAt(startDegrees + 360.0 * index / count.toDouble(), radius)
        }.distinct()
    }

    fun progressRingPositions(progress: Float, radius: Double = fullRadius): Set<Pair<Int, Int>> {
        val clamped = progress.coerceIn(0f, 1f)
        return arcPoints(startDegrees = -90.0, sweepDegrees = 360.0 * clamped, radius = radius)
    }

    fun arcPoints(
        startDegrees: Double,
        sweepDegrees: Double,
        radius: Double = fullRadius,
        thickness: Double = 0.75
    ): Set<Pair<Int, Int>> =
        buildSet {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (!isInSafeCircle(x, y)) continue
                    if (kotlin.math.abs(radius(x, y) - radius) > thickness) continue
                    if (angleWithinSweep(angleDegrees(x, y), startDegrees, sweepDegrees)) {
                        add(x to y)
                    }
                }
            }
        }

    fun needlePoints(angleDegrees: Double, length: Double = fullRadius, thickness: Double = 0.5): Set<Pair<Int, Int>> {
        val radians = Math.toRadians(angleDegrees - 90.0)
        val targetX = centerX + cos(radians) * length
        val targetY = centerY + sin(radians) * length
        val steps = (length * 3.0).roundToInt().coerceAtLeast(1)
        return buildSet {
            for (step in 0..steps) {
                val ratio = step.toDouble() / steps.toDouble()
                val px = centerX + (targetX - centerX) * ratio
                val py = centerY + (targetY - centerY) * ratio
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        if (isInSafeCircle(x, y) && hypot(x - px, y - py) <= thickness) add(x to y)
                    }
                }
            }
        }
    }

    fun circularFill(radius: Double): Set<Pair<Int, Int>> =
        buildSet {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (isInSafeCircle(x, y) && radius(x, y) <= radius) add(x to y)
                }
            }
        }

    fun drawPoints(frame: GlyphFrame, points: Iterable<Pair<Int, Int>>, intensity: Int = 100): GlyphFrame {
        var output = frame
        points.forEach { (x, y) ->
            if (isInSafeCircle(x, y)) output = output.withPixelBrightness(x, y, intensity)
        }
        return output
    }

    fun drawNeedle(frame: GlyphFrame, angleDegrees: Double, length: Double = fullRadius, intensity: Int = 100): GlyphFrame =
        drawPoints(frame, needlePoints(angleDegrees, length), intensity)

    fun drawProgressRing(frame: GlyphFrame, progress: Float, intensity: Int = 100): GlyphFrame =
        drawPoints(frame, progressRingPositions(progress), intensity)

    private fun angleWithinSweep(angle: Double, startDegrees: Double, sweepDegrees: Double): Boolean {
        val normalizedAngle = normalizeDegrees(angle)
        val normalizedStart = normalizeDegrees(startDegrees)
        val normalizedEnd = normalizeDegrees(startDegrees + sweepDegrees)
        return if (sweepDegrees >= 0.0) {
            if (normalizedStart <= normalizedEnd) {
                normalizedAngle in normalizedStart..normalizedEnd
            } else {
                normalizedAngle >= normalizedStart || normalizedAngle <= normalizedEnd
            }
        } else {
            if (normalizedEnd <= normalizedStart) {
                normalizedAngle in normalizedEnd..normalizedStart
            } else {
                normalizedAngle >= normalizedEnd || normalizedAngle <= normalizedStart
            }
        }
    }

    private fun normalizeDegrees(value: Double): Double {
        var result = value % 360.0
        if (result < 0.0) result += 360.0
        return result
    }
}
