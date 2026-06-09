package com.pelikan.glyphhub.glyph.assets

import com.pelikan.glyphhub.glyph.animation.GlyphFrameSequence
import com.pelikan.glyphhub.glyph.animation.GlyphSequenceFrame

data class GlyphAnimationAsset(
    val asset: GlyphAsset,
    val loop: Boolean = false,
    val fps: Int = 12
) {
    fun toSequence(): GlyphFrameSequence =
        GlyphFrameSequence(
            id = asset.id,
            name = asset.name,
            loop = loop,
            fps = fps,
            frames = asset.frames.map { GlyphSequenceFrame(it.frame, it.durationMs.coerceAtLeast(1L)) }
        )
}
