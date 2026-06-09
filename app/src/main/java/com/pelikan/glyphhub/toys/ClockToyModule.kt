package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

class ClockToyModule : BaseGlyphToyModule() {
    override val id = "clock"
    override val name = "Clock Toy"
    override val shortName = "TIME"
    override val description = "Readable always-on time display for the 13x13 matrix."
    override val iconAsset = "glyphs/clock/clock_idle.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("format24h", "24 hour", "Use 24 hour time.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("blinkColon", "Blink colon", "Pulse separators once per second.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("showBatteryEverySeconds", "Battery interval", "Reserved for periodic battery glance.", ToySettingType.Int, "0", 0, 3600, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "76", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition(
                "mode",
                "Mode",
                "Clock rendering style.",
                ToySettingType.Choice,
                "digits",
                options = listOf(
                    ToySettingOption("digits", "Stacked digits"),
                    ToySettingOption("vertical", "Vertical split"),
                    ToySettingOption("pair", "Large pair"),
                    ToySettingOption("orbit", "Minute orbit"),
                    ToySettingOption("analog", "Analog"),
                    ToySettingOption("binary", "Binary BCD"),
                    ToySettingOption("dot_clock", "Needle dots")
                ),
                scope = ToySettingScope.QUICK
            ),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    override fun onTick(deltaMs: Long): GlyphFrame =
        ClockToyVisuals.draw(
            now = LocalTime.now(),
            mode = getSettings().choice("mode", "digits"),
            format24h = getSettings().bool("format24h", true),
            blinkColon = getSettings().bool("blinkColon", true),
            brightness = getSettings().int("brightness", 76).coerceIn(0, 100)
        )
}

internal object ClockToyVisuals {
    val supportedModes = listOf("digits", "vertical", "pair", "orbit", "analog", "binary", "dot_clock")

    fun draw(
        now: LocalTime,
        mode: String,
        format24h: Boolean,
        blinkColon: Boolean,
        brightness: Int
    ): GlyphFrame {
        val safeBrightness = brightness.coerceIn(0, 100)
        val hour = displayHour(now, format24h)
        val normalizedMode = when (mode) {
            "stacked", "digital", "abstract" -> "digits"
            "dot" -> "dot_clock"
            else -> mode
        }
        val frame = when (normalizedMode) {
            "vertical" -> verticalSplit(hour, now.minute, now.second, blinkColon, safeBrightness)
            "pair" -> largePair(hour, now.minute, now.second, safeBrightness)
            "orbit" -> minuteOrbit(hour, now.minute, now.second, safeBrightness)
            "analog" -> analogClock(now, safeBrightness)
            "binary" -> binaryClock(hour, now.minute, safeBrightness)
            "dot_clock" -> dotClock(now, safeBrightness)
            else -> stackedDigits(hour, now.minute, now.second, blinkColon, safeBrightness)
        }
        return GlyphMatrixLayout.mask(frame)
    }

    private fun stackedDigits(hour: Int, minute: Int, second: Int, blinkColon: Boolean, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = frame.overlay(drawTwoDigit3x5(hour, brightness, yOffset = 1, spacing = 2))
        frame = frame.overlay(drawTwoDigit3x5(minute, brightness, yOffset = 7, spacing = 2))
        return frame
    }

    private fun verticalSplit(hour: Int, minute: Int, second: Int, blinkColon: Boolean, brightness: Int): GlyphFrame {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        var frame = GlyphFrame.empty13(brightness)
        frame = frame.overlay(drawDigit3x5(h[0], brightness), 1, 1)
        frame = frame.overlay(drawDigit3x5(h[1], brightness), 1, 7)
        frame = frame.overlay(drawDigit3x5(m[0], brightness), 9, 1)
        frame = frame.overlay(drawDigit3x5(m[1], brightness), 9, 7)
        val center = if (blinkColon && second % 2 == 1) 36 else 76
        listOf(6 to 4, 6 to 6, 6 to 8).forEach { (x, y) ->
            frame = frame.withPixelBrightness(x, y, center)
        }
        return frame
    }

    private fun largePair(hour: Int, minute: Int, second: Int, brightness: Int): GlyphFrame {
        val showingHour = second % 4 < 2
        var frame = readableTwoDigit(if (showingHour) hour else minute, brightness, yOffset = 4)
        val progress = if (showingHour) hour % 12 / 12f else minute / 60f
        frame = drawOuterTickProgress(frame, progress, if (showingHour) 58 else 82, includeDimBase = false)
        val markerY = if (showingHour) 2 else 10
        listOf(5 to markerY, 6 to markerY, 7 to markerY).forEach { (x, y) ->
            frame = frame.withPixelBrightness(x, y, 72)
        }
        return frame
    }

    private fun minuteOrbit(hour: Int, minute: Int, second: Int, brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = drawOuterTickProgress(frame, minute / 60f, 92, includeDimBase = true)
        frame = frame.overlay(drawTwoDigit3x5(hour, brightness, yOffset = 4, spacing = 1))
        val secondIndex = ((second / 60f) * outerRing.size).roundToInt().coerceIn(0, outerRing.lastIndex)
        val (x, y) = outerRing[secondIndex]
        frame = frame.withPixelBrightness(x, y, 100)
        return frame
    }

    private fun analogClock(now: LocalTime, brightness: Int): GlyphFrame {
        val hourAngle = ((now.hour % 12) + now.minute / 60.0) * 30.0
        val minuteAngle = now.minute * 6.0
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = GlyphDesignSystem.fullRingRadius, intensity = 34, thickness = 0.48)
        listOf(6 to 1, 11 to 6, 6 to 11, 1 to 6).forEach { (x, y) ->
            frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 70)
        }
        frame = GlyphDesignSystem.drawNeedle(frame, hourAngle, length = 3.25, intensity = 82, thickness = 0.5)
        frame = GlyphDesignSystem.drawNeedle(frame, minuteAngle, length = GlyphDesignSystem.fullNeedleLength, intensity = 100, thickness = 0.36)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 100)
        val secondIndex = ((now.second / 60f) * outerRing.size).roundToInt().coerceIn(0, outerRing.lastIndex)
        val (sx, sy) = outerRing[secondIndex]
        frame = frame.withPixelBrightness(sx, sy, 58)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun binaryClock(hour: Int, minute: Int, brightness: Int): GlyphFrame {
        val values = listOf(hour / 10, hour % 10, minute / 10, minute % 10)
        val columns = listOf(1, 4, 8, 11)
        var frame = GlyphFrame.empty13(brightness)
        columns.forEach { x ->
            for (bit in 0..3) {
                frame = frame.withPixelBrightness(x, 9 - bit * 2, 14)
            }
        }
        values.forEachIndexed { column, value ->
            for (bit in 0..3) {
                if ((value and (1 shl bit)) != 0) {
                    frame = frame.withPixelBrightness(columns[column], 9 - bit * 2, 100)
                    frame = frame.withPixelBrightness(columns[column] + 1, 9 - bit * 2, 64)
                }
            }
        }
        listOf(6 to 3, 6 to 5, 6 to 7, 6 to 9).forEach { (x, y) ->
            frame = frame.withPixelBrightness(x, y, 36)
        }
        return frame
    }

    private fun dotClock(now: LocalTime, brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = GlyphDesignSystem.fullRingRadius, intensity = 28, thickness = 0.48)
        frame = GlyphDesignSystem.drawNeedle(frame, (now.hour % 12) * 30.0, length = 4.2, intensity = 70, thickness = 0.28)
        frame = GlyphDesignSystem.drawNeedle(frame, now.minute * 6.0, length = GlyphDesignSystem.fullNeedleLength, intensity = 100, thickness = 0.28)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 86)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun drawOuterTickProgress(frame: GlyphFrame, progress: Float, intensity: Int, includeDimBase: Boolean): GlyphFrame {
        var output = frame
        val active = (progress.coerceIn(0f, 1f) * outerRing.size).roundToInt().coerceIn(0, outerRing.size)
        outerRing.forEachIndexed { index, point ->
            val (x, y) = point
            val value = when {
                index < active -> intensity
                includeDimBase -> 18
                else -> 0
            }
            if (value > 0) output = output.withPixelBrightness(x, y, value)
        }
        return output
    }

    private fun drawTwoDigit3x5(value: Int, brightness: Int, yOffset: Int, spacing: Int): GlyphFrame {
        val text = value.coerceIn(0, 99).toString().padStart(2, '0')
        val width = GlyphTextRenderer.measureText3x5(text, spacing = spacing)
        val xOffset = if (width == 8) 3 else ((GlyphFrame.MATRIX_SIZE - width) / 2).coerceAtLeast(0)
        return GlyphTextRenderer.drawText3x5(text, brightness, yOffset = yOffset, xOffset = xOffset, spacing = spacing)
    }

    private fun drawDigit3x5(digit: Char, brightness: Int): GlyphFrame =
        GlyphTextRenderer.drawText3x5(digit.toString(), brightness, yOffset = 0, xOffset = 0, spacing = 0)

    private fun readableTwoDigit(value: Int, brightness: Int, yOffset: Int): GlyphFrame {
        val text = value.coerceIn(0, 99).toString().padStart(2, '0')
        val glyphs = text.map { readableDigitRows(it) }
        val width = glyphs.sumOf { it.first().length } + 1
        var xOffset = ((GlyphFrame.MATRIX_SIZE - width) / 2).coerceAtLeast(0)
        var frame = GlyphFrame.empty13(brightness)
        glyphs.forEach { rows ->
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    if (bit == '1') frame = frame.withPixelBrightness(xOffset + x, yOffset + y, 100)
                }
            }
            xOffset += rows.first().length + 1
        }
        return frame
    }

    private fun readableDigitRows(digit: Char): List<String> =
        when (digit) {
            '0' -> listOf("1110", "1001", "1001", "1001", "1110")
            '1' -> listOf("0010", "0110", "0010", "0010", "0111")
            '2' -> listOf("1110", "0001", "1110", "1000", "1111")
            '3' -> listOf("1110", "0001", "0110", "0001", "1110")
            '4' -> listOf("1001", "1001", "1111", "0001", "0001")
            '5' -> listOf("1111", "1000", "1110", "0001", "1110")
            '6' -> listOf("0111", "1000", "1110", "1001", "1110")
            '7' -> listOf("1111", "0001", "0010", "0100", "0100")
            '8' -> listOf("1110", "1001", "1110", "1001", "1110")
            else -> listOf("1110", "1001", "1111", "0001", "1110")
        }

    private fun displayHour(now: LocalTime, format24h: Boolean): Int =
        if (format24h) now.hour else ((now.hour + 11) % 12) + 1

    private val outerRing = buildList {
        for (y in 0 until GlyphFrame.MATRIX_SIZE) {
            for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val edge = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                    .any { (nx, ny) -> !GlyphMatrixLayout.isPhysicalLed(nx, ny) }
                if (edge) add(x to y)
            }
        }
    }.sortedBy { (x, y) ->
        val angle = atan2((y - 6).toDouble(), (x - 6).toDouble()) + PI / 2.0
        if (angle < 0.0) angle + PI * 2.0 else angle
    }
}
