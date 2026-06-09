package com.pelikan.glyphhub.glyph.assets

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import com.pelikan.glyphhub.glyph.validation.GlyphCircularLayoutEngine
import com.pelikan.glyphhub.glyph.validation.GlyphFrameNormalizer

object GlyphIconLibrary {
    fun dice(face: Int, brightness: Int = 80): GlyphFrame {
        val pips = when (face.coerceIn(1, 6)) {
            1 -> setOf(6 to 6)
            2 -> setOf(4 to 4, 8 to 8)
            3 -> setOf(4 to 4, 6 to 6, 8 to 8)
            4 -> setOf(4 to 4, 8 to 4, 4 to 8, 8 to 8)
            5 -> setOf(4 to 4, 8 to 4, 6 to 6, 4 to 8, 8 to 8)
            else -> setOf(4 to 4, 8 to 4, 4 to 6, 8 to 6, 4 to 8, 8 to 8)
        }
        var frame = roundedSquare(brightness)
        pips.forEach { (x, y) -> frame = frame.withPixelBrightness(x, y, 100) }
        return GlyphFrameNormalizer.normalize(frame)
    }

    fun coin(label: String, brightness: Int = 80): GlyphFrame {
        var frame = ring(brightness, intensity = 100)
        frame = frame.overlay(GlyphTextRenderer.drawCenteredText3x5(label.take(3), brightness, yOffset = 4))
        return GlyphFrameNormalizer.normalize(frame)
    }

    fun coinEdge(brightness: Int = 80): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        (1..11).forEach { y -> frame = frame.withPixelBrightness(6, y, 100) }
        frame = frame.withPixelBrightness(5, 3, 45)
        frame = frame.withPixelBrightness(7, 9, 45)
        return GlyphFrameNormalizer.normalize(frame)
    }

    fun battery(
        level: Int,
        brightness: Int = 100,
        charging: Boolean = false,
        elapsedMs: Long = 0L,
        lowWarning: Boolean = false,
        animateCharging: Boolean = true
    ): GlyphFrame {
        val clampedLevel = level.coerceIn(0, 100)
        val frameBrightness = BATTERY_FRAME_BRIGHTNESS
        val fillIntensity = if (clampedLevel >= BATTERY_FULL_LEVEL) BATTERY_OUTLINE_INTENSITY else BATTERY_FILL_INTENSITY
        val litRows = if (clampedLevel <= 0) {
            0
        } else {
            ((clampedLevel * BATTERY_FILL_ROWS.size) / 100).coerceIn(1, BATTERY_FILL_ROWS.size)
        }
        val pulseIntensity = batteryPulseIntensity(elapsedMs)
        var frame = GlyphFrame.empty13(frameBrightness)

        BATTERY_OUTLINE.forEach { (x, y) ->
            frame = frame.withPixelBrightness(x, y, batteryOutlineIntensity(y))
        }

        BATTERY_FILL_ROWS.take(litRows).forEachIndexed { index, y ->
            val topLitRow = index == litRows - 1
            val rowIntensity = if (animateCharging && lowWarning && clampedLevel <= 15 && topLitRow) {
                pulseIntensity
            } else {
                fillIntensity
            }
            frame = drawBatteryFillRow(frame, y, rowIntensity)
        }

        if (animateCharging && charging && clampedLevel in 1..94) {
            val nextRow = BATTERY_FILL_ROWS.getOrNull(litRows)
            if (nextRow != null) frame = drawBatteryFillRow(frame, nextRow, pulseIntensity)
        }

        return GlyphFrameNormalizer.normalize(frame)
    }

    fun compassNeedle(degrees: Float, brightness: Int = 80): GlyphFrame {
        var frame = ring(brightness, intensity = 34)
        frame = GlyphCircularLayoutEngine.drawNeedle(frame, degrees.toDouble(), intensity = 100)
        frame = frame.withPixelBrightness(6, 6, 72)
        return GlyphFrameNormalizer.normalize(frame)
    }

    fun eye(brightness: Int = 80): GlyphFrame =
        GlyphDesignSystem.drawCenteredIcon(
            listOf(
                "0111110",
                "1100011",
                "1001001",
                "1011101",
                "1001001",
                "1100011",
                "0111110"
            ),
            brightness,
            removeStrays = false
        )

    fun text(label: String, brightness: Int = 80): GlyphFrame =
        GlyphTextRenderer.drawCenteredText3x5(label.take(3), brightness)

    private fun ring(brightness: Int, intensity: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphCircularLayoutEngine.drawPoints(frame, GlyphCircularLayoutEngine.ringPoints(5.45, 0.55), intensity)
        return frame
    }

    private fun roundedSquare(brightness: Int): GlyphFrame =
        GlyphFrame.fromBinaryRows(
            listOf(
                "0000000000000",
                "0000000000000",
                "0011111111100",
                "0010000000100",
                "0010000000100",
                "0010000000100",
                "0010000000100",
                "0010000000100",
                "0010000000100",
                "0010000000100",
                "0011111111100",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        )

    private fun drawBatteryFillRow(frame: GlyphFrame, y: Int, intensity: Int): GlyphFrame {
        var output = frame
        BATTERY_FILL_COLUMNS.forEach { x -> output = output.withPixelBrightness(x, y, intensity) }
        return output
    }

    private fun batteryOutlineIntensity(y: Int): Int =
        when {
            y <= 1 -> 92
            y <= 4 -> 96
            else -> BATTERY_OUTLINE_INTENSITY
        }

    private fun batteryPulseIntensity(elapsedMs: Long): Int =
        BATTERY_PULSE_STEPS[((elapsedMs / BATTERY_PULSE_STEP_MS) % BATTERY_PULSE_STEPS.size).toInt()]

    private val BATTERY_OUTLINE = buildSet {
        add(5 to 0)
        add(6 to 0)
        add(7 to 0)
        for (x in 3..9) {
            add(x to 1)
            add(x to 11)
        }
        for (y in 2..10) {
            add(3 to y)
            add(9 to y)
        }
    }

    private val BATTERY_FILL_ROWS = listOf(10, 9, 8, 7, 6, 5, 4, 3, 2)
    private val BATTERY_FILL_COLUMNS = 4..8
    private const val BATTERY_FRAME_BRIGHTNESS = 100
    private const val BATTERY_OUTLINE_INTENSITY = 100
    private const val BATTERY_FILL_INTENSITY = 56
    private const val BATTERY_FULL_LEVEL = 95
    private const val BATTERY_PULSE_MIN_INTENSITY = 3
    private const val BATTERY_PULSE_STEP_MS = 33L
    private val BATTERY_PULSE_STEPS = IntArray(90) { index ->
        val phase = index.toDouble() / 90.0
        val eased = (1.0 - kotlin.math.cos(phase * kotlin.math.PI * 2.0)) / 2.0
        (BATTERY_PULSE_MIN_INTENSITY + eased * (BATTERY_FILL_INTENSITY - BATTERY_PULSE_MIN_INTENSITY))
            .toInt()
            .coerceIn(BATTERY_PULSE_MIN_INTENSITY, BATTERY_FILL_INTENSITY)
    }
}
