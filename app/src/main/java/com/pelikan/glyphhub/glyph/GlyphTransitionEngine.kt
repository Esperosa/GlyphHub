package com.pelikan.glyphhub.glyph

import kotlin.math.abs
import kotlin.math.roundToInt

object GlyphTransitionEngine {
    val transitionIds = listOf(
        "appear_from_center",
        "disappear_to_center",
        "scanline_in",
        "scanline_out",
        "pixel_burst_in",
        "pixel_burst_out",
        "fade_in",
        "fade_out",
        "glitch_in",
        "glitch_out"
    )

    fun animation(id: String, brightness: Int = 80): GlyphAnimation {
        val frames = when (id) {
            "disappear_to_center" -> appearFromCenter(brightness).reversed()
            "scanline_in" -> scanline(brightness, inward = true)
            "scanline_out" -> scanline(brightness, inward = false)
            "pixel_burst_in" -> burst(brightness, inward = true)
            "pixel_burst_out" -> burst(brightness, inward = false)
            "fade_in" -> fade(brightness, inward = true)
            "fade_out" -> fade(brightness, inward = false)
            "glitch_in" -> glitch(brightness, inward = true)
            "glitch_out" -> glitch(brightness, inward = false)
            else -> appearFromCenter(brightness)
        }
        return GlyphAnimation(
            id = id,
            name = id,
            loop = false,
            frames = frames.map { GlyphAnimationFrame(it, 72L) }
        )
    }

    private fun appearFromCenter(brightness: Int): List<GlyphFrame> {
        return listOf(
            GlyphDesignSystem.drawSoftDot(GlyphFrame.empty13(brightness), 6, 6, 95),
            GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 0.8, 1.8, 78),
            GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 1.5, 2.8, 84),
            GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 2.4, 3.7, 88),
            GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 3.4, 4.7, 92),
            GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 96, thickness = 0.9),
            GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 4.1, 5.75, 82)
        ).map { GlyphDesignSystem.clean(it) }
    }

    private fun scanline(brightness: Int, inward: Boolean): List<GlyphFrame> {
        val rowBands = listOf(
            listOf(0, 12),
            listOf(1, 11),
            listOf(2, 10),
            listOf(3, 9),
            listOf(4, 8),
            listOf(5, 7),
            listOf(6)
        )
        val sequence = if (inward) rowBands else rowBands.reversed()
        return sequence.map { activeRows ->
            var frame = GlyphFrame.empty13(brightness)
            for (y in 0 until GlyphFrame.MATRIX_SIZE) {
                val distanceToBand = activeRows.minOf { activeRow -> abs(y - activeRow) }
                if (distanceToBand > 1) continue
                for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                    if ((x to y) !in GlyphDesignSystem.softCircularMask13) continue
                    val intensity = if (distanceToBand == 0) 92 else 56
                    frame = frame.withPixelBrightness(x, y, intensity)
                }
            }
            GlyphDesignSystem.clean(frame, removeStrays = false)
        }
    }

    private fun burst(brightness: Int, inward: Boolean): List<GlyphFrame> {
        val radii = listOf(0.9, 1.6, 2.3, 3.0, 3.7, 4.4, 5.05, 5.55)
        val sequence = if (inward) radii else radii.reversed()
        return sequence.mapIndexed { index, radius ->
            var frame = GlyphFrame.empty13(brightness)
            frame = if (radius <= 1.0) {
                GlyphDesignSystem.drawSoftDot(frame, 6, 6, 96)
            } else {
                GlyphDesignSystem.drawCircleApprox(frame, radius, 90, thickness = 0.8)
            }
            val accent = when (index % 4) {
                0 -> listOf(6 to 1, 11 to 6, 6 to 11, 1 to 6)
                1 -> listOf(9 to 3, 10 to 9, 3 to 9, 2 to 3)
                2 -> listOf(8 to 2, 10 to 7, 4 to 10, 2 to 5)
                else -> listOf(7 to 2, 10 to 5, 5 to 10, 2 to 7)
            }
            accent.forEach { (x, y) ->
                frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 64)
            }
            GlyphDesignSystem.clean(frame, removeStrays = false)
        }
    }

    private fun fade(brightness: Int, inward: Boolean): List<GlyphFrame> {
        val intensities = if (inward) {
            listOf(24, 34, 44, 54, 64, 74, 84, 94)
        } else {
            listOf(94, 84, 74, 64, 54, 44, 34, 24)
        }
        return intensities.mapIndexed { index, intensity ->
            var frame = GlyphFrame.empty13(brightness)
            frame = GlyphDesignSystem.drawCircleApprox(
                frame,
                radius = GlyphDesignSystem.fullRingRadius,
                intensity = intensity,
                thickness = 0.55
            )
            if (index >= 1) {
                frame = GlyphDesignSystem.drawCircleApprox(frame, radius = 3.55, intensity = (intensity * 0.78f).roundToInt(), thickness = 0.45)
            }
            if (index >= 3) {
                frame = GlyphDesignSystem.drawSoftDot(frame, 6, 6, (intensity * 0.92f).roundToInt())
            }
            GlyphDesignSystem.clean(frame, removeStrays = false)
        }
    }

    private fun glitch(brightness: Int, inward: Boolean): List<GlyphFrame> {
        val clusters = listOf(
            listOf(2 to 4, 3 to 4, 4 to 5, 5 to 5, 6 to 6, 7 to 6, 8 to 7),
            listOf(5 to 2, 6 to 2, 7 to 3, 8 to 3, 9 to 4, 10 to 4),
            listOf(2 to 8, 3 to 8, 4 to 7, 5 to 7, 6 to 6, 7 to 6),
            listOf(7 to 9, 8 to 9, 9 to 8, 10 to 8, 6 to 7, 5 to 7),
            listOf(3 to 3, 4 to 3, 5 to 4, 6 to 4, 7 to 5, 8 to 5),
            listOf(1 to 6, 2 to 6, 4 to 6, 5 to 6, 7 to 6, 8 to 6, 10 to 6, 11 to 6)
        )
        val sequence = if (inward) clusters else clusters.reversed()
        return sequence.mapIndexed { index, points ->
            var frame = GlyphFrame.empty13(brightness)
            points.forEach { (x, y) ->
                frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, if (index % 2 == 0) 84 else 60)
            }
            if (index % 2 == 0) {
                frame = GlyphDesignSystem.drawCircleApprox(frame, GlyphDesignSystem.fullRingRadius, 34, thickness = 0.45)
            }
            GlyphDesignSystem.clean(frame, removeStrays = false)
        }
    }
}
