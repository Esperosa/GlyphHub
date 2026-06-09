package com.pelikan.glyphhub.glyph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object GlyphFramePreview {
    fun bitmap(frame: GlyphFrame, sizePx: Int = 156, active: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.TRANSPARENT)
        val cell = sizePx / frame.width.toFloat()
        val dotRadius = cell * 0.32f
        val onColor = if (active) Color.rgb(230, 0, 18) else Color.WHITE
        val offColor = Color.rgb(42, 42, 42)
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                paint.color = if (frame.isOn(x, y) && GlyphMatrixLayout.isPhysicalLed(x, y)) onColor else offColor
                paint.alpha = if (frame.isOn(x, y)) (frame.intensityAt(x, y) * 255 / 100).coerceIn(48, 255) else 255
                canvas.drawCircle(
                    x * cell + cell / 2f,
                    y * cell + cell / 2f,
                    dotRadius,
                    paint
                )
            }
        }
        return bitmap
    }

    fun ledOnlyBitmap(frame: GlyphFrame, sizePx: Int = 192, active: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            style = Paint.Style.FILL
            color = if (active) Color.rgb(230, 0, 18) else Color.WHITE
        }
        canvas.drawColor(Color.TRANSPARENT)
        val cell = sizePx / frame.width.toFloat()
        val led = cell * 0.62f
        val inset = (cell - led) / 2f
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (frame.isOn(x, y) && GlyphMatrixLayout.isPhysicalLed(x, y)) {
                    paint.alpha = (frame.intensityAt(x, y) * 255 / 100).coerceIn(48, 255)
                    canvas.drawRect(
                        x * cell + inset,
                        y * cell + inset,
                        x * cell + inset + led,
                        y * cell + inset + led,
                        paint
                    )
                }
            }
        }
        return bitmap
    }
}
