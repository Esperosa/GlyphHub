package com.pelikan.glyphhub.glyph.animation

import com.pelikan.glyphhub.glyph.GlyphAnimation
import com.pelikan.glyphhub.glyph.GlyphAnimationFrame
import com.pelikan.glyphhub.glyph.GlyphFrame

data class GlyphSequenceFrame(
    val frame: GlyphFrame,
    val durationMs: Long
)

data class GlyphFrameSequence(
    val id: String,
    val name: String,
    val loop: Boolean = false,
    val fps: Int = 12,
    val frames: List<GlyphSequenceFrame>
) {
    init {
        require(id.isNotBlank()) { "Sequence id cannot be blank." }
        require(frames.isNotEmpty()) { "Sequence must contain at least one frame." }
    }

    val totalDurationMs: Long = frames.sumOf { it.durationMs.coerceAtLeast(1L) }

    fun toGlyphAnimation(): GlyphAnimation =
        GlyphAnimation(
            id = id,
            name = name,
            loop = loop,
            frames = frames.map { GlyphAnimationFrame(it.frame, it.durationMs.coerceAtLeast(1L)) }
        )

    companion object {
        fun single(id: String, name: String, frame: GlyphFrame, durationMs: Long = 1000L): GlyphFrameSequence =
            GlyphFrameSequence(
                id = id,
                name = name,
                loop = false,
                frames = listOf(GlyphSequenceFrame(frame, durationMs.coerceAtLeast(1L)))
            )

        fun fromAnimation(animation: GlyphAnimation): GlyphFrameSequence =
            GlyphFrameSequence(
                id = animation.id,
                name = animation.name,
                loop = animation.loop,
                frames = animation.frames.map { GlyphSequenceFrame(it.frame, it.durationMs) }
            )
    }
}
