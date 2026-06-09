package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelToyVisualsTest {
    @Test
    fun accelerometerPoseChoosesBubbleWhenPhoneIsFlat() {
        val reading = LevelToyLogic.reading(
            ax = 0.02f,
            ay = -0.03f,
            az = 0.98f,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.07f
        )

        assertEquals(LevelVisualMode.Bubble, reading.mode)
        assertTrue(reading.centered)
        assertEquals(0f, reading.normalizedError)
    }

    @Test
    fun accelerometerPoseChoosesLineWhenPhoneIsNotFlat() {
        val reading = LevelToyLogic.reading(
            ax = 0.32f,
            ay = 0.88f,
            az = 0.08f,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.07f
        )

        assertEquals(LevelVisualMode.Line, reading.mode)
        assertTrue(!reading.centered)
        assertTrue(reading.normalizedError > 0f)
    }

    @Test
    fun flatModeDoesNotSwitchTooEarly() {
        val reading = LevelToyLogic.reading(
            ax = 0.24f,
            ay = 0.12f,
            az = 0.84f,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )

        assertEquals(LevelVisualMode.Bubble, reading.mode)
    }

    @Test
    fun modeSwitchUsesStrongHysteresisNearNinetyDegrees() {
        val stillBubble = LevelToyLogic.reading(
            ax = 0.72f,
            ay = 0.14f,
            az = 0.46f,
            previousMode = LevelVisualMode.Bubble,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )
        val entersLine = LevelToyLogic.reading(
            ax = 0.97f,
            ay = 0.08f,
            az = 0.16f,
            previousMode = LevelVisualMode.Bubble,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )
        val staysLine = LevelToyLogic.reading(
            ax = 0.74f,
            ay = 0.18f,
            az = 0.52f,
            previousMode = LevelVisualMode.Line,
            previousAxis = LevelLineAxis.Vertical,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )
        val returnsBubble = LevelToyLogic.reading(
            ax = 0.08f,
            ay = 0.06f,
            az = 0.94f,
            previousMode = LevelVisualMode.Line,
            previousAxis = LevelLineAxis.Vertical,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )

        assertEquals(LevelVisualMode.Bubble, stillBubble.mode)
        assertEquals(LevelVisualMode.Line, entersLine.mode)
        assertEquals(LevelVisualMode.Line, staysLine.mode)
        assertEquals(LevelVisualMode.Bubble, returnsBubble.mode)
    }

    @Test
    fun lineModeDetectsHorizontalAndVerticalAxes() {
        val horizontal = LevelToyLogic.reading(
            ax = 0.2f,
            ay = 0.92f,
            az = 0.05f,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )
        val vertical = LevelToyLogic.reading(
            ax = 0.92f,
            ay = 0.2f,
            az = 0.05f,
            calibrationX = 0f,
            calibrationY = 0f,
            tolerance = 0.04f
        )

        assertEquals(LevelLineAxis.Horizontal, horizontal.axis)
        assertEquals(LevelLineAxis.Vertical, vertical.axis)
    }

    @Test
    fun bubbleAndLineVisualsStayInsidePhysicalMatrix() {
        val frames = listOf(
            LevelToyVisuals.draw(
                reading = LevelReading(
                    mode = LevelVisualMode.Bubble,
                    bubbleX = 0.46f,
                    bubbleY = -0.28f,
                    normalizedError = 0.58f,
                    centered = false
                ),
                brightness = 90,
                sensitivity = 0.9f,
                showGuides = true,
                pulseProgress = 1f
            ),
            LevelToyVisuals.draw(
                reading = LevelReading(
                    mode = LevelVisualMode.Line,
                    axis = LevelLineAxis.Horizontal,
                    lineTilt = -0.52f,
                    normalizedError = 0.62f,
                    centered = false
                ),
                brightness = 90,
                sensitivity = 0.9f,
                showGuides = true,
                pulseProgress = 1f
            )
        )

        frames.forEach { frame ->
            assertTrue(frame.litPixelCount() > 0)
            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    if (frame.intensityAt(x, y) > 0) {
                        assertTrue(GlyphMatrixLayout.isPhysicalLed(x, y), "lit non-physical LED at $x,$y")
                    }
                }
            }
        }
    }

    @Test
    fun bubbleUsesSubpixelPositionAcrossInnerCircle() {
        val frame = LevelToyVisuals.draw(
            reading = LevelReading(
                mode = LevelVisualMode.Bubble,
                bubbleX = 0.52f,
                bubbleY = -0.42f,
                normalizedError = 0.7f,
                centered = false
            ),
            brightness = 90,
            sensitivity = 1f,
            showGuides = false,
            pulseProgress = 1f
        )
        val brightest = brightestPixel(frame)

        assertTrue(brightest.first >= 8, "expected bubble to move right, got $brightest")
        assertTrue(brightest.second <= 5, "expected bubble to move up, got $brightest")
    }

    @Test
    fun lineAlwaysPivotsThroughCenter() {
        val horizontal = LevelToyVisuals.draw(
            reading = LevelReading(
                mode = LevelVisualMode.Line,
                axis = LevelLineAxis.Horizontal,
                lineTilt = 0.72f,
                normalizedError = 0.8f,
                centered = false
            ),
            brightness = 90,
            sensitivity = 1f,
            showGuides = false,
            pulseProgress = 1f
        )
        val vertical = LevelToyVisuals.draw(
            reading = LevelReading(
                mode = LevelVisualMode.Line,
                axis = LevelLineAxis.Vertical,
                lineTilt = -0.72f,
                normalizedError = 0.8f,
                centered = false
            ),
            brightness = 90,
            sensitivity = 1f,
            showGuides = false,
            pulseProgress = 1f
        )

        assertTrue(horizontal.intensityAt(6, 6) > 0)
        assertTrue(vertical.intensityAt(6, 6) > 0)
    }

    @Test
    fun highErrorDoesNotLightTheWholeOuterRing() {
        val frame = LevelToyVisuals.draw(
            reading = LevelReading(
                mode = LevelVisualMode.Bubble,
                bubbleX = 1f,
                bubbleY = 0f,
                normalizedError = 1f,
                centered = false
            ),
            brightness = 90,
            sensitivity = 1f,
            showGuides = false,
            pulseProgress = 1f
        )
        var litOuter = 0
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val edge = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                    .any { (nx, ny) -> !GlyphMatrixLayout.isPhysicalLed(nx, ny) }
                if (edge && frame.intensityAt(x, y) > 0) litOuter += 1
            }
        }

        assertTrue(litOuter <= 13, "too many outer LEDs lit: $litOuter")
    }

    @Test
    fun simulatedRuntimeModeSwitchNeverCreatesFullPanelFlash() {
        val samples = listOf(
            Triple(0.00f, 0.00f, 1.00f),
            Triple(0.22f, 0.08f, 0.94f),
            Triple(0.46f, 0.10f, 0.74f),
            Triple(0.82f, 0.08f, 0.38f),
            Triple(0.97f, 0.06f, 0.15f),
            Triple(0.88f, 0.22f, 0.12f),
            Triple(0.36f, 0.86f, 0.12f),
            Triple(0.12f, 0.96f, 0.10f),
            Triple(0.20f, 0.48f, 0.72f),
            Triple(0.04f, 0.04f, 0.96f)
        )
        var target = LevelReading()
        var displayed = LevelReading()

        samples.forEachIndexed { index, (ax, ay, az) ->
            val next = LevelToyLogic.reading(
                ax = ax,
                ay = ay,
                az = az,
                previousMode = target.mode,
                previousAxis = target.axis,
                calibrationX = 0f,
                calibrationY = 0f,
                tolerance = 0.04f
            )
            if (next.mode != target.mode || next.axis != target.axis) {
                displayed = next
            }
            target = next
            displayed = LevelToyLogic.smooth(displayed, target, smoothing = 0.07f, tolerance = 0.04f)
            val frame = LevelToyVisuals.draw(
                reading = displayed,
                brightness = 90,
                sensitivity = 0.82f,
                showGuides = false,
                pulseProgress = -1f
            )

            assertTrue(frame.litPixelCount() in 1..38, "sample $index lit ${frame.litPixelCount()} LEDs")
        }
    }

    private fun brightestPixel(frame: GlyphFrame): Pair<Int, Int> {
        var best = 0 to 0
        var bestIntensity = -1
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val intensity = frame.intensityAt(x, y)
                if (intensity > bestIntensity) {
                    best = x to y
                    bestIntensity = intensity
                }
            }
        }
        return best
    }
}
