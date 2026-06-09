package com.pelikan.glyphhub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.ui.theme.GlyphLine
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite

@Composable
fun PixelGrid(
    frame: GlyphFrame,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    showOffPixels: Boolean = false
) {
    Canvas(
        modifier = modifier.aspectRatio(1f)
    ) {
        val cell = size.width / frame.width
        val led = cell * 0.62f
        val inset = (cell - led) / 2f
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val isOn = frame.isOn(x, y)
                if (!isOn && !showOffPixels) continue
                val alpha = if (isOn) {
                    (frame.intensityAt(x, y) / 100f).coerceIn(0.25f, 1f)
                } else {
                    0.45f
                }
                drawRect(
                    color = when {
                        isOn && active -> GlyphRed.copy(alpha = alpha)
                        isOn -> GlyphWhite.copy(alpha = alpha)
                        else -> GlyphLine.copy(alpha = alpha)
                    },
                    topLeft = androidx.compose.ui.geometry.Offset(x * cell + inset, y * cell + inset),
                    size = Size(led, led)
                )
            }
        }
    }
}

@Composable
fun EditablePixelGrid(
    frame: GlyphFrame,
    modifier: Modifier = Modifier,
    onToggle: (x: Int, y: Int) -> Unit
) {
    PixelGrid(
        frame = frame,
        active = true,
        showOffPixels = true,
        modifier = modifier.pointerInput(frame) {
            detectTapGestures { offset ->
                val cell = size.width / GlyphFrame.MATRIX_SIZE.toFloat()
                val x = (offset.x / cell).toInt()
                val y = (offset.y / cell).toInt()
                if (GlyphMatrixLayout.isPhysicalLed(x, y)) onToggle(x, y)
            }
        }
    )
}
