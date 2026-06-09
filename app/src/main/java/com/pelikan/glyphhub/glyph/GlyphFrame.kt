package com.pelikan.glyphhub.glyph

data class GlyphFrame(
    val width: Int,
    val height: Int,
    val brightness: Int,
    val pixels: List<Int>
) {
    init {
        require(width > 0 && height > 0) { "Frame dimensions must be positive." }
        require(pixels.size == width * height) { "Pixel data must match frame dimensions." }
    }

    fun isOn(x: Int, y: Int): Boolean =
        x in 0 until width && y in 0 until height && pixels[y * width + x] > 0

    fun intensityAt(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        val value = pixels[y * width + x]
        return if (value == 1) 100 else value.coerceIn(0, 100)
    }

    fun withPixel(x: Int, y: Int, on: Boolean): GlyphFrame {
        return withPixelBrightness(x, y, if (on) 100 else 0)
    }

    fun withPixelBrightness(x: Int, y: Int, intensity: Int): GlyphFrame {
        if (x !in 0 until width || y !in 0 until height) return this
        val copy = pixels.toMutableList()
        copy[y * width + x] = intensity.coerceIn(0, 100)
        return copy(pixels = copy)
    }

    fun overlay(other: GlyphFrame, offsetX: Int = 0, offsetY: Int = 0): GlyphFrame {
        var output = this
        for (y in 0 until other.height) {
            for (x in 0 until other.width) {
                val intensity = other.intensityAt(x, y)
                if (intensity > 0) {
                    output = output.withPixelBrightness(x + offsetX, y + offsetY, intensity)
                }
            }
        }
        return output
    }

    fun shift(dx: Int, dy: Int): GlyphFrame {
        var output = empty(width, height, brightness)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val intensity = intensityAt(x, y)
                if (intensity > 0) {
                    output = output.withPixelBrightness(x + dx, y + dy, intensity)
                }
            }
        }
        return output
    }

    fun invert(): GlyphFrame = copy(pixels = pixels.map { if (it > 0) 0 else 1 })

    fun scaleBrightness(scale: Float): GlyphFrame =
        copy(brightness = (brightness * scale).toInt().coerceIn(0, 100))

    fun scaleIntensity(scale: Float): GlyphFrame =
        copy(pixels = pixels.map { (it * scale).toInt().coerceIn(0, 100) })

    fun litPixelCount(): Int = pixels.count { it > 0 }

    fun withBrightness(brightness: Int): GlyphFrame =
        copy(brightness = brightness.coerceIn(0, 100))

    fun toSdkFormat(): IntArray =
        GlyphMatrixLayout.mask(this).pixels
            .map { value ->
                val intensity = if (value == 1) 100 else value.coerceIn(0, 100)
                brightness.coerceIn(0, 100) * intensity / 100
            }
            .toIntArray()

    companion object {
        const val MATRIX_SIZE = 13

        fun empty13(brightness: Int = 0): GlyphFrame = empty(MATRIX_SIZE, MATRIX_SIZE, brightness)

        fun empty(width: Int, height: Int, brightness: Int = 0): GlyphFrame =
            GlyphFrame(width, height, brightness.coerceIn(0, 100), List(width * height) { 0 })

        fun fromBinaryRows(rows: List<String>, brightness: Int = 80): GlyphFrame {
            require(rows.isNotEmpty()) { "Rows cannot be empty." }
            val width = rows.first().length
            require(rows.all { it.length == width }) { "All rows must have equal length." }
            val pixels = rows.flatMap { row ->
                row.map { if (it == '1') 100 else 0 }
            }
            return GlyphFrame(width, rows.size, brightness.coerceIn(0, 100), pixels)
        }
    }
}
