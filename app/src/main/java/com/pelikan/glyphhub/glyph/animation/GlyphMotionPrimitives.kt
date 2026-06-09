package com.pelikan.glyphhub.glyph.animation

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.validation.GlyphCircularLayoutEngine
import com.pelikan.glyphhub.glyph.validation.GlyphFrameNormalizer

object GlyphMotionPrimitives {
    fun radialWipe(
        id: String,
        name: String,
        clockwise: Boolean = true,
        frames: Int = 12,
        brightness: Int = 80
    ): GlyphFrameSequence {
        val count = frames.coerceAtLeast(2)
        val sequenceFrames = (1..count).map { step ->
            val sweep = 360.0 * step / count.toDouble() * if (clockwise) 1.0 else -1.0
            var frame = GlyphFrame.empty13(brightness)
            frame = GlyphCircularLayoutEngine.drawPoints(
                frame,
                GlyphCircularLayoutEngine.arcPoints(-90.0, sweep),
                intensity = 100
            )
            GlyphSequenceFrame(GlyphFrameNormalizer.normalize(frame), 54L)
        }
        return GlyphFrameSequence(id, name, loop = false, fps = 18, frames = sequenceFrames)
    }

    fun orbitDot(
        id: String,
        name: String,
        frames: Int = 16,
        brightness: Int = 80,
        radius: Double = GlyphCircularLayoutEngine.fullRadius
    ): GlyphFrameSequence {
        val count = frames.coerceAtLeast(4)
        val sequenceFrames = (0 until count).map { index ->
            val angle = 360.0 * index / count.toDouble()
            val point = GlyphCircularLayoutEngine.pointAt(angle, radius)
            var frame = GlyphFrame.empty13(brightness)
            frame = GlyphCircularLayoutEngine.drawPoints(frame, GlyphCircularLayoutEngine.ringPoints(radius, thickness = 0.4), 30)
            frame = GlyphCircularLayoutEngine.drawPoints(frame, listOf(point), 100)
            GlyphSequenceFrame(GlyphFrameNormalizer.normalize(frame), 62L)
        }
        return GlyphFrameSequence(id, name, loop = true, fps = 16, frames = sequenceFrames)
    }

    fun pulse(
        id: String,
        name: String,
        brightness: Int = 80,
        intensities: List<Int> = listOf(35, 55, 75, 100, 75, 55)
    ): GlyphFrameSequence {
        val sequenceFrames = intensities.mapIndexed { index, intensity ->
            val radius = 1.0 + index.coerceAtMost(3) * 1.2
            var frame = GlyphFrame.empty13(brightness)
            frame = GlyphCircularLayoutEngine.drawPoints(frame, GlyphCircularLayoutEngine.ringPoints(radius, 0.7), intensity)
            GlyphSequenceFrame(GlyphFrameNormalizer.normalize(frame), 70L)
        }
        return GlyphFrameSequence(id, name, loop = false, fps = 14, frames = sequenceFrames)
    }
}
