package com.pelikan.glyphhub.glyph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class GlyphFrameValidation(
    val litPixels: Int,
    val outOfMaskPixels: Int,
    val isolatedPixels: Int,
    val accidentalFullPanel: Boolean
)

object GlyphDesignSystem {
    const val size = GlyphFrame.MATRIX_SIZE
    const val center = 6.0
    const val fullRingRadius = 5.45
    const val fullNeedleLength = 5.55

    val circularMask13: Set<Pair<Int, Int>> = buildMask(5.05)
    val softCircularMask13: Set<Pair<Int, Int>> = buildMask(5.75)
    val innerAreaMask: Set<Pair<Int, Int>> = buildMask(4.1)
    val edgeRingMask: Set<Pair<Int, Int>> = softCircularMask13 - innerAreaMask
    val centerSafeArea: Set<Pair<Int, Int>> = buildMask(2.25)

    fun empty(brightness: Int = 80): GlyphFrame = GlyphFrame.empty13(brightness)

    fun drawPixelSafe(
        frame: GlyphFrame,
        x: Int,
        y: Int,
        intensity: Int = 100,
        mask: Set<Pair<Int, Int>> = softCircularMask13
    ): GlyphFrame {
        if ((x to y) !in mask) return frame
        return frame.withPixelBrightness(x, y, intensity)
    }

    fun drawDot(frame: GlyphFrame, x: Int, y: Int, intensity: Int = 100): GlyphFrame {
        var output = frame
        val points = listOf(
            x to y,
            (x - 1) to y,
            (x + 1) to y,
            x to (y - 1),
            x to (y + 1)
        )
        points.forEach { (px, py) ->
            output = drawPixelSafe(output, px, py, intensity)
        }
        return output
    }

    fun drawSoftDot(frame: GlyphFrame, x: Int, y: Int, intensity: Int = 100): GlyphFrame {
        var output = frame
        output = drawPixelSafe(output, x, y, intensity)
        listOf(
            (x - 1) to y,
            (x + 1) to y,
            x to (y - 1),
            x to (y + 1)
        ).forEach { (px, py) ->
            output = drawPixelSafe(output, px, py, (intensity * 0.68f).roundToInt())
        }
        listOf(
            (x - 1) to (y - 1),
            (x - 1) to (y + 1),
            (x + 1) to (y - 1),
            (x + 1) to (y + 1)
        ).forEach { (px, py) ->
            output = drawPixelSafe(output, px, py, (intensity * 0.38f).roundToInt())
        }
        return output
    }

    fun drawCircleApprox(
        frame: GlyphFrame,
        radius: Double,
        intensity: Int = 100,
        thickness: Double = 0.55,
        mask: Set<Pair<Int, Int>> = softCircularMask13
    ): GlyphFrame {
        var output = frame
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((x to y) !in mask) continue
                val distance = distanceFromCenter(x, y)
                if (abs(distance - radius) <= thickness) {
                    output = output.withPixelBrightness(x, y, intensity)
                }
            }
        }
        return output
    }

    fun drawRing(
        frame: GlyphFrame,
        innerRadius: Double,
        outerRadius: Double,
        intensity: Int = 100,
        mask: Set<Pair<Int, Int>> = softCircularMask13
    ): GlyphFrame {
        var output = frame
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((x to y) !in mask) continue
                val distance = distanceFromCenter(x, y)
                if (distance in innerRadius..outerRadius) {
                    output = output.withPixelBrightness(x, y, intensity)
                }
            }
        }
        return output
    }

    fun drawArcApprox(
        frame: GlyphFrame,
        radius: Double,
        startDegrees: Double,
        sweepDegrees: Double,
        intensity: Int = 100,
        thickness: Double = 0.75
    ): GlyphFrame {
        var output = frame
        for (y in 0 until size) {
            for (x in 0 until size) {
                if ((x to y) !in softCircularMask13) continue
                val distance = distanceFromCenter(x, y)
                if (abs(distance - radius) > thickness) continue
                val angle = angleForPoint(x, y)
                if (angleWithinSweep(angle, startDegrees, sweepDegrees)) {
                    output = output.withPixelBrightness(x, y, intensity)
                }
            }
        }
        return output
    }

    fun drawNeedle(
        frame: GlyphFrame,
        angleDegrees: Double,
        length: Double,
        intensity: Int = 100,
        thickness: Double = 0.55
    ): GlyphFrame {
        var output = frame
        val radians = Math.toRadians(angleDegrees - 90.0)
        val targetX = center + cos(radians) * length
        val targetY = center + sin(radians) * length
        val steps = (length * 3.0).roundToInt().coerceAtLeast(1)
        for (step in 0..steps) {
            val ratio = step.toDouble() / steps.toDouble()
            val px = center + (targetX - center) * ratio
            val py = center + (targetY - center) * ratio
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if ((x to y) !in softCircularMask13) continue
                    val distance = hypot(x - px, y - py)
                    if (distance <= thickness) {
                        output = output.withPixelBrightness(x, y, intensity)
                    }
                }
            }
        }
        return output
    }

    fun drawGauge(frame: GlyphFrame, progress: Float, intensity: Int = 100): GlyphFrame {
        val clamped = progress.coerceIn(0f, 1f)
        return drawArcApprox(frame, radius = fullRingRadius, startDegrees = -135.0, sweepDegrees = 270.0 * clamped, intensity = intensity)
    }

    fun drawProgressRing(frame: GlyphFrame, progress: Float, intensity: Int = 100): GlyphFrame {
        val clamped = progress.coerceIn(0f, 1f)
        return drawArcApprox(frame, radius = fullRingRadius, startDegrees = -90.0, sweepDegrees = 360.0 * clamped, intensity = intensity)
    }

    fun drawCenteredIcon(rows: List<String>, brightness: Int, removeStrays: Boolean = true): GlyphFrame {
        val icon = GlyphFrame.fromBinaryRows(rows, brightness)
        val offsetX = ((size - icon.width) / 2).coerceAtLeast(0)
        val offsetY = ((size - icon.height) / 2).coerceAtLeast(0)
        val frame = empty(brightness).overlay(icon, offsetX, offsetY)
        return clean(frame, removeStrays = removeStrays)
    }

    fun removeStrayPixels(frame: GlyphFrame, minNeighbors: Int = 1): GlyphFrame {
        var output = GlyphFrame.empty13(frame.brightness)
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val intensity = frame.intensityAt(x, y)
                if (intensity <= 0) continue
                val neighbors = surroundingPoints(x, y).count { (px, py) -> frame.intensityAt(px, py) > 0 }
                if (neighbors >= minNeighbors || (x to y) in centerSafeArea) {
                    output = output.withPixelBrightness(x, y, intensity)
                }
            }
        }
        return output
    }

    fun validateAgainstCircularMask(frame: GlyphFrame): GlyphFrameValidation {
        var outOfMask = 0
        var isolated = 0
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (frame.intensityAt(x, y) <= 0) continue
                if ((x to y) !in softCircularMask13) {
                    outOfMask += 1
                }
                if (surroundingPoints(x, y).none { (px, py) -> frame.intensityAt(px, py) > 0 }) {
                    isolated += 1
                }
            }
        }
        return GlyphFrameValidation(
            litPixels = frame.litPixelCount(),
            outOfMaskPixels = outOfMask,
            isolatedPixels = isolated,
            accidentalFullPanel = frame.litPixelCount() >= softCircularMask13.size - 2
        )
    }

    fun applyMask(frame: GlyphFrame, mask: Set<Pair<Int, Int>> = softCircularMask13): GlyphFrame {
        var output = GlyphFrame.empty13(frame.brightness)
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val intensity = frame.intensityAt(x, y)
                if (intensity > 0 && (x to y) in mask) {
                    output = output.withPixelBrightness(x, y, intensity)
                }
            }
        }
        return output
    }

    fun clean(frame: GlyphFrame, removeStrays: Boolean = true): GlyphFrame {
        val masked = applyMask(frame)
        return if (removeStrays) removeStrayPixels(masked) else masked
    }

    private fun buildMask(radius: Double): Set<Pair<Int, Int>> {
        val mask = mutableSetOf<Pair<Int, Int>>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (distanceFromCenter(x, y) <= radius) {
                    mask.add(x to y)
                }
            }
        }
        return mask
    }

    private fun distanceFromCenter(x: Int, y: Int): Double =
        hypot(x - center, y - center)

    private fun surroundingPoints(x: Int, y: Int): List<Pair<Int, Int>> = buildList {
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                add((x + dx) to (y + dy))
            }
        }
    }

    private fun angleForPoint(x: Int, y: Int): Double {
        val radians = atan2(y - center, x - center)
        val degrees = Math.toDegrees(radians) + 90.0
        return if (degrees < 0.0) degrees + 360.0 else degrees
    }

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
