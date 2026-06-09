package com.pelikan.glyphhub.glyph.assets

data class GlyphAssetManifest(
    val matrix: Int,
    val device: String,
    val assets: List<String>,
    val transitions: List<String>
)
