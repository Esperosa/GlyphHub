package com.pelikan.glyphhub.glyph

data class GlyphAnimationFrame(
    val frame: GlyphFrame,
    val durationMs: Long
)

data class GlyphAnimation(
    val id: String,
    val name: String,
    val loop: Boolean,
    val frames: List<GlyphAnimationFrame>
)
