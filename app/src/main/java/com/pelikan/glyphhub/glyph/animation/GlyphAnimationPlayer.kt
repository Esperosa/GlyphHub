package com.pelikan.glyphhub.glyph.animation

import com.pelikan.glyphhub.glyph.GlyphFrame

class GlyphAnimationPlayer(
    private val sequence: GlyphFrameSequence,
    private val startedAtMs: Long
) {
    fun frameAt(nowMs: Long): GlyphFrame {
        val elapsed = (nowMs - startedAtMs).coerceAtLeast(0L)
        val position = if (sequence.loop && sequence.totalDurationMs > 0L) {
            elapsed % sequence.totalDurationMs
        } else {
            elapsed.coerceAtMost(sequence.totalDurationMs - 1L)
        }
        var cursor = 0L
        sequence.frames.forEach { frame ->
            cursor += frame.durationMs.coerceAtLeast(1L)
            if (position < cursor) return frame.frame
        }
        return sequence.frames.last().frame
    }
}
