package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import com.pelikan.glyphhub.schoolonline.SkolaOnlineDaySummary
import com.pelikan.glyphhub.schoolonline.SkolaOnlineRepository
import com.pelikan.glyphhub.schoolonline.SkolaOnlineScheduleSnapshot
import com.pelikan.glyphhub.schoolonline.SkolaOnlineSyncResult
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

class SchoolClassTimerToyModule : BaseGlyphToyModule() {
    override val id = "school_class_timer"
    override val name = "School Class Timer Toy"
    override val shortName = "CLASS"
    override val description = "Wall-clock school timetable with class, break, and free-time Matrix states."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("schedule", "Schedule", "Exact timetable encoded by the day editor.", ToySettingType.Text, SchoolClassTimerLogic.DEFAULT_SCHEDULE, visibleByDefault = false),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "86", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var haptic: HapticFeedbackController? = null
    private val warnedStageKeys = mutableSetOf<String>()
    private var blinkRemainingMs = 0L
    private var blinkTotalMs = 0L
    private var blinkStepMs = FIVE_MINUTE_BLINK_STEP_MS
    private var urgentBlink = false
    private var activatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
    private var freeAnimationMs = 0L
    private var freeTransitionMs = 0L
    private var freeTransitionFromFrame: GlyphFrame? = null
    private var lastNonFreeFrame: GlyphFrame? = null
    private var lastStateWasFree = false
    private var nextLessonRevealMs = 0L
    private var elapsedMs = 0L
    @Volatile private var onlineSnapshot: SkolaOnlineScheduleSnapshot? = null
    @Volatile private var onlineEnabled = false
    @Volatile private var onlineRefreshInFlight = false
    @Volatile private var generation = 0L
    private var onlineRepository: SkolaOnlineRepository? = null
    private var daySummaryReveal: SkolaOnlineDaySummary? = null
    private var daySummaryRevealRemainingMs = 0L
    private var daySummaryRevealTotalMs = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        generation += 1
        haptic = HapticFeedbackController(context.androidContext)
        activatedAt = java.time.LocalDateTime.now()
        freeAnimationMs = 0L
        freeTransitionMs = 0L
        freeTransitionFromFrame = null
        lastNonFreeFrame = null
        lastStateWasFree = false
        nextLessonRevealMs = 0L
        elapsedMs = 0L
        val repository = SkolaOnlineRepository(context.androidContext)
        onlineRepository = repository
        onlineEnabled = repository.accountStatus().enabled
        onlineSnapshot = repository.cachedSchedule()
        daySummaryReveal = null
        daySummaryRevealRemainingMs = 0L
        daySummaryRevealTotalMs = 0L
        maybeRefreshOnline(force = true)
    }

    override fun onDeactivate() {
        generation += 1
        haptic = null
        warnedStageKeys.clear()
        blinkRemainingMs = 0L
        blinkTotalMs = 0L
        blinkStepMs = FIVE_MINUTE_BLINK_STEP_MS
        urgentBlink = false
        freeAnimationMs = 0L
        freeTransitionMs = 0L
        freeTransitionFromFrame = null
        lastNonFreeFrame = null
        lastStateWasFree = false
        nextLessonRevealMs = 0L
        elapsedMs = 0L
        onlineSnapshot = null
        onlineEnabled = false
        onlineRefreshInFlight = false
        onlineRepository = null
        daySummaryReveal = null
        daySummaryRevealRemainingMs = 0L
        daySummaryRevealTotalMs = 0L
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        maybeRefreshOnline(force = false)
        val schedule = getSettings().text("schedule", SchoolClassTimerLogic.DEFAULT_SCHEDULE)
        val now = java.time.LocalDateTime.now()
        val state = currentState(now, schedule)
        val isFree = state is SchoolTimerState.Free
        if (isFree) {
            if (!lastStateWasFree) {
                freeAnimationMs = 0L
                freeTransitionMs = FREE_STATE_MORPH_MS
                freeTransitionFromFrame = lastNonFreeFrame
            } else {
                freeAnimationMs = (freeAnimationMs + deltaMs).coerceAtMost(FREE_ANIMATION_MS)
            }
            if (freeTransitionMs > 0L) freeTransitionMs = (freeTransitionMs - deltaMs).coerceAtLeast(0L)
        } else {
            freeAnimationMs = 0L
            freeTransitionMs = 0L
            freeTransitionFromFrame = null
        }
        if (nextLessonRevealMs > 0L) nextLessonRevealMs = (nextLessonRevealMs - deltaMs).coerceAtLeast(0L)
        updateWarning(state, deltaMs, now, schedule)
        if (daySummaryRevealRemainingMs > 0L) {
            val frame = drawDaySummaryReveal(getSettings().int("brightness", 86).coerceIn(0, 100))
            daySummaryRevealRemainingMs = (daySummaryRevealRemainingMs - deltaMs).coerceAtLeast(0L)
            if (daySummaryRevealRemainingMs == 0L) {
                daySummaryReveal = null
                daySummaryRevealTotalMs = 0L
            }
            return frame
        }
        val frame = renderState(state)
        if (!isFree) lastNonFreeFrame = frame
        lastStateWasFree = isFree
        return frame
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event != ToySensorEvent.BackTap && event !is ToySensorEvent.Shake) return
        val schedule = getSettings().text("schedule", SchoolClassTimerLogic.DEFAULT_SCHEDULE)
        val now = java.time.LocalDateTime.now()
        val state = currentState(now, schedule)
        when (state) {
            is SchoolTimerState.Break -> nextLessonRevealMs = NEXT_LESSON_REVEAL_MS
            is SchoolTimerState.FreeCountdown -> {
                val summary = preFirstLessonSummary(now)
                if (summary != null) {
                    showDaySummaryReveal(summary)
                } else {
                    nextLessonRevealMs = NEXT_LESSON_REVEAL_MS
                }
            }
            SchoolTimerState.Free -> {
                freeAnimationMs = 0L
                freeTransitionMs = 0L
                freeTransitionFromFrame = null
            }
            else -> Unit
        }
    }

    private fun updateWarning(
        state: SchoolTimerState,
        deltaMs: Long,
        now: java.time.LocalDateTime,
        fallbackSchedule: String
    ) {
        if (blinkRemainingMs > 0L) blinkRemainingMs = (blinkRemainingMs - deltaMs).coerceAtLeast(0L)
        val timed = state as? SchoolTimerState.Timed ?: return
        if (timed is SchoolTimerState.FreeCountdown && isBeforeFirstLessonToday(now, fallbackSchedule)) {
            val preStartKey = "${timed.segmentKey}:pre_start_15"
            if (timed.remainingSeconds in 1..PRE_FIRST_LESSON_WARNING_SECONDS && preStartKey !in warnedStageKeys) {
                warnedStageKeys += preStartKey
                urgentBlink = true
                blinkTotalMs = PRE_FIRST_LESSON_BLINK_TOTAL_MS
                blinkStepMs = PRE_FIRST_LESSON_BLINK_STEP_MS
                blinkRemainingMs = blinkTotalMs
                haptic?.pulseSchoolPreStartWarning()
                return
            }
        }
        val stage = SchoolClassTimerLogic.warningStage(timed, warnedStageKeys) ?: return
        warnedStageKeys += SchoolClassTimerLogic.warningKey(timed, stage)
        urgentBlink = stage == SchoolWarningStage.LastMinute
        blinkTotalMs = if (urgentBlink) LAST_MINUTE_BLINK_TOTAL_MS else FIVE_MINUTE_BLINK_TOTAL_MS
        blinkStepMs = if (urgentBlink) LAST_MINUTE_BLINK_STEP_MS else FIVE_MINUTE_BLINK_STEP_MS
        blinkRemainingMs = blinkTotalMs
        if (urgentBlink) {
            haptic?.pulseUrgentWarning()
        } else {
            haptic?.pulseWarning()
        }
    }

    private fun renderState(state: SchoolTimerState): GlyphFrame {
        val brightness = getSettings().int("brightness", 86).coerceIn(0, 100)
        return when (state) {
            is SchoolTimerState.Class -> {
                var frame = drawRemainingOuterRing(brightness, state.progressRemaining, 100)
                val labelFrame = SchoolMatrixText.drawSubjectLabel(state.label, brightness)
                if (state.isSubstitution) {
                    frame = drawSubstitutionStars(frame, labelFrame, brightness)
                }
                frame = frame.overlay(labelFrame)
                GlyphMatrixLayout.mask(frame)
            }

            is SchoolTimerState.Break -> {
                val base = drawRemainingOuterRing(brightness, state.progressRemaining, 72)
                val frame = if (nextLessonRevealMs > 0L) {
                    drawRevealLabel(base, state.nextLabel, state.nextIsSubstitution)
                } else {
                    drawBreakIcon(base)
                }
                GlyphMatrixLayout.mask(frame)
            }

            is SchoolTimerState.FreeCountdown -> {
                val base = drawRemainingOuterRing(brightness, state.progressRemaining, 86)
                val frame = if (nextLessonRevealMs > 0L) {
                    drawRevealLabel(base, state.nextLabel, state.nextIsSubstitution)
                } else {
                    drawBreakIcon(base)
                }
                GlyphMatrixLayout.mask(frame)
            }

            SchoolTimerState.Free -> freeIconWithTransition(brightness)
        }
    }

    private fun drawRemainingOuterRing(brightness: Int, progressRemaining: Float, intensity: Int): GlyphFrame {
        val remaining = progressRemaining.coerceIn(0f, 1f)
        var frame = GlyphFrame.empty13(brightness)
        if (blinkRemainingMs > 0L) {
            val blinkElapsedMs = blinkTotalMs - blinkRemainingMs
            val blinkOn = (blinkElapsedMs / blinkStepMs.coerceAtLeast(1L)) % 2L == 0L
            outerRing.forEach { (x, y) ->
                frame = frame.withPixelBrightness(x, y, if (blinkOn) 100 else 0)
            }
            return frame
        }
        val stateUnits = SchoolClassTimerLogic.ringLedStateUnits(outerRing.size, remaining)
        outerRing.asReversed().forEachIndexed { index, (x, y) ->
            frame = frame.withPixelBrightness(
                x,
                y,
                SchoolClassTimerLogic.ringLedIntensity(intensity, stateUnits[index])
            )
        }
        return frame
    }

    private fun drawBreakIcon(frame: GlyphFrame, intensity: Int = 82): GlyphFrame {
        var output = frame
        for (y in 4..8) {
            output = output.withPixelBrightness(4, y, intensity)
            output = output.withPixelBrightness(5, y, intensity)
            output = output.withPixelBrightness(7, y, intensity)
            output = output.withPixelBrightness(8, y, intensity)
        }
        return output
    }

    private fun drawRevealLabel(frame: GlyphFrame, label: String, isSubstitution: Boolean): GlyphFrame {
        val elapsed = NEXT_LESSON_REVEAL_MS - nextLessonRevealMs
        val progress = min(
            smoothStep(elapsed / NEXT_LESSON_MORPH_MS.toFloat()),
            smoothStep(nextLessonRevealMs / NEXT_LESSON_MORPH_MS.toFloat())
        )
        val pauseFrame = drawBreakIcon(GlyphFrame.empty13(frame.brightness), 82)
        var labelFrame = SchoolMatrixText.drawSubjectLabel(label, 100)
        if (isSubstitution) {
            labelFrame = drawSubstitutionStars(labelFrame, labelFrame, 100)
        }
        return frame.overlay(blendFrames(pauseFrame, labelFrame, progress))
    }

    private fun currentState(now: java.time.LocalDateTime, fallbackSchedule: String): SchoolTimerState {
        val onlineLessons = onlineSnapshot
            ?.takeIf { onlineEnabled }
            ?.activeSchoolLessons()
            .orEmpty()
        return if (onlineLessons.isNotEmpty()) {
            SchoolClassTimerLogic.stateAt(now, onlineLessons, activatedAt) ?: SchoolTimerState.Free
        } else {
            SchoolClassTimerLogic.stateAt(now, fallbackSchedule, activatedAt) ?: SchoolTimerState.Free
        }
    }

    private fun maybeRefreshOnline(force: Boolean) {
        val repository = onlineRepository ?: return
        onlineEnabled = repository.accountStatus().enabled
        if (!onlineEnabled || onlineRefreshInFlight) return
        val now = System.currentTimeMillis()
        val cachedAt = onlineSnapshot?.fetchedAtMs ?: 0L
        if (!force && now - cachedAt < ONLINE_REFRESH_MIN_MS) return
        onlineRefreshInFlight = true
        val refreshGeneration = generation
        Thread({
            when (val result = repository.refresh()) {
                SkolaOnlineSyncResult.Disabled -> Unit
                is SkolaOnlineSyncResult.Failure -> {
                    if (generation == refreshGeneration) {
                        onlineSnapshot = result.cached ?: onlineSnapshot
                    }
                }
                is SkolaOnlineSyncResult.Success -> {
                    if (generation == refreshGeneration) {
                        onlineSnapshot = result.snapshot
                    }
                }
            }
            if (generation == refreshGeneration) onlineRefreshInFlight = false
        }, "GlyphHubSchoolOnline").apply {
            isDaemon = true
            start()
        }
    }

    private fun preFirstLessonSummary(now: java.time.LocalDateTime): SkolaOnlineDaySummary? {
        val snapshot = onlineSnapshot?.takeIf { onlineEnabled } ?: return null
        val lessonsToday = snapshot
            .activeSchoolLessons()
            .filter { it.startDateTime.toLocalDate() == now.toLocalDate() }
        val firstLesson = lessonsToday.minByOrNull { it.startDateTime } ?: return null
        if (!now.isBefore(firstLesson.startDateTime)) return null
        return snapshot.daySummary(now.toLocalDate()).takeIf {
            it.regularCount > 0 || it.substitutionCount > 0
        }
    }

    private fun isBeforeFirstLessonToday(now: java.time.LocalDateTime, fallbackSchedule: String): Boolean {
        val onlineLessons = onlineSnapshot
            ?.takeIf { onlineEnabled }
            ?.activeSchoolLessons()
            .orEmpty()
        return if (onlineLessons.isNotEmpty()) {
            SchoolClassTimerLogic.isBeforeFirstLessonToday(now, onlineLessons)
        } else {
            SchoolClassTimerLogic.isBeforeFirstLessonToday(now, fallbackSchedule)
        }
    }

    private fun showDaySummaryReveal(summary: SkolaOnlineDaySummary) {
        daySummaryReveal = summary
        daySummaryRevealTotalMs = if (summary.substitutionCount > 0) {
            DAY_SUMMARY_REVEAL_MS
        } else {
            DAY_SUMMARY_REGULAR_ONLY_MS
        }
        daySummaryRevealRemainingMs = daySummaryRevealTotalMs
        nextLessonRevealMs = 0L
    }

    private fun freeIconWithTransition(brightness: Int): GlyphFrame {
        val target = freeIcon(brightness, freeAnimationMs / FREE_ANIMATION_MS.toFloat())
        val from = freeTransitionFromFrame
        if (from == null || freeTransitionMs <= 0L) return target
        val progress = smoothStep(1f - freeTransitionMs / FREE_STATE_MORPH_MS.toFloat())
        return GlyphMatrixLayout.mask(blendFrames(from.withBrightness(brightness), target, progress))
    }

    private fun freeIcon(brightness: Int, progress: Float): GlyphFrame {
        val eased = smoothStep(progress)
        var frame = GlyphFrame.empty13(brightness)
        val eyeOffset = ((1f - eased) * 3f).roundToInt()
        for (y in 3..6) {
            frame = frame.withPixelBrightness(4, y + eyeOffset, 100)
            frame = frame.withPixelBrightness(5, y + eyeOffset, 100)
            frame = frame.withPixelBrightness(7, y + eyeOffset, 100)
            frame = frame.withPixelBrightness(8, y + eyeOffset, 100)
        }
        val smile = listOf(
            3 to 8,
            4 to 9,
            5 to 10,
            6 to 10,
            7 to 10,
            8 to 9,
            9 to 8
        )
        val smileOffset = ((1f - eased) * 3f).roundToInt()
        val visible = ceil(smile.size * eased).toInt().coerceIn(0, smile.size)
        smile.take(visible).forEach { (x, y) ->
            frame = frame.withPixelBrightness(x, y + smileOffset, 100)
        }
        return GlyphMatrixLayout.mask(frame)
    }

    private fun drawSubstitutionStars(frame: GlyphFrame, labelFrame: GlyphFrame, brightness: Int): GlyphFrame {
        var output = frame
        substitutionStars.forEachIndexed { index, (x, y) ->
            val labelPixel = labelFrame.pixels.getOrNull(y * GlyphFrame.MATRIX_SIZE + x) ?: 0
            if (labelPixel > 0) return@forEachIndexed
            val wave = ((elapsedMs / 160L + index * 2) % 6).toInt()
            val intensity = when (wave) {
                0 -> 32
                1 -> 54
                2 -> 78
                3 -> 60
                4 -> 42
                else -> 24
            }
            output = output.withPixelBrightness(x, y, (intensity * brightness / 100f).roundToInt().coerceIn(12, 100))
        }
        return output
    }

    private fun drawDaySummaryReveal(brightness: Int): GlyphFrame {
        val summary = daySummaryReveal ?: return GlyphFrame.empty13(brightness)
        val elapsed = daySummaryRevealTotalMs - daySummaryRevealRemainingMs
        val showSubstitution = summary.substitutionCount > 0 && elapsed >= ACTIVATION_SUMMARY_REGULAR_MS
        val frame = if (showSubstitution) {
            val label = summary.substitutionLabels.firstOrNull().orEmpty().ifBlank { "S" }
            var frame = GlyphFrame.empty13(brightness)
            val labelFrame = SchoolMatrixText.drawSubjectLabel(label, brightness)
            frame = drawDeadEyes(frame)
            frame = drawSubstitutionStars(frame, labelFrame, brightness)
            frame = frame.overlay(labelFrame)
            frame.overlay(
                GlyphTextRenderer.drawText3x5(
                    summary.substitutionCount.coerceIn(0, 9).toString(),
                    brightness,
                    yOffset = 8,
                    xOffset = 10,
                    spacing = 0
                )
            )
        } else {
            var frame = GlyphFrame.empty13(brightness)
            frame = drawSadFace(frame)
            frame.overlay(
                GlyphTextRenderer.drawSingleGlyph5x7(
                    summary.regularCount.coerceIn(0, 9).digitToChar(),
                    brightness
                )
            )
        }
        return GlyphMatrixLayout.mask(frame)
    }

    private fun drawSadFace(frame: GlyphFrame): GlyphFrame {
        var output = frame
        listOf(3 to 2, 9 to 2, 4 to 10, 5 to 9, 6 to 9, 7 to 9, 8 to 10).forEach { (x, y) ->
            output = output.withPixelBrightness(x, y, 100)
        }
        return output
    }

    private fun drawDeadEyes(frame: GlyphFrame): GlyphFrame {
        var output = frame
        listOf(
            2 to 2, 4 to 4, 4 to 2, 2 to 4,
            8 to 2, 10 to 4, 10 to 2, 8 to 4
        ).forEach { (x, y) ->
            output = output.withPixelBrightness(x, y, 100)
        }
        return output
    }

    private fun blendFrames(from: GlyphFrame, to: GlyphFrame, progress: Float): GlyphFrame {
        val p = progress.coerceIn(0f, 1f)
        val pixels = List(GlyphFrame.MATRIX_SIZE * GlyphFrame.MATRIX_SIZE) { index ->
            val start = from.pixels.getOrNull(index)?.coerceIn(0, 100) ?: 0
            val end = to.pixels.getOrNull(index)?.coerceIn(0, 100) ?: 0
            (start * (1f - p) + end * p).roundToInt().coerceIn(0, 100)
        }
        return GlyphFrame(GlyphFrame.MATRIX_SIZE, GlyphFrame.MATRIX_SIZE, to.brightness, pixels)
    }

    private fun smoothStep(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private companion object {
        const val FIVE_MINUTE_BLINK_TOTAL_MS = 1_800L
        const val FIVE_MINUTE_BLINK_STEP_MS = 300L
        const val LAST_MINUTE_BLINK_TOTAL_MS = 2_100L
        const val LAST_MINUTE_BLINK_STEP_MS = 175L
        const val PRE_FIRST_LESSON_WARNING_SECONDS = 15 * 60L
        const val PRE_FIRST_LESSON_BLINK_TOTAL_MS = 2_400L
        const val PRE_FIRST_LESSON_BLINK_STEP_MS = 120L
        const val FREE_ANIMATION_MS = 900L
        const val FREE_STATE_MORPH_MS = 1_400L
        const val NEXT_LESSON_REVEAL_MS = 5_000L
        const val NEXT_LESSON_MORPH_MS = 1_000L
        const val ONLINE_REFRESH_MIN_MS = 20 * 60_000L
        const val ACTIVATION_SUMMARY_REGULAR_MS = 2_200L
        const val DAY_SUMMARY_REVEAL_MS = 5_200L
        const val DAY_SUMMARY_REGULAR_ONLY_MS = 2_600L

        val substitutionStars = listOf(
            2 to 2,
            10 to 2,
            1 to 6,
            11 to 5,
            2 to 10,
            10 to 9,
            6 to 1,
            6 to 11
        )

        val outerRing = buildList {
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
}
