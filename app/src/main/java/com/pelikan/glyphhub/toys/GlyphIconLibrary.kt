package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.settings.ToySettingOption

data class GlyphIconPreset(
    val id: String,
    val label: String,
    val render: (brightness: Int) -> GlyphFrame
)

object GlyphIconLibrary {
    val presets: List<GlyphIconPreset> = listOf(
        GlyphIconPreset("heart", "Heart") { heart(it) },
        GlyphIconPreset("community_heartbeat", "Heartbeat") { CommunityGlyphVisuals.fullHeart(it) },
        GlyphIconPreset("smile", "Smile") { smile(it, happy = true) },
        GlyphIconPreset("sad", "Sad") { smile(it, happy = false) },
        GlyphIconPreset("check", "Check") { check(it) },
        GlyphIconPreset("x_mark", "X mark") { xMark(it) },
        GlyphIconPreset("sun", "Sun") { sun(it) },
        GlyphIconPreset("moon", "Moon") { moon(it) },
        GlyphIconPreset("star", "Star") { star(it) },
        GlyphIconPreset("bell", "Bell") { bell(it) },
        GlyphIconPreset("music", "Music") { music(it) },
        GlyphIconPreset("wifi", "Wi-Fi") { wifi(it) },
        GlyphIconPreset("bolt", "Bolt") { bolt(it) },
        GlyphIconPreset("message", "Message") { message(it) },
        GlyphIconPreset("play", "Play") { play(it) },
        GlyphIconPreset("pause", "Pause") { pause(it) },
        GlyphIconPreset("stop", "Stop") { stop(it) },
        GlyphIconPreset("arrow_up", "Arrow up") { arrow(it, Direction.Up) },
        GlyphIconPreset("arrow_down", "Arrow down") { arrow(it, Direction.Down) },
        GlyphIconPreset("arrow_left", "Arrow left") { arrow(it, Direction.Left) },
        GlyphIconPreset("arrow_right", "Arrow right") { arrow(it, Direction.Right) },
        GlyphIconPreset("home", "Home") { home(it) },
        GlyphIconPreset("lock", "Lock") { lock(it, open = false) },
        GlyphIconPreset("unlock", "Unlock") { lock(it, open = true) },
        GlyphIconPreset("flame", "Flame") { flame(it) },
        GlyphIconPreset("drop", "Drop") { drop(it) },
        GlyphIconPreset("snow", "Snow") { snow(it) },
        GlyphIconPreset("cloud", "Cloud") { cloud(it) },
        GlyphIconPreset("umbrella", "Umbrella") { umbrella(it) },
        GlyphIconPreset("dice", "Dice") { dice(it) },
        GlyphIconPreset("coin", "Coin") { coin(it) },
        GlyphIconPreset("clock", "Clock") { clock(it) },
        GlyphIconPreset("battery", "Battery") { battery(it) },
        GlyphIconPreset("compass", "Compass") { compass(it) },
        GlyphIconPreset("eye", "Eye") { eye(it) },
        GlyphIconPreset("community_rings", "Rings") { CommunityGlyphVisuals.ringsFrame(6, it) },
        GlyphIconPreset("community_equalizer", "Equalizer") { CommunityGlyphVisuals.equalizerFrame(6, it) },
        GlyphIconPreset("community_fire", "Fire") { CommunityGlyphVisuals.fireFrame(6, it) },
        GlyphIconPreset("community_spinner", "Spinner") { CommunityGlyphVisuals.spinnerFrame(6, it) },
        GlyphIconPreset("community_stars", "Stars") { CommunityGlyphVisuals.starsFrame(6, it) },
        GlyphIconPreset("community_rocket", "Rocket") { CommunityGlyphVisuals.rocketFrame(6, it) },
        GlyphIconPreset("plus", "Plus") { plus(it) },
        GlyphIconPreset("grid", "Grid") { grid(it) }
    )

    fun settingOptions(): List<ToySettingOption> =
        listOf(ToySettingOption("custom", "Custom")) + presets.map { ToySettingOption(it.id, it.label) }

    fun frame(id: String, customRows: String, brightness: Int): GlyphFrame {
        if (id == "custom") {
            val custom = customFrame(customRows, brightness)
            if (custom != null) return custom
        }
        return (presets.firstOrNull { it.id == id } ?: presets.first()).render(brightness.coerceIn(0, 100))
    }

    fun animatedFrame(id: String, customRows: String, brightness: Int, elapsedMs: Long, frameDurationMs: Long): GlyphFrame =
        CommunityGlyphVisuals.animatedPresetFrame(
            id = id,
            elapsedMs = elapsedMs,
            frameDurationMs = frameDurationMs,
            brightness = brightness.coerceIn(0, 100)
        ) ?: frame(id, customRows, brightness)

    fun rowsForEditor(id: String, customRows: String): List<String> =
        toBinaryRows(frame(id, customRows, brightness = 100))

    fun encodeRows(rows: List<String>): String =
        normalizeRows(rows).joinToString("/")

    fun normalizeRows(rows: List<String>): List<String> =
        rows.take(GlyphFrame.MATRIX_SIZE).let { taken ->
            if (taken.size == GlyphFrame.MATRIX_SIZE) {
                taken.mapIndexed { y, row ->
                    row.padEnd(GlyphFrame.MATRIX_SIZE, '0')
                        .take(GlyphFrame.MATRIX_SIZE)
                        .mapIndexed { x, value ->
                            if (value == '1' && GlyphMatrixLayout.isPhysicalLed(x, y)) '1' else '0'
                        }
                        .joinToString("")
                }
            } else {
                List(GlyphFrame.MATRIX_SIZE) { "0".repeat(GlyphFrame.MATRIX_SIZE) }
            }
        }

    private fun customFrame(encodedRows: String, brightness: Int): GlyphFrame? {
        val rows = encodedRows.split("/")
            .filter { it.length == GlyphFrame.MATRIX_SIZE }
            .take(GlyphFrame.MATRIX_SIZE)
        if (rows.size != GlyphFrame.MATRIX_SIZE) return null
        return rows(rows, brightness)
    }

    private fun toBinaryRows(frame: GlyphFrame): List<String> =
        List(GlyphFrame.MATRIX_SIZE) { y ->
            buildString {
                for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                    append(if (frame.intensityAt(x, y) > 0 && GlyphMatrixLayout.isPhysicalLed(x, y)) '1' else '0')
                }
            }
        }

    private fun rows(rows: List<String>, brightness: Int): GlyphFrame =
        GlyphMatrixLayout.mask(GlyphFrame.fromBinaryRows(rows, brightness.coerceIn(0, 100)))

    private fun rows(brightness: Int, vararg rows: String): GlyphFrame =
        rows(rows.toList(), brightness)

    private fun points(brightness: Int, block: MutableSet<Pair<Int, Int>>.() -> Unit): GlyphFrame {
        val pointSet = mutableSetOf<Pair<Int, Int>>()
        pointSet.block()
        var frame = GlyphFrame.empty13(brightness)
        pointSet.forEach { (x, y) ->
            if (GlyphMatrixLayout.isPhysicalLed(x, y)) frame = frame.withPixelBrightness(x, y, 100)
        }
        return frame
    }

    private fun MutableSet<Pair<Int, Int>>.pixel(x: Int, y: Int) {
        add(x to y)
    }

    private fun MutableSet<Pair<Int, Int>>.hline(x0: Int, x1: Int, y: Int) {
        for (x in x0..x1) add(x to y)
    }

    private fun MutableSet<Pair<Int, Int>>.vline(x: Int, y0: Int, y1: Int) {
        for (y in y0..y1) add(x to y)
    }

    private fun MutableSet<Pair<Int, Int>>.rect(x0: Int, y0: Int, x1: Int, y1: Int) {
        hline(x0, x1, y0)
        hline(x0, x1, y1)
        vline(x0, y0, y1)
        vline(x1, y0, y1)
    }

    private fun MutableSet<Pair<Int, Int>>.fillRect(x0: Int, y0: Int, x1: Int, y1: Int) {
        for (y in y0..y1) {
            for (x in x0..x1) add(x to y)
        }
    }

    private fun MutableSet<Pair<Int, Int>>.diagonal(x0: Int, y0: Int, dx: Int, dy: Int, length: Int) {
        for (step in 0 until length) add((x0 + dx * step) to (y0 + dy * step))
    }

    private fun heart(brightness: Int): GlyphFrame = CommunityGlyphVisuals.fullHeart(brightness)

    private fun plus(brightness: Int): GlyphFrame = points(brightness) {
        vline(6, 1, 11)
        hline(1, 11, 6)
    }

    private fun grid(brightness: Int): GlyphFrame = points(brightness) {
        for (i in 0..12 step 3) {
            hline(0, 12, i)
            vline(i, 0, 12)
        }
    }

    private fun check(brightness: Int): GlyphFrame = points(brightness) {
        diagonal(2, 7, 1, 1, 3)
        diagonal(5, 9, 1, -1, 6)
        pixel(3, 8)
        pixel(9, 4)
    }

    private fun xMark(brightness: Int): GlyphFrame = points(brightness) {
        diagonal(3, 3, 1, 1, 7)
        diagonal(9, 3, -1, 1, 7)
        diagonal(4, 3, 1, 1, 6)
        diagonal(8, 3, -1, 1, 6)
    }

    private fun smile(brightness: Int, happy: Boolean): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = 5.35, intensity = 88, thickness = 0.62)
        frame = GlyphDesignSystem.drawDot(frame, 4, 5, 100)
        frame = GlyphDesignSystem.drawDot(frame, 8, 5, 100)
        frame = if (happy) {
            GlyphDesignSystem.drawArcApprox(frame, radius = 3.0, startDegrees = 118.0, sweepDegrees = 124.0, intensity = 100, thickness = 0.55)
        } else {
            GlyphDesignSystem.drawArcApprox(frame, radius = 3.0, startDegrees = 298.0, sweepDegrees = 124.0, intensity = 100, thickness = 0.55)
        }
        return GlyphMatrixLayout.mask(frame)
    }

    private fun sun(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = 2.2, intensity = 100, thickness = 0.95)
        listOf(6 to 0, 6 to 1, 6 to 11, 6 to 12, 0 to 6, 1 to 6, 11 to 6, 12 to 6, 2 to 2, 10 to 2, 2 to 10, 10 to 10)
            .forEach { (x, y) -> frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 86) }
        return GlyphMatrixLayout.mask(frame)
    }

    private fun moon(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000011100000",
        "0001111000000",
        "0011110000000",
        "0111100000000",
        "0111000000000",
        "1111000000000",
        "1111000000000",
        "1111000000000",
        "0111000000000",
        "0111100000000",
        "0011110000000",
        "0001111000000",
        "0000011100000"
    )

    private fun star(brightness: Int): GlyphFrame = points(brightness) {
        pixel(6, 1)
        pixel(5, 4); pixel(6, 4); pixel(7, 4)
        hline(2, 10, 5)
        diagonal(3, 6, 1, 1, 4)
        diagonal(9, 6, -1, 1, 4)
        pixel(6, 7)
        pixel(5, 9); pixel(7, 9)
    }

    private fun bell(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000001000000",
        "0000011100000",
        "0000111110000",
        "0001111111000",
        "0001111111000",
        "0011111111100",
        "0011111111100",
        "0011111111100",
        "0111111111110",
        "0001111111000",
        "0000011100000",
        "0000001000000",
        "0000000000000"
    )

    private fun music(brightness: Int): GlyphFrame = points(brightness) {
        vline(8, 2, 8)
        hline(8, 11, 2)
        vline(11, 2, 7)
        fillRect(5, 8, 7, 10)
        fillRect(8, 7, 10, 9)
    }

    private fun wifi(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawArcApprox(GlyphFrame.empty13(brightness), radius = 5.35, startDegrees = 228.0, sweepDegrees = 84.0, intensity = 80, thickness = 0.55)
        frame = GlyphDesignSystem.drawArcApprox(frame, radius = 3.7, startDegrees = 232.0, sweepDegrees = 76.0, intensity = 90, thickness = 0.55)
        frame = GlyphDesignSystem.drawArcApprox(frame, radius = 2.1, startDegrees = 238.0, sweepDegrees = 64.0, intensity = 100, thickness = 0.55)
        frame = GlyphDesignSystem.drawSoftDot(frame, 6, 9, 100)
        return GlyphMatrixLayout.mask(frame)
    }

    private fun bolt(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000000100000",
        "0000001100000",
        "0000011000000",
        "0000110000000",
        "0001111110000",
        "0000011000000",
        "0000110000000",
        "0001100000000",
        "0011000000000",
        "0110000000000",
        "0100000000000",
        "0000000000000",
        "0000000000000"
    )

    private fun message(brightness: Int): GlyphFrame = points(brightness) {
        rect(2, 3, 10, 8)
        pixel(4, 9)
        pixel(3, 10)
        hline(4, 8, 5)
        hline(4, 7, 6)
    }

    private fun play(brightness: Int): GlyphFrame = points(brightness) {
        for (y in 2..10) {
            for (x in 4..(4 + (y - 2).coerceAtMost(10 - y))) add(x to y)
        }
    }

    private fun pause(brightness: Int): GlyphFrame = points(brightness) {
        fillRect(4, 2, 5, 10)
        fillRect(8, 2, 9, 10)
    }

    private fun stop(brightness: Int): GlyphFrame = points(brightness) {
        fillRect(3, 3, 9, 9)
    }

    private fun arrow(brightness: Int, direction: Direction): GlyphFrame = points(brightness) {
        when (direction) {
            Direction.Up -> {
                vline(6, 2, 11)
                diagonal(6, 2, -1, 1, 4)
                diagonal(6, 2, 1, 1, 4)
            }
            Direction.Down -> {
                vline(6, 1, 10)
                diagonal(6, 10, -1, -1, 4)
                diagonal(6, 10, 1, -1, 4)
            }
            Direction.Left -> {
                hline(2, 11, 6)
                diagonal(2, 6, 1, -1, 4)
                diagonal(2, 6, 1, 1, 4)
            }
            Direction.Right -> {
                hline(1, 10, 6)
                diagonal(10, 6, -1, -1, 4)
                diagonal(10, 6, -1, 1, 4)
            }
        }
    }

    private fun home(brightness: Int): GlyphFrame = points(brightness) {
        diagonal(6, 2, -1, 1, 5)
        diagonal(6, 2, 1, 1, 5)
        rect(3, 6, 9, 10)
        vline(6, 8, 10)
    }

    private fun lock(brightness: Int, open: Boolean): GlyphFrame = points(brightness) {
        rect(3, 6, 9, 10)
        if (open) {
            rect(5, 2, 10, 6)
            pixel(10, 5)
        } else {
            rect(4, 2, 8, 6)
        }
        pixel(6, 8)
    }

    private fun flame(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000001000000",
        "0000011000000",
        "0000110100000",
        "0001100110000",
        "0011001111000",
        "0011011111000",
        "0111111111100",
        "0111111111100",
        "0011111111000",
        "0001111110000",
        "0000111100000",
        "0000011000000",
        "0000000000000"
    )

    private fun drop(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000001000000",
        "0000011100000",
        "0000111110000",
        "0001111111000",
        "0011111111100",
        "0111111111110",
        "0111111111110",
        "0011111111100",
        "0001111111000",
        "0000111110000",
        "0000011100000",
        "0000000000000",
        "0000000000000"
    )

    private fun snow(brightness: Int): GlyphFrame = points(brightness) {
        vline(6, 1, 11)
        hline(1, 11, 6)
        diagonal(2, 2, 1, 1, 9)
        diagonal(10, 2, -1, 1, 9)
    }

    private fun cloud(brightness: Int): GlyphFrame = points(brightness) {
        hline(2, 10, 8)
        hline(3, 10, 9)
        hline(4, 9, 10)
        pixel(2, 7); pixel(3, 6); pixel(4, 5); pixel(5, 5)
        pixel(6, 4); pixel(7, 4); pixel(8, 5); pixel(9, 6); pixel(10, 7)
    }

    private fun umbrella(brightness: Int): GlyphFrame = points(brightness) {
        hline(2, 10, 6)
        diagonal(2, 6, 1, -1, 5)
        diagonal(10, 6, -1, -1, 5)
        vline(6, 6, 11)
        pixel(7, 11); pixel(8, 10)
    }

    private fun dice(brightness: Int): GlyphFrame = rows(
        brightness,
        "0000000000000",
        "0000000000000",
        "0011111111100",
        "0010000000100",
        "0010100010100",
        "0010000000100",
        "0010001000100",
        "0010000000100",
        "0010100010100",
        "0010000000100",
        "0011111111100",
        "0000000000000",
        "0000000000000"
    )

    private fun coin(brightness: Int): GlyphFrame =
        GlyphMatrixLayout.mask(GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = 5.35, intensity = 100, thickness = 0.82)
            .overlay(drawCenteredText3x5("C", brightness, yOffset = 4))
        )

    private fun clock(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = 5.35, intensity = 86, thickness = 0.62)
        frame = GlyphDesignSystem.drawNeedle(frame, 0.0, length = 4.8, intensity = 100, thickness = 0.4)
        frame = GlyphDesignSystem.drawNeedle(frame, 90.0, length = 3.2, intensity = 86, thickness = 0.4)
        return GlyphMatrixLayout.mask(frame)
    }

    private fun battery(brightness: Int): GlyphFrame =
        com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary.battery(level = 72, brightness = brightness)

    private fun compass(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = 5.35, intensity = 38, thickness = 0.5)
        frame = GlyphDesignSystem.drawNeedle(frame, 0.0, length = 5.5, intensity = 100, thickness = 0.45)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 80)
        return GlyphMatrixLayout.mask(frame)
    }

    private fun eye(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawArcApprox(GlyphFrame.empty13(brightness), radius = 5.2, startDegrees = -118.0, sweepDegrees = 236.0, intensity = 95)
        frame = GlyphDesignSystem.drawArcApprox(frame, radius = 5.2, startDegrees = 212.0, sweepDegrees = 136.0, intensity = 88)
        frame = GlyphDesignSystem.drawSoftDot(frame, 6, 6, 100)
        return GlyphMatrixLayout.mask(frame)
    }

    private enum class Direction {
        Up,
        Down,
        Left,
        Right
    }
}
