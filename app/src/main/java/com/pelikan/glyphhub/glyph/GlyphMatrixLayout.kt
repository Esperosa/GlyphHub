package com.pelikan.glyphhub.glyph

object GlyphMatrixLayout {
    const val SIZE = GlyphFrame.MATRIX_SIZE

    private val circularRows = listOf(
        "0000111110000",
        "0011111111100",
        "0111111111110",
        "0111111111110",
        "1111111111111",
        "1111111111111",
        "1111111111111",
        "1111111111111",
        "1111111111111",
        "0111111111110",
        "0111111111110",
        "0011111111100",
        "0000111110000"
    )

    fun isPhysicalLed(x: Int, y: Int): Boolean =
        x in 0 until SIZE && y in 0 until SIZE && circularRows[y][x] == '1'

    fun centerOffset(width: Int, height: Int): Pair<Int, Int> =
        ((SIZE - width) / 2) to ((SIZE - height) / 2)

    fun mask(frame: GlyphFrame): GlyphFrame {
        if (frame.width != SIZE || frame.height != SIZE) return frame
        return frame.copy(
            pixels = frame.pixels.mapIndexed { index, value ->
                val x = index % SIZE
                val y = index / SIZE
                if (isPhysicalLed(x, y)) value else 0
            }
        )
    }
}
