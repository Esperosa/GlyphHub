package com.pelikan.glyphhub.glyph

import java.text.Normalizer

object GlyphTextRenderer {
    enum class FontMode {
        Compact3x5,
        ReadableDigits4x5,
        SingleGlyph5x7
    }

    fun transliterate(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return buildString(normalized.length) {
            normalized.forEach { char ->
                if (Character.getType(char) != Character.NON_SPACING_MARK.toInt()) {
                    append(
                        when (char) {
                            'Ř', 'ř' -> 'R'
                            'Š', 'š' -> 'S'
                            'Ž', 'ž' -> 'Z'
                            'Č', 'č' -> 'C'
                            'Ď', 'ď' -> 'D'
                            'Ť', 'ť' -> 'T'
                            'Ň', 'ň' -> 'N'
                            'Ě', 'ě' -> 'E'
                            'Ů', 'ů', 'Ú', 'ú' -> 'U'
                            'Ý', 'ý' -> 'Y'
                            'Á', 'á' -> 'A'
                            'Í', 'í' -> 'I'
                            'É', 'é' -> 'E'
                            'Ó', 'ó' -> 'O'
                            else -> char
                        }
                    )
                }
            }
        }.uppercase()
    }

    fun drawText3x5(text: String, brightness: Int, yOffset: Int = 4, xOffset: Int = 0, spacing: Int = 1): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        var cursor = xOffset
        transliterate(text).forEach { char ->
            val glyph = Font3x5[char] ?: Font3x5[' ']!!
            glyph.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    if (bit == '1') {
                        frame = frame.withPixelBrightness(cursor + x, yOffset + y, 100)
                    }
                }
            }
            cursor += glyph.first().length + spacing
        }
        return frame
    }

    fun drawText3x5Vertical(text: String, brightness: Int, xOffset: Int = 5, yOffset: Int = 0, spacing: Int = 1): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        var cursor = yOffset
        transliterate(text).forEach { char ->
            val glyph = Font3x5[char] ?: Font3x5[' ']!!
            glyph.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    if (bit == '1') {
                        frame = frame.withPixelBrightness(xOffset + x, cursor + y, 100)
                    }
                }
            }
            cursor += glyph.size + spacing
        }
        return frame
    }

    fun drawCenteredText3x5(text: String, brightness: Int, yOffset: Int = 4, spacing: Int = 1): GlyphFrame {
        val width = measureText3x5(text, spacing)
        val xOffset = ((GlyphFrame.MATRIX_SIZE - width) / 2).coerceAtLeast(0)
        return drawText3x5(text, brightness, yOffset = yOffset, xOffset = xOffset, spacing = spacing)
    }

    fun measureText3x5(text: String, spacing: Int = 1): Int {
        val normalized = transliterate(text)
        if (normalized.isEmpty()) return 0
        return normalized.sumOf { char -> (Font3x5[char] ?: Font3x5[' ']!!).first().length + spacing } - spacing
    }

    fun drawReadableDigits(value: String, brightness: Int, centered: Boolean = true): GlyphFrame {
        val digits = value.filter { it.isDigit() }.take(2).ifEmpty { "0" }
        val width = digits.sumOf { (Digits4x5[it] ?: Digits4x5['0']!!).first().length } + (digits.length - 1)
        var cursor = if (centered) ((GlyphFrame.MATRIX_SIZE - width) / 2).coerceAtLeast(0) else 0
        var frame = GlyphFrame.empty13(brightness)
        digits.forEach { digit ->
            val glyph = Digits4x5[digit] ?: Digits4x5['0']!!
            glyph.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    if (bit == '1') {
                        frame = frame.withPixelBrightness(cursor + x, 4 + y, 100)
                    }
                }
            }
            cursor += glyph.first().length + 1
        }
        return frame
    }

    fun drawSingleGlyph5x7(char: Char, brightness: Int): GlyphFrame {
        val glyph = Upper5x7[transliterate(char.toString()).firstOrNull() ?: ' '] ?: Upper5x7[' ']!!
        val xOffset = ((GlyphFrame.MATRIX_SIZE - glyph.first().length) / 2).coerceAtLeast(0)
        val yOffset = ((GlyphFrame.MATRIX_SIZE - glyph.size) / 2).coerceAtLeast(0)
        var frame = GlyphFrame.empty13(brightness)
        glyph.forEachIndexed { y, row ->
            row.forEachIndexed { x, bit ->
                if (bit == '1') {
                    frame = frame.withPixelBrightness(xOffset + x, yOffset + y, 100)
                }
            }
        }
        return frame
    }

    private val Font3x5: Map<Char, List<String>> = mapOf(
        ' ' to listOf("000", "000", "000", "000", "000"),
        '-' to listOf("000", "000", "111", "000", "000"),
        ':' to listOf("000", "010", "000", "010", "000"),
        '.' to listOf("000", "000", "000", "000", "010"),
        '/' to listOf("001", "001", "010", "100", "100"),
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111"),
        'A' to listOf("010", "101", "111", "101", "101"),
        'B' to listOf("110", "101", "110", "101", "110"),
        'C' to listOf("111", "100", "100", "100", "111"),
        'D' to listOf("110", "101", "101", "101", "110"),
        'E' to listOf("111", "100", "110", "100", "111"),
        'F' to listOf("111", "100", "110", "100", "100"),
        'G' to listOf("111", "100", "101", "101", "111"),
        'H' to listOf("101", "101", "111", "101", "101"),
        'I' to listOf("111", "010", "010", "010", "111"),
        'J' to listOf("001", "001", "001", "101", "111"),
        'K' to listOf("101", "101", "110", "101", "101"),
        'L' to listOf("100", "100", "100", "100", "111"),
        'M' to listOf("101", "111", "111", "101", "101"),
        'N' to listOf("101", "111", "111", "111", "101"),
        'O' to listOf("111", "101", "101", "101", "111"),
        'P' to listOf("111", "101", "111", "100", "100"),
        'Q' to listOf("111", "101", "101", "111", "001"),
        'R' to listOf("110", "101", "110", "101", "101"),
        'S' to listOf("111", "100", "111", "001", "111"),
        'T' to listOf("111", "010", "010", "010", "010"),
        'U' to listOf("101", "101", "101", "101", "111"),
        'V' to listOf("101", "101", "101", "101", "010"),
        'W' to listOf("101", "101", "111", "111", "101"),
        'X' to listOf("101", "101", "010", "101", "101"),
        'Y' to listOf("101", "101", "010", "010", "010"),
        'Z' to listOf("111", "001", "010", "100", "111")
    )

    private val Digits4x5: Map<Char, List<String>> = mapOf(
        '0' to listOf("1110", "1001", "1001", "1001", "1110"),
        '1' to listOf("0010", "0110", "0010", "0010", "0111"),
        '2' to listOf("1110", "0001", "1110", "1000", "1111"),
        '3' to listOf("1110", "0001", "0110", "0001", "1110"),
        '4' to listOf("1001", "1001", "1111", "0001", "0001"),
        '5' to listOf("1111", "1000", "1110", "0001", "1110"),
        '6' to listOf("0111", "1000", "1110", "1001", "1110"),
        '7' to listOf("1111", "0001", "0010", "0100", "0100"),
        '8' to listOf("1110", "1001", "1110", "1001", "1110"),
        '9' to listOf("1110", "1001", "1111", "0001", "1110")
    )

    private val Upper5x7: Map<Char, List<String>> = mapOf(
        ' ' to List(7) { "00000" },
        'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
        'B' to listOf("11110", "10001", "11110", "10001", "10001", "10001", "11110"),
        'C' to listOf("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
        'D' to listOf("11100", "10010", "10001", "10001", "10001", "10010", "11100"),
        'E' to listOf("11111", "10000", "11110", "10000", "10000", "10000", "11111"),
        'F' to listOf("11111", "10000", "11110", "10000", "10000", "10000", "10000"),
        'G' to listOf("01110", "10001", "10000", "10111", "10001", "10001", "01110"),
        'H' to listOf("10001", "10001", "11111", "10001", "10001", "10001", "10001"),
        'I' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
        'J' to listOf("00111", "00010", "00010", "00010", "10010", "10010", "01100"),
        'K' to listOf("10001", "10010", "11100", "10010", "10001", "10001", "10001"),
        'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
        'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
        'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
        'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
        'Q' to listOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
        'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
        'S' to listOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
        'T' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
        'U' to listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
        'V' to listOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
        'W' to listOf("10001", "10001", "10001", "10101", "10101", "11011", "10001"),
        'X' to listOf("10001", "01010", "00100", "00100", "00100", "01010", "10001"),
        'Y' to listOf("10001", "01010", "00100", "00100", "00100", "00100", "00100"),
        'Z' to listOf("11111", "00010", "00100", "00100", "01000", "10000", "11111")
    )
}