package com.pelikan.glyphhub.glyph.animation

import android.graphics.Bitmap
import android.graphics.Color
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.validation.GlyphFrameNormalizer

object BitmapGlyphImporter {
    fun importBitmap(bitmap: Bitmap, options: GlyphImportOptions = GlyphImportOptions()): GlyphImportResult =
        runCatching {
            val frame = bitmapToFrame(bitmap, options)
            GlyphImportResult.Success(
                GlyphFrameSequence.single(
                    id = options.assetId,
                    name = options.assetName,
                    frame = GlyphFrameNormalizer.normalize(frame, respectCircularMask = options.respectCircularMask)
                ),
                notes = listOf("Imported bitmap as one 13x13 thresholded frame.")
            )
        }.getOrElse { GlyphImportResult.Failed("Bitmap import failed.", it) }

    fun bitmapToFrame(bitmap: Bitmap, options: GlyphImportOptions = GlyphImportOptions()): GlyphFrame {
        var frame = GlyphFrame.empty13(options.brightness)
        val sourceSize = minOf(bitmap.width, bitmap.height).coerceAtLeast(1)
        val startX = ((bitmap.width - sourceSize) / 2).coerceAtLeast(0)
        val startY = ((bitmap.height - sourceSize) / 2).coerceAtLeast(0)
        for (y in 0 until GlyphFrame.MATRIX_SIZE) {
            for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                val sx = startX + (x + 0.5f) * sourceSize / GlyphFrame.MATRIX_SIZE
                val sy = startY + (y + 0.5f) * sourceSize / GlyphFrame.MATRIX_SIZE
                val color = bitmap.getPixel(
                    sx.toInt().coerceIn(0, bitmap.width - 1),
                    sy.toInt().coerceIn(0, bitmap.height - 1)
                )
                val alpha = Color.alpha(color)
                val luminance = (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114).toInt()
                val intensity = if (alpha <= 8 || luminance < options.threshold) 0 else luminance.coerceIn(1, 100)
                frame = frame.withPixelBrightness(x, y, intensity)
            }
        }
        return frame
    }
}
