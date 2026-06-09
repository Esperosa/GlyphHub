package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.audio.TuningMath
import com.pelikan.glyphhub.audio.TuningTarget
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

internal object TunerToyLogic {
    const val MAX_DISPLAY_CENTS = 50

    fun targets(instrument: String, preset: String, a4: Float): List<TuningTarget> =
        when (instrument) {
            "ukulele" -> ukuleleTargets(preset, a4)
            "chromatic" -> (36..84).map { midi ->
                TuningTarget(noteName(midi), TuningMath.frequencyForMidi(midi, a4))
            }
            else -> guitarTargets(preset, a4)
        }

    fun displayLabel(target: TuningTarget): String =
        target.label.filter { it.isLetterOrDigit() }.take(2).ifBlank { "?" }

    fun markerX(cents: Int): Int {
        val normalized = cents.coerceIn(-MAX_DISPLAY_CENTS, MAX_DISPLAY_CENTS) / MAX_DISPLAY_CENTS.toFloat()
        return (6 + normalized * 5f).roundToInt().coerceIn(1, 11)
    }

    fun markerPosition(cents: Float): Float {
        val normalized = cents.coerceIn(-MAX_DISPLAY_CENTS.toFloat(), MAX_DISPLAY_CENTS.toFloat()) / MAX_DISPLAY_CENTS
        return (6f + normalized * 6f).coerceIn(0f, 12f)
    }

    private fun guitarTargets(preset: String, a4: Float): List<TuningTarget> =
        if (preset == "drop_d") {
            listOf(
                TuningTarget("D2", TuningMath.frequencyForMidi(38, a4)),
                TuningTarget("A2", TuningMath.frequencyForMidi(45, a4)),
                TuningTarget("D3", TuningMath.frequencyForMidi(50, a4)),
                TuningTarget("G3", TuningMath.frequencyForMidi(55, a4)),
                TuningTarget("B3", TuningMath.frequencyForMidi(59, a4)),
                TuningTarget("E4", TuningMath.frequencyForMidi(64, a4))
            )
        } else {
            listOf(
                TuningTarget("E2", TuningMath.frequencyForMidi(40, a4)),
                TuningTarget("A2", TuningMath.frequencyForMidi(45, a4)),
                TuningTarget("D3", TuningMath.frequencyForMidi(50, a4)),
                TuningTarget("G3", TuningMath.frequencyForMidi(55, a4)),
                TuningTarget("B3", TuningMath.frequencyForMidi(59, a4)),
                TuningTarget("E4", TuningMath.frequencyForMidi(64, a4))
            )
        }

    private fun ukuleleTargets(preset: String, a4: Float): List<TuningTarget> =
        when (preset) {
            "uke_low_g" -> listOf(
                TuningTarget("G3", TuningMath.frequencyForMidi(55, a4)),
                TuningTarget("C4", TuningMath.frequencyForMidi(60, a4)),
                TuningTarget("E4", TuningMath.frequencyForMidi(64, a4)),
                TuningTarget("A4", TuningMath.frequencyForMidi(69, a4))
            )
            "uke_baritone" -> listOf(
                TuningTarget("D3", TuningMath.frequencyForMidi(50, a4)),
                TuningTarget("G3", TuningMath.frequencyForMidi(55, a4)),
                TuningTarget("B3", TuningMath.frequencyForMidi(59, a4)),
                TuningTarget("E4", TuningMath.frequencyForMidi(64, a4))
            )
            else -> listOf(
                TuningTarget("G4", TuningMath.frequencyForMidi(67, a4)),
                TuningTarget("C4", TuningMath.frequencyForMidi(60, a4)),
                TuningTarget("E4", TuningMath.frequencyForMidi(64, a4)),
                TuningTarget("A4", TuningMath.frequencyForMidi(69, a4))
            )
        }

    private fun noteName(midi: Int): String {
        val names = listOf("C", "CS", "D", "DS", "E", "F", "FS", "G", "GS", "A", "AS", "B")
        return names[midi.floorMod(12)]
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}

internal object TunerToyVisuals {
    fun drawTuner(
        label: String,
        cents: Float,
        toleranceCents: Int,
        brightness: Int
    ): GlyphFrame {
        val inTune = abs(cents) <= toleranceCents.coerceIn(1, 20)
        var frame = GlyphFrame.empty13(brightness)
        frame = frame.overlay(drawDominantLabel(label))
        frame = drawMeter(frame, cents, inTune)
        return GlyphMatrixLayout.mask(frame)
    }

    fun drawWaiting(brightness: Int): GlyphFrame =
        GlyphMatrixLayout.mask(drawCenteredText3x5("TUN", brightness, yOffset = 4))

    fun drawNoSignal(brightness: Int): GlyphFrame {
        var frame = drawCenteredText3x5("TUN", brightness, yOffset = 3)
        frame = frame.withPixelBrightness(4, 10, 52)
        frame = frame.withPixelBrightness(6, 10, 72)
        frame = frame.withPixelBrightness(8, 10, 52)
        frame = frame.withPixelBrightness(5, 11, 34)
        frame = frame.withPixelBrightness(7, 11, 34)
        return GlyphMatrixLayout.mask(frame)
    }

    private fun drawDominantLabel(label: String): GlyphFrame {
        val display = label.filter { it.isLetterOrDigit() }.take(2).ifBlank { "?" }
        if (display.length == 2 && display[0].isLetter() && display[1].isDigit()) {
            var frame = GlyphFrame.empty13(100)
            frame = drawTunerLetter(frame, display[0], xOffset = 2, yOffset = 1, intensity = 100)
            frame = drawTunerDigit(frame, display[1], xOffset = 7, yOffset = 1, intensity = 100)
            return frame
        }
        return GlyphTextRenderer.drawCenteredText3x5(display, brightness = 100, yOffset = 2, spacing = 1)
    }

    private fun drawMeter(input: GlyphFrame, cents: Float, inTune: Boolean): GlyphFrame {
        var frame = input
        for (x in 0..12) {
            val intensity = if (inTune) {
                100
            } else {
                val distance = abs(x - 6)
                (74 - distance * 9).coerceIn(18, 74)
            }
            frame = frame.withPixelBrightness(x, 8, intensity)
        }
        val position = TunerToyLogic.markerPosition(cents)
        val leftX = floor(position).toInt().coerceIn(0, 12)
        val rightX = (leftX + 1).coerceIn(0, 12)
        val rightWeight = position - leftX
        val leftWeight = 1f - rightWeight
        val markerIntensity = if (inTune) 100 else 96
        frame = drawMarkerColumn(frame, leftX, markerIntensity, leftWeight)
        if (rightX != leftX && rightWeight > 0.01f) {
            frame = drawMarkerColumn(frame, rightX, markerIntensity, rightWeight)
        }
        return frame
    }

    private fun drawMarkerColumn(input: GlyphFrame, x: Int, markerIntensity: Int, weight: Float): GlyphFrame {
        var frame = input
        val core = (markerIntensity * weight).roundToInt()
        val tip = (core * 0.82f).roundToInt()
        frame = frame.withPixelBrightness(x, 6, tip.coerceAtLeast(14))
        frame = frame.withPixelBrightness(x, 7, core.coerceAtLeast(28))
        frame = frame.withPixelBrightness(x, 8, core.coerceAtLeast(28))
        frame = frame.withPixelBrightness(x, 9, tip.coerceAtLeast(14))
        return frame
    }

    private fun drawTunerLetter(
        input: GlyphFrame,
        char: Char,
        xOffset: Int,
        yOffset: Int,
        intensity: Int
    ): GlyphFrame {
        var frame = input
        val glyph = tunerLetterGlyphs[GlyphTextRenderer.transliterate(char.toString()).firstOrNull() ?: char]
            ?: tunerLetterGlyphs['?']
            ?: return frame
        glyph.forEachIndexed { y, row ->
            row.forEachIndexed { x, pixel ->
                if (pixel == '1') {
                    frame = frame.withPixelBrightness(xOffset + x, yOffset + y, intensity)
                }
            }
        }
        return frame
    }

    private fun drawTunerDigit(
        input: GlyphFrame,
        char: Char,
        xOffset: Int,
        yOffset: Int,
        intensity: Int
    ): GlyphFrame {
        var frame = input
        val glyph = tunerDigitGlyphs[char] ?: return frame
        glyph.forEachIndexed { y, row ->
            row.forEachIndexed { x, pixel ->
                if (pixel == '1') {
                    frame = frame.withPixelBrightness(xOffset + x, yOffset + y, intensity)
                }
            }
        }
        return frame
    }

    private val tunerLetterGlyphs: Map<Char, List<String>> = mapOf(
        '?' to listOf("1110", "0001", "0110", "0000", "0100"),
        'A' to listOf("0110", "1001", "1111", "1001", "1001"),
        'B' to listOf("1110", "1001", "1110", "1001", "1110"),
        'C' to listOf("0111", "1000", "1000", "1000", "0111"),
        'D' to listOf("1110", "1001", "1001", "1001", "1110"),
        'E' to listOf("1111", "1000", "1110", "1000", "1111"),
        'F' to listOf("1111", "1000", "1110", "1000", "1000"),
        'G' to listOf("0111", "1000", "1011", "1001", "0111")
    )

    private val tunerDigitGlyphs: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111")
    )
}
