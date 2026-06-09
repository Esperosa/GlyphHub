package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ClockToyVisualsTest {
    @Test
    fun allClockModesRenderInsidePhysicalMatrix() {
        ClockToyVisuals.supportedModes.forEach { mode ->
            val frame = ClockToyVisuals.draw(
                now = LocalTime.of(23, 59, 31),
                mode = mode,
                format24h = true,
                blinkColon = true,
                brightness = 90
            )

            assertTrue(frame.litPixelCount() > 0, "$mode produced an empty frame")
            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    if (frame.intensityAt(x, y) > 0) {
                        assertTrue(GlyphMatrixLayout.isPhysicalLed(x, y), "$mode lit non-physical LED at $x,$y")
                    }
                }
            }
        }
    }

    @Test
    fun stackedDigitsKeepTwoPixelGapBetweenDigits() {
        val frame = ClockToyVisuals.draw(
            now = LocalTime.of(12, 34, 0),
            mode = "digits",
            format24h = true,
            blinkColon = false,
            brightness = 90
        )

        for (y in 1..5) {
            assertEquals(0, frame.intensityAt(6, y), "top hour gap x=6 y=$y")
            assertEquals(0, frame.intensityAt(7, y), "top hour gap x=7 y=$y")
        }
        for (y in 7..11) {
            assertEquals(0, frame.intensityAt(6, y), "bottom minute gap x=6 y=$y")
            assertEquals(0, frame.intensityAt(7, y), "bottom minute gap x=7 y=$y")
        }
        for (x in 0 until frame.width) {
            assertEquals(0, frame.intensityAt(x, 6), "stacked clock separator row should be empty at x=$x")
        }
    }

    @Test
    fun twelveHourModeMapsMidnightAndAfternoon() {
        val midnight = ClockToyVisuals.draw(
            now = LocalTime.of(0, 5, 0),
            mode = "digits",
            format24h = false,
            blinkColon = false,
            brightness = 90
        )
        val afternoon = ClockToyVisuals.draw(
            now = LocalTime.of(13, 5, 0),
            mode = "digits",
            format24h = false,
            blinkColon = false,
            brightness = 90
        )
        val oneTwentyFourHour = ClockToyVisuals.draw(
            now = LocalTime.of(1, 5, 0),
            mode = "digits",
            format24h = true,
            blinkColon = false,
            brightness = 90
        )
        val thirteenTwentyFourHour = ClockToyVisuals.draw(
            now = LocalTime.of(13, 5, 0),
            mode = "digits",
            format24h = true,
            blinkColon = false,
            brightness = 90
        )

        assertTrue(midnight.litPixelCount() > 0)
        assertEquals(oneTwentyFourHour.pixels, afternoon.pixels)
        assertNotEquals(thirteenTwentyFourHour.pixels, afternoon.pixels)
    }
}
