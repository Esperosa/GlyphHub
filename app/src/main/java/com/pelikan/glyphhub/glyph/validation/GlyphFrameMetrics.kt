package com.pelikan.glyphhub.glyph.validation

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout

data class GlyphBoundingBox(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int
) {
    val width: Int get() = maxX - minX + 1
    val height: Int get() = maxY - minY + 1
}

data class GlyphFrameMetrics(
    val width: Int,
    val height: Int,
    val litPixels: Int,
    val physicalLitPixels: Int,
    val physicalLedCount: Int,
    val outOfMaskPixels: Int,
    val isolatedPixels: Int,
    val minIntensity: Int,
    val maxIntensity: Int,
    val uniqueIntensityCount: Int,
    val boundingBox: GlyphBoundingBox?,
    val centerOfMassX: Double?,
    val centerOfMassY: Double?
) {
    val fillRatioPercent: Int =
        if (physicalLedCount == 0) 0 else physicalLitPixels * 100 / physicalLedCount

    val isAllOff: Boolean = physicalLitPixels == 0
    val isAllOn: Boolean = physicalLedCount > 0 && physicalLitPixels >= physicalLedCount
    val isTiny: Boolean = boundingBox?.let { it.width <= 3 && it.height <= 3 && litPixels <= 5 } ?: false

    companion object {
        fun from(frame: GlyphFrame): GlyphFrameMetrics {
            var lit = 0
            var physicalLit = 0
            var outOfMask = 0
            var isolated = 0
            var minIntensity: Int? = null
            var maxIntensity = 0
            val unique = mutableSetOf<Int>()
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE
            var weightedX = 0.0
            var weightedY = 0.0
            var weight = 0.0

            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    val intensity = frame.intensityAt(x, y)
                    if (intensity <= 0) continue
                    lit += 1
                    minIntensity = minOf(minIntensity ?: intensity, intensity)
                    maxIntensity = maxOf(maxIntensity, intensity)
                    unique += intensity
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                    weightedX += x * intensity
                    weightedY += y * intensity
                    weight += intensity
                    val physical = frame.width != GlyphFrame.MATRIX_SIZE ||
                        frame.height != GlyphFrame.MATRIX_SIZE ||
                        GlyphMatrixLayout.isPhysicalLed(x, y)
                    if (physical) {
                        physicalLit += 1
                    } else {
                        outOfMask += 1
                    }
                    if (neighborCount(frame, x, y) == 0) isolated += 1
                }
            }

            val physicalLedCount =
                if (frame.width == GlyphFrame.MATRIX_SIZE && frame.height == GlyphFrame.MATRIX_SIZE) {
                    physicalLedCount13
                } else {
                    frame.width * frame.height
                }
            return GlyphFrameMetrics(
                width = frame.width,
                height = frame.height,
                litPixels = lit,
                physicalLitPixels = physicalLit,
                physicalLedCount = physicalLedCount,
                outOfMaskPixels = outOfMask,
                isolatedPixels = isolated,
                minIntensity = minIntensity ?: 0,
                maxIntensity = maxIntensity,
                uniqueIntensityCount = unique.size,
                boundingBox = if (lit == 0) null else GlyphBoundingBox(minX, minY, maxX, maxY),
                centerOfMassX = if (weight > 0.0) weightedX / weight else null,
                centerOfMassY = if (weight > 0.0) weightedY / weight else null
            )
        }

        private fun neighborCount(frame: GlyphFrame, x: Int, y: Int): Int {
            var count = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    if (frame.intensityAt(x + dx, y + dy) > 0) count += 1
                }
            }
            return count
        }

        private val physicalLedCount13: Int =
            (0 until GlyphFrame.MATRIX_SIZE).sumOf { y ->
                (0 until GlyphFrame.MATRIX_SIZE).count { x -> GlyphMatrixLayout.isPhysicalLed(x, y) }
            }
    }
}
