package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generic 13x13 visuals adapted from MIT community projects reviewed in
 * docs/THIRD_PARTY_NOTICES.md. The implementation is rewritten for GlyphHub's
 * GlyphFrame pipeline and physical circular LED mask.
 */
object CommunityGlyphVisuals {
    fun fullHeart(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000000000000",
            "0001100011000",
            "0011110111100",
            "0111111111110",
            "1111111111111",
            "1111111111111",
            "0111111111110",
            "0011111111100",
            "0001111111000",
            "0000111110000",
            "0000011100000",
            "0000001000000",
            "0000000000000"
        )

    fun heartbeatFrame(tick: Int, brightness: Int): GlyphFrame {
        val phase = sin(tick * 0.18).let { (it + 1.0) * 0.5 }
        val scale = 4.05 + phase * 0.28
        var frame = GlyphFrame.empty13(brightness)
        for (dy in -6..6) {
            for (dx in -6..6) {
                val nx = dx / scale
                val ny = -dy / scale
                val equation = Math.pow(nx * nx + ny * ny - 1.0, 3.0) - nx * nx * ny * ny * ny
                if (equation <= 0.0) {
                    frame = GlyphDesignSystem.drawPixelSafe(frame, GlyphDesignSystem.center.roundToInt() + dx, GlyphDesignSystem.center.roundToInt() + dy, 100)
                }
            }
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun ringsFrame(tick: Int, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        for (index in 0 until 3) {
            val radius = ((tick * 0.55 + index * 2.15) % 6.0)
            if (radius >= 0.85) {
                val intensity = (100 - index * 18).coerceIn(42, 100)
                frame = GlyphDesignSystem.drawCircleApprox(frame, radius, intensity, thickness = 0.48)
            }
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun equalizerFrame(tick: Int, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        val startX = 2
        for (bar in 0 until 5) {
            val x = startX + bar * 2
            val wave = abs(sin(tick * 0.20 + bar * 0.9))
            val height = (2 + wave * 9).roundToInt().coerceIn(2, 11)
            for (y in (12 - height)..11) {
                frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 100)
            }
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun fireFrame(tick: Int, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        for (y in 2..11) {
            for (x in 1..11) {
                val distance = hypot((x - 6).toDouble(), ((y - 9) * 0.82).toDouble())
                val envelope = 1.0 - (distance / 6.2)
                val wave = sin(x * 0.72 + tick * 0.28) + sin(y * 0.58 - tick * 0.22)
                val threshold = 0.34 + (11 - y) * 0.035
                if (envelope + wave * 0.16 > threshold) {
                    val intensity = (58 + envelope * 42 + wave * 8).roundToInt().coerceIn(42, 100)
                    frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, intensity)
                }
            }
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun spinnerFrame(tick: Int, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        val angle = (tick * 18.0) % 360.0
        frame = GlyphDesignSystem.drawNeedle(frame, angle, length = GlyphDesignSystem.fullNeedleLength, intensity = 100, thickness = 0.42)
        frame = GlyphDesignSystem.drawCircleApprox(frame, GlyphDesignSystem.fullRingRadius, 34, thickness = 0.4)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 100)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun starsFrame(tick: Int, brightness: Int): GlyphFrame {
        val seededPoints = listOf(
            6 to 1,
            9 to 2,
            3 to 3,
            11 to 5,
            1 to 6,
            8 to 7,
            4 to 9,
            10 to 10,
            6 to 11
        )
        var frame = GlyphFrame.empty13(brightness)
        seededPoints.forEachIndexed { index, point ->
            val intensity = if ((tick + index * 2) % 7 < 3) 100 else 52
            frame = GlyphDesignSystem.drawPixelSafe(frame, point.first, point.second, intensity)
        }
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 84)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun rocketFrame(tick: Int, brightness: Int): GlyphFrame {
        val y = 10 - (tick % 10)
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, y, 100)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 5, y + 1, 100)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, y + 1, 100)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 7, y + 1, 100)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 5, y + 2, 82)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 7, y + 2, 82)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, y + 3, if (tick % 2 == 0) 96 else 58)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun timerRing(progress: Float, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawProgressRing(frame, progress, 100)
        val angle = progress.coerceIn(0f, 1f) * 2.0 * PI - PI / 2.0
        val x = (6.0 + cos(angle) * 5.45).roundToInt()
        val y = (6.0 + sin(angle) * 5.45).roundToInt()
        frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 100)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    fun animatedPresetFrame(id: String, elapsedMs: Long, frameDurationMs: Long, brightness: Int): GlyphFrame? {
        val tick = (elapsedMs / frameDurationMs.coerceAtLeast(40L)).toInt()
        return when (id) {
            "community_heartbeat" -> heartbeatFrame(tick, brightness)
            "community_rings" -> ringsFrame(tick, brightness)
            "community_equalizer" -> equalizerFrame(tick, brightness)
            "community_fire" -> fireFrame(tick, brightness)
            "community_spinner" -> spinnerFrame(tick, brightness)
            "community_stars" -> starsFrame(tick, brightness)
            "community_rocket" -> rocketFrame(tick, brightness)
            else -> null
        }
    }

    private fun rows(brightness: Int, vararg rows: String): GlyphFrame =
        GlyphMatrixLayout.mask(GlyphFrame.fromBinaryRows(rows.toList(), brightness.coerceIn(0, 100)))
}
