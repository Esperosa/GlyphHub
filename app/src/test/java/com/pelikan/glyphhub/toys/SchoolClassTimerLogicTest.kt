package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SchoolClassTimerLogicTest {
    @Test
    fun parsesScreenshotScheduleWithWeekModes() {
        val schedule = SchoolClassTimerLogic.parseSchedule(SchoolClassTimerLogic.DEFAULT_SCHEDULE)

        assertEquals("HV", schedule[DayOfWeek.MONDAY]?.single()?.label)
        assertEquals(3, schedule[DayOfWeek.TUESDAY]?.size)
        assertTrue(schedule[DayOfWeek.TUESDAY].orEmpty().all { it.weekMode == SchoolWeekMode.Even })
        assertEquals(3, schedule[DayOfWeek.THURSDAY]?.size)
    }

    @Test
    fun filtersEvenAndOddWeeks() {
        val evenTuesday = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 9, 11, 50),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )
        val oddTuesday = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 16, 11, 50),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertIs<SchoolTimerState.Class>(evenTuesday)
        assertEquals("IKT", evenTuesday.label)
        assertNotEquals(SchoolTimerState.Class::class, oddTuesday!!::class)
    }

    @Test
    fun classProgressIsSecondAccurate() {
        val atStart = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 12, 35, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )
        val halfway = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 12, 57, 30),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )
        val oneSecondBeforeEnd = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 13, 19, 59),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertIs<SchoolTimerState.Class>(atStart)
        assertIs<SchoolTimerState.Class>(halfway)
        assertIs<SchoolTimerState.Class>(oneSecondBeforeEnd)
        assertEquals(2700L, atStart.totalSeconds)
        assertEquals(2700L, atStart.remainingSeconds)
        assertTrue(abs(halfway.progressRemaining - 0.5f) < 0.001f)
        assertEquals(1L, oneSecondBeforeEnd.remainingSeconds)
    }

    @Test
    fun progressMapsToExpectedRingPixelCount() {
        val ringPixels = 36

        val full = SchoolClassTimerLogic.ringLedStateUnits(ringPixels, 1f)
        val half = SchoolClassTimerLogic.ringLedStateUnits(ringPixels, 0.5f)
        val oneUnit = SchoolClassTimerLogic.ringLedStateUnits(ringPixels, 1f / (ringPixels * SchoolClassTimerLogic.RING_LED_ACTIVE_STATES))
        val empty = SchoolClassTimerLogic.ringLedStateUnits(ringPixels, 0f)

        assertEquals(ringPixels * SchoolClassTimerLogic.RING_LED_ACTIVE_STATES, full.sum())
        assertTrue(full.all { it == 3 })
        assertEquals(ringPixels * SchoolClassTimerLogic.RING_LED_ACTIVE_STATES / 2, half.sum())
        assertEquals(18, half.count { it == 3 })
        assertEquals(1, oneUnit.sum())
        assertEquals(1, oneUnit.count { it == 1 })
        assertTrue(empty.all { it == 0 })
        assertEquals(35, SchoolClassTimerLogic.ringLedIntensity(100, 1))
        assertEquals(65, SchoolClassTimerLogic.ringLedIntensity(100, 2))
        assertEquals(100, SchoolClassTimerLogic.ringLedIntensity(100, 3))
        assertEquals(0, SchoolClassTimerLogic.ringLedIntensity(100, 0))
    }

    @Test
    fun computesLongFreeCountdownToFutureLesson() {
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 8, 0, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertIs<SchoolTimerState.FreeCountdown>(state)
        assertEquals(16_500L, state.remainingSeconds)
        assertEquals(16_500L, state.totalSeconds)
        assertEquals(1f, state.progressRemaining)
    }

    @Test
    fun freeCountdownBeforeFirstLessonUsesActivationTime() {
        val activatedAt = LocalDateTime.of(2026, 6, 8, 11, 40, 0)
        val atActivation = SchoolClassTimerLogic.stateAt(
            activatedAt,
            SchoolClassTimerLogic.DEFAULT_SCHEDULE,
            countdownAnchor = activatedAt
        )
        val halfway = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 12, 7, 30),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE,
            countdownAnchor = activatedAt
        )

        assertIs<SchoolTimerState.FreeCountdown>(atActivation)
        assertEquals(3_300L, atActivation.remainingSeconds)
        assertEquals(3_300L, atActivation.totalSeconds)
        assertEquals(1f, atActivation.progressRemaining)

        assertIs<SchoolTimerState.FreeCountdown>(halfway)
        assertEquals(1_650L, halfway.remainingSeconds)
        assertEquals(3_300L, halfway.totalSeconds)
        assertTrue(abs(halfway.progressRemaining - 0.5f) < 0.001f)
    }

    @Test
    fun detectsPreFirstLessonOnlyBeforeFirstLessonOfTheDay() {
        assertTrue(
            SchoolClassTimerLogic.isBeforeFirstLessonToday(
                LocalDateTime.of(2026, 6, 8, 12, 20, 0),
                SchoolClassTimerLogic.DEFAULT_SCHEDULE
            )
        )
        assertTrue(
            !SchoolClassTimerLogic.isBeforeFirstLessonToday(
                LocalDateTime.of(2026, 6, 8, 12, 35, 0),
                SchoolClassTimerLogic.DEFAULT_SCHEDULE
            )
        )
        assertTrue(
            !SchoolClassTimerLogic.isBeforeFirstLessonToday(
                LocalDateTime.of(2026, 6, 8, 13, 21, 0),
                SchoolClassTimerLogic.DEFAULT_SCHEDULE
            )
        )
    }

    @Test
    fun detectsPreFirstLessonForResolvedOnlineLessons() {
        val lessons = listOf(
            SchoolResolvedLesson(
                startDateTime = LocalDateTime.of(2026, 6, 8, 12, 35, 0),
                endDateTime = LocalDateTime.of(2026, 6, 8, 13, 20, 0),
                label = "HV"
            )
        )

        assertTrue(SchoolClassTimerLogic.isBeforeFirstLessonToday(LocalDateTime.of(2026, 6, 8, 12, 20, 0), lessons))
        assertTrue(!SchoolClassTimerLogic.isBeforeFirstLessonToday(LocalDateTime.of(2026, 6, 8, 12, 35, 0), lessons))
        assertTrue(!SchoolClassTimerLogic.isBeforeFirstLessonToday(LocalDateTime.of(2026, 6, 9, 12, 20, 0), lessons))
    }

    @Test
    fun noFutureLessonInCurrentWeekShowsFreeState() {
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 12, 16, 0, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertEquals(SchoolTimerState.Free, state)
    }

    @Test
    fun noFutureLessonTodayShowsFreeStateEvenWhenAnotherDayHasLesson() {
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 13, 21, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertEquals(SchoolTimerState.Free, state)
    }

    @Test
    fun immediatelyAfterLastLessonShowsFreeInsteadOfBreak() {
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 13, 20, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertEquals(SchoolTimerState.Free, state)
    }

    @Test
    fun computesBreaksFromGapsBetweenLessons() {
        val state = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 9, 12, 30, 0),
            SchoolClassTimerLogic.DEFAULT_SCHEDULE
        )

        assertIs<SchoolTimerState.Break>(state)
        assertEquals(600L, state.totalSeconds)
        assertEquals(300L, state.remainingSeconds)
    }

    @Test
    fun shortSegmentsWarnOnlyInLastMinute() {
        assertEquals(300L, SchoolClassTimerLogic.warningThresholdSeconds(301L))
        assertEquals(60L, SchoolClassTimerLogic.warningThresholdSeconds(300L))
        assertEquals(60L, SchoolClassTimerLogic.warningThresholdSeconds(240L))

        val shortBreak = SchoolTimerState.Break(
            nextLabel = "M",
            progressRemaining = 0.5f,
            remainingSeconds = 90L,
            totalSeconds = 240L,
            segmentKey = "short"
        )
        assertTrue(!SchoolClassTimerLogic.shouldWarn(shortBreak, null))
        assertTrue(SchoolClassTimerLogic.shouldWarn(shortBreak.copy(remainingSeconds = 60L), null))
        assertEquals(null, SchoolClassTimerLogic.warningStage(shortBreak, emptySet()))
        assertEquals(SchoolWarningStage.LastMinute, SchoolClassTimerLogic.warningStage(shortBreak.copy(remainingSeconds = 60L), emptySet()))
    }

    @Test
    fun longSegmentsWarnAtFiveMinutesAndAgainAtLastMinute() {
        val longClass = SchoolTimerState.Class(
            label = "HV",
            lesson = 1,
            progressRemaining = 0.2f,
            remainingSeconds = 300L,
            totalSeconds = 2700L,
            segmentKey = "long"
        )

        val fiveMinute = SchoolClassTimerLogic.warningStage(longClass, emptySet())
        assertEquals(SchoolWarningStage.FiveMinute, fiveMinute)

        val warnedFiveMinute = setOf(SchoolClassTimerLogic.warningKey(longClass, SchoolWarningStage.FiveMinute))
        assertEquals(null, SchoolClassTimerLogic.warningStage(longClass, warnedFiveMinute))
        assertEquals(
            SchoolWarningStage.LastMinute,
            SchoolClassTimerLogic.warningStage(longClass.copy(remainingSeconds = 60L), warnedFiveMinute)
        )
    }

    @Test
    fun fractionalSecondsDoNotTriggerFiveMinuteWarningEarly() {
        val schedule = "MON=16:19-16:25:HV:ALL"
        val justBeforeThreshold = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 16, 19, 59, 1_000_000),
            schedule
        )
        val atThreshold = SchoolClassTimerLogic.stateAt(
            LocalDateTime.of(2026, 6, 8, 16, 20, 0),
            schedule
        )

        assertIs<SchoolTimerState.Class>(justBeforeThreshold)
        assertEquals(301L, justBeforeThreshold.remainingSeconds)
        assertEquals(null, SchoolClassTimerLogic.warningStage(justBeforeThreshold, emptySet()))

        assertIs<SchoolTimerState.Class>(atThreshold)
        assertEquals(300L, atThreshold.remainingSeconds)
        assertEquals(SchoolWarningStage.FiveMinute, SchoolClassTimerLogic.warningStage(atThreshold, emptySet()))
    }

    @Test
    fun matrixLabelsUseSafeOneOrTwoCharacterForms() {
        val labels = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".map { it.toString() } + listOf("HV", "IKT", "PDT", "ČJ", "CH", "MATH"))

        labels.forEach { label ->
            val display = SchoolMatrixText.displayLabel(label)
            assertTrue(display.length in 1..2, "$label rendered as $display")

            val frame = SchoolMatrixText.drawSubjectLabel(label, brightness = 90)
            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    if (frame.intensityAt(x, y) > 0) {
                        assertTrue(GlyphMatrixLayout.isPhysicalLed(x, y), "$label lit non-physical led $x,$y")
                        assertTrue(x in 1..10, "$label outside safe 10-wide text window at x=$x")
                        assertTrue(y in 2..11, "$label outside safe 10-high text window at y=$y")
                    }
                }
            }
        }

        assertEquals("I", SchoolMatrixText.displayLabel("IKT"))
        assertEquals("P", SchoolMatrixText.displayLabel("PDT"))
        assertEquals("HV", SchoolMatrixText.displayLabel("HV"))
    }
}
