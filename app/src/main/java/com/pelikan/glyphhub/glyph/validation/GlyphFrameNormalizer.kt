package com.pelikan.glyphhub.glyph.validation

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout

object GlyphFrameNormalizer {
    fun normalize(
        frame: GlyphFrame,
        respectCircularMask: Boolean = true,
        targetSize: Int = GlyphFrame.MATRIX_SIZE
    ): GlyphFrame {
        val sized = if (frame.width == targetSize && frame.height == targetSize) {
            frame
        } else {
            centerCropTo13(frame)
        }
        val normalizedPixels = sized.pixels.mapIndexed { index, raw ->
            val x = index % sized.width
            val y = index / sized.width
            val intensity = when {
                raw <= 0 -> 0
                raw == 1 -> 100
                else -> raw.coerceIn(0, 100)
            }
            if (
                respectCircularMask &&
                sized.width == GlyphFrame.MATRIX_SIZE &&
                sized.height == GlyphFrame.MATRIX_SIZE &&
                !GlyphMatrixLayout.isPhysicalLed(x, y)
            ) {
                0
            } else {
                intensity
            }
        }
        val litPixels = normalizedPixels.count { it > 0 }
        val brightness = sized.brightness.coerceIn(0, 100).let { value ->
            if (litPixels > 0 && value <= 0) DEFAULT_ACTIVE_BRIGHTNESS else value
        }
        return sized.copy(
            brightness = brightness,
            pixels = normalizedPixels
        )
    }

    fun quantize(frame: GlyphFrame, buckets: List<Int> = listOf(0, 40, 70, 100)): GlyphFrame {
        val sorted = buckets.distinct().sorted().ifEmpty { listOf(0, 100) }
        return frame.copy(
            pixels = frame.pixels.map { value ->
                sorted.minBy { bucket -> kotlin.math.abs(bucket - value.coerceIn(0, 100)) }
            }
        )
    }

    private fun centerCropTo13(frame: GlyphFrame): GlyphFrame {
        var output = GlyphFrame.empty13(frame.brightness)
        val offsetX = (GlyphFrame.MATRIX_SIZE - frame.width) / 2
        val offsetY = (GlyphFrame.MATRIX_SIZE - frame.height) / 2
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val targetX = x + offsetX
                val targetY = y + offsetY
                if (targetX in 0 until GlyphFrame.MATRIX_SIZE && targetY in 0 until GlyphFrame.MATRIX_SIZE) {
                    output = output.withPixelBrightness(targetX, targetY, frame.intensityAt(x, y))
                }
            }
        }
        return output
    }

    private const val DEFAULT_ACTIVE_BRIGHTNESS = 80
}
