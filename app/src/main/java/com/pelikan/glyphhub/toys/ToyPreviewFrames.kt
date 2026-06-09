package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettings
import java.time.LocalTime

object ToyPreviewFrames {
    fun preview(module: GlyphToyModule, settings: ToySettings): GlyphFrame {
        val brightness = settings.int("brightness", settings.int("idleBrightness", 90)).coerceIn(82, 100)
        return when (module.id) {
            "dice" -> dice(brightness)
            "coin" -> coin(brightness)
            "rps" -> rps(brightness)
            "timer" -> timer(brightness)
            "school_class_timer" -> school(brightness)
            "level" -> level(brightness)
            "lux_meter" -> lux(brightness)
            "compass" -> compass(brightness)
            "tuner" -> tuner(brightness)
            "clock" -> clock(brightness)
            "battery" -> battery(brightness)
            "eye" -> eye(brightness)
            "maze" -> maze(brightness)
            "weather" -> weather(brightness)
            "pixel_art" -> GlyphIconLibrary.frame(
                id = settings.choice("selectedGlyphAsset", "heart"),
                customRows = settings.text("customGlyphRows", ""),
                brightness = brightness
            )
            "text_scroll" -> text(brightness)
            "idle_default" -> idle(brightness)
            else -> fallback(module.shortName, brightness)
        }
    }

    fun fallbackPreview(label: String, brightness: Int = 90): GlyphFrame =
        fallback(label, brightness.coerceIn(82, 100))

    private fun dice(brightness: Int): GlyphFrame =
        GlyphIconLibrary.frame("dice", customRows = "", brightness = brightness)

    private fun coin(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000011100000",
            "0001111111000",
            "0011001001100",
            "0110001000110",
            "0100001000010",
            "1100001000011",
            "1100001000011",
            "1100001000011",
            "0100001000010",
            "0110001000110",
            "0011001001100",
            "0001111111000",
            "0000011100000"
        )

    private fun rps(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000000000000",
            "0000011100000",
            "0000111110000",
            "0010110110100",
            "0110110110110",
            "0010110110100",
            "0000010100000",
            "0000100010000",
            "0001000001000",
            "0010000000100",
            "0000000000000",
            "0000000000000",
            "0000000000000"
        )

    private fun timer(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000011100000",
            "0000111110000",
            "0001100011000",
            "0010001000100",
            "0100001000010",
            "0100011000010",
            "0100110000010",
            "0101100000010",
            "0011000000100",
            "0001111111000",
            "0000011100000",
            "0000111110000",
            "0000000000000"
        )

    private fun school(brightness: Int): GlyphFrame =
        frameFromPoints(buildSet {
            for (y in 0 until GlyphFrame.MATRIX_SIZE) {
                for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                    if (!com.pelikan.glyphhub.glyph.GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                    val edge = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                        .any { (nx, ny) -> !com.pelikan.glyphhub.glyph.GlyphMatrixLayout.isPhysicalLed(nx, ny) }
                    if (edge) add(x to y)
                }
            }
            for (y in 4..8) {
                add(4 to y)
                add(5 to y)
                add(7 to y)
                add(8 to y)
            }
        }, brightness)

    private fun level(brightness: Int): GlyphFrame =
        LevelToyVisuals.draw(
            reading = LevelReading(
                mode = LevelVisualMode.Bubble,
                bubbleX = 0.28f,
                bubbleY = -0.18f,
                normalizedError = 0.32f,
                centered = false
            ),
            brightness = brightness,
            sensitivity = 0.82f,
            showGuides = true,
            pulseProgress = 1f
        )

    private fun lux(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000001000000",
            "0000101010000",
            "0010011100100",
            "0001011101000",
            "0100111110010",
            "0001111111000",
            "1011111111101",
            "0001111111000",
            "0100111110010",
            "0001011101000",
            "0010011100100",
            "0000101010000",
            "0000001000000"
        )

    private fun compass(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000001000000",
            "0000011100000",
            "0000111110000",
            "0000011100000",
            "0000011100000",
            "0000011100000",
            "1000011100001",
            "0000010100000",
            "0000010100000",
            "0000010100000",
            "0000010100000",
            "0000001000000",
            "0000000000000"
        )

    private fun tuner(brightness: Int): GlyphFrame =
        TunerToyVisuals.drawTuner("E2", cents = -18f, toleranceCents = 5, brightness = brightness)

    private fun clock(brightness: Int): GlyphFrame =
        ClockToyVisuals.draw(
            now = LocalTime.of(12, 34, 0),
            mode = "digits",
            format24h = true,
            blinkColon = false,
            brightness = brightness
        )

    private fun battery(brightness: Int): GlyphFrame =
        com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary.battery(level = 72, brightness = brightness)

    private fun eye(brightness: Int): GlyphFrame =
        frameFromRows(
            listOf(
                "0000000000000",
                "0000111110000",
                "0011100011100",
                "0110000000110",
                "0100011100010",
                "1100111110011",
                "1100110110011",
                "1100111110011",
                "0100011100010",
                "0110000000110",
                "0011100011100",
                "0000111110000",
                "0000000000000"
            ),
            brightness
        )

    private fun maze(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000111110000",
            "0011000011100",
            "0110111000110",
            "0100100010010",
            "1100101110011",
            "1000101000001",
            "1011101011101",
            "1000001000101",
            "1101111010101",
            "0100000010100",
            "0110111110110",
            "0011100001100",
            "0000111110000"
        )

    private fun weather(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000001000000",
            "0000100010000",
            "0010000000100",
            "0000011100000",
            "0100111110010",
            "0001111111000",
            "1001111111001",
            "0001111111000",
            "0100111110010",
            "0000011100000",
            "0010000000100",
            "0000100010000",
            "0000001000000"
        )

    private fun text(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000000000000",
            "0000111110000",
            "0011000001100",
            "0110011100110",
            "0100000000010",
            "1101110111011",
            "1100000000011",
            "1101011101011",
            "0100000000010",
            "0110011100110",
            "0011000001100",
            "0000111110000",
            "0000000000000"
        )

    private fun idle(brightness: Int): GlyphFrame =
        rows(
            brightness,
            "0000000000000",
            "0000111110000",
            "0011000001100",
            "0110000000110",
            "0100000000010",
            "1100000000011",
            "1100001000011",
            "1100000000011",
            "0100000000010",
            "0110000000110",
            "0011000001100",
            "0000111110000",
            "0000000000000"
        )

    private fun fallback(label: String, brightness: Int): GlyphFrame =
        drawCenteredText3x5(label.take(3), brightness, yOffset = 4)

    private fun rows(brightness: Int, vararg rows: String): GlyphFrame =
        frameFromRows(rows.toList(), brightness)
}
