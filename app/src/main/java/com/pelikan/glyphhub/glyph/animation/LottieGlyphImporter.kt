package com.pelikan.glyphhub.glyph.animation

object LottieGlyphImporter : GlyphAnimationImporter {
    override val id: String = "lottie"
    override val displayName: String = "Lottie JSON"

    override fun import(bytes: ByteArray, options: GlyphImportOptions): GlyphImportResult =
        GlyphImportResult.Unsupported(
            "Lottie import is documented as feasible but is not enabled in this build. " +
                "Add Lottie Android, render sampled frames to bitmaps, downsample to 13x13, normalize, then validate."
        )
}
