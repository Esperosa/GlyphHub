package com.pelikan.glyphhub.glyph.assets

import com.pelikan.glyphhub.glyph.GlyphFrame

enum class GlyphAssetType(val id: String) {
    Icon("icon"),
    Animation("animation"),
    Text("text"),
    Transition("transition"),
    Imported("imported");

    companion object {
        fun fromId(id: String): GlyphAssetType =
            entries.firstOrNull { it.id == id } ?: Animation
    }
}

data class GlyphAssetMetadata(
    val category: String = "",
    val intendedUse: List<String> = emptyList(),
    val source: String = "internal",
    val sourceTool: String = "GlyphHub",
    val validated: Boolean = false,
    val allowAllOff: Boolean = false,
    val allowFullMatrixFlash: Boolean = false,
    val allowOffMaskPixels: Boolean = false,
    val particleEffect: Boolean = false,
    val directional: Boolean = false,
    val notes: String = ""
)

data class GlyphAssetFrame(
    val frame: GlyphFrame,
    val durationMs: Long = 1000L
)

data class GlyphAsset(
    val id: String,
    val name: String,
    val matrix: Int = GlyphFrame.MATRIX_SIZE,
    val type: GlyphAssetType = GlyphAssetType.Icon,
    val respectCircularMask: Boolean = true,
    val normalizeBrightness: Boolean = true,
    val frames: List<GlyphAssetFrame>,
    val metadata: GlyphAssetMetadata = GlyphAssetMetadata()
)
