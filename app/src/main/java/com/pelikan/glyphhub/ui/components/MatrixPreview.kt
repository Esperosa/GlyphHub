package com.pelikan.glyphhub.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pelikan.glyphhub.glyph.GlyphFrame

@Composable
fun MatrixPreview(
    frame: GlyphFrame,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    PixelGrid(
        frame = frame,
        modifier = modifier,
        active = active,
        showOffPixels = false
    )
}
