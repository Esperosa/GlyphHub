package com.pelikan.glyphhub.glyph

import com.pelikan.glyphhub.toys.GlyphToyModule

interface GlyphRenderer {
    fun render(module: GlyphToyModule, deltaMs: Long): GlyphFrame
}

class ModuleGlyphRenderer : GlyphRenderer {
    override fun render(module: GlyphToyModule, deltaMs: Long): GlyphFrame =
        module.onTick(deltaMs)
}
