package com.pelikan.glyphhub.glyph.animation

sealed class GlyphImportResult {
    data class Success(val sequence: GlyphFrameSequence, val notes: List<String> = emptyList()) : GlyphImportResult()
    data class Unsupported(val reason: String) : GlyphImportResult()
    data class Failed(val reason: String, val throwable: Throwable? = null) : GlyphImportResult()
}

interface GlyphAnimationImporter {
    val id: String
    val displayName: String
    fun import(bytes: ByteArray, options: GlyphImportOptions = GlyphImportOptions()): GlyphImportResult
}

data class GlyphImportOptions(
    val assetId: String = "imported_asset",
    val assetName: String = "Imported Asset",
    val fps: Int = 12,
    val threshold: Int = 90,
    val brightness: Int = 80,
    val loop: Boolean = false,
    val respectCircularMask: Boolean = true
)
