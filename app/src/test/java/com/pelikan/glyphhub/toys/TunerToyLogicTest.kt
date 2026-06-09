package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.audio.TuningMath
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TunerToyLogicTest {
    @Test
    fun guitarStandardTargetsUseOpenStringNotes() {
        val targets = TunerToyLogic.targets("guitar", "standard", 440f)

        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), targets.map { it.label })
        assertNear(82.41f, targets[0].frequencyHz)
        assertNear(329.63f, targets.last().frequencyHz)
    }

    @Test
    fun guitarDropDChangesLowestStringOnly() {
        val targets = TunerToyLogic.targets("guitar", "drop_d", 440f)

        assertEquals(listOf("D2", "A2", "D3", "G3", "B3", "E4"), targets.map { it.label })
        assertNear(73.42f, targets[0].frequencyHz)
    }

    @Test
    fun ukulelePresetsCoverHighGLowGAndBaritone() {
        assertEquals(
            listOf("G4", "C4", "E4", "A4"),
            TunerToyLogic.targets("ukulele", "standard", 440f).map { it.label }
        )
        assertEquals(
            listOf("G3", "C4", "E4", "A4"),
            TunerToyLogic.targets("ukulele", "uke_low_g", 440f).map { it.label }
        )
        assertEquals(
            listOf("D3", "G3", "B3", "E4"),
            TunerToyLogic.targets("ukulele", "uke_baritone", 440f).map { it.label }
        )
    }

    @Test
    fun markerMapsCentsToReadablePositions() {
        assertEquals(1, TunerToyLogic.markerX(-50))
        assertEquals(6, TunerToyLogic.markerX(0))
        assertEquals(11, TunerToyLogic.markerX(50))
        assertEquals(4, TunerToyLogic.markerX(-20))
        assertEquals(8, TunerToyLogic.markerX(20))
        assertNear(5.4f, TunerToyLogic.markerPosition(-5f))
        assertNear(6.6f, TunerToyLogic.markerPosition(5f))
    }

    @Test
    fun nearestTargetRecognizesExpectedGuitarString() {
        val targets = TunerToyLogic.targets("guitar", "standard", 440f)
        val nearest = TuningMath.nearestTarget(111f, targets)

        assertEquals("A2", nearest?.label)
    }

    @Test
    fun visualKeepsPixelsInsidePhysicalLayout() {
        listOf(-50, -18, 0, 17, 50).forEach { cents ->
            val frame = TunerToyVisuals.drawTuner("E2", cents.toFloat(), toleranceCents = 5, brightness = 90)
            assertTrue(frame.litPixelCount() > 0)
            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    if (frame.intensityAt(x, y) > 0) {
                        assertTrue(GlyphMatrixLayout.isPhysicalLed(x, y), "lit non-physical LED at $x,$y for $cents cents")
                    }
                }
            }
        }
    }

    @Test
    fun tuningLineUsesGradientAndFullLock() {
        val moving = TunerToyVisuals.drawTuner("E2", -18f, toleranceCents = 3, brightness = 90)
        assertTrue(moving.intensityAt(6, 8) > moving.intensityAt(0, 8))
        assertTrue(moving.intensityAt(6, 8) > moving.intensityAt(12, 8))

        val locked = TunerToyVisuals.drawTuner("E2", 0f, toleranceCents = 3, brightness = 90)
        for (x in 0..12) {
            assertEquals(100, locked.intensityAt(x, 8))
        }
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.05f, "expected $expected actual $actual")
    }
}
