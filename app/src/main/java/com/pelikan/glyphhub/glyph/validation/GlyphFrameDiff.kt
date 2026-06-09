package com.pelikan.glyphhub.glyph.validation

import com.pelikan.glyphhub.glyph.GlyphFrame

data class GlyphFrameDiff(
    val changedPixels: Int,
    val turnedOnPixels: Int,
    val turnedOffPixels: Int,
    val maxIntensityDelta: Int
) {
    companion object {
        fun between(previous: GlyphFrame?, next: GlyphFrame): GlyphFrameDiff {
            if (previous == null || previous.width != next.width || previous.height != next.height) {
                return GlyphFrameDiff(
                    changedPixels = next.pixels.count { it > 0 },
                    turnedOnPixels = next.pixels.count { it > 0 },
                    turnedOffPixels = 0,
                    maxIntensityDelta = next.pixels.maxOrNull() ?: 0
                )
            }
            var changed = 0
            var on = 0
            var off = 0
            var maxDelta = 0
            for (index in next.pixels.indices) {
                val before = previous.pixels[index].coerceIn(0, 100)
                val after = next.pixels[index].coerceIn(0, 100)
                if (before != after) {
                    changed += 1
                    if (before <= 0 && after > 0) on += 1
                    if (before > 0 && after <= 0) off += 1
                    maxDelta = maxOf(maxDelta, kotlin.math.abs(after - before))
                }
            }
            return GlyphFrameDiff(changed, on, off, maxDelta)
        }
    }
}
