package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import kotlin.math.ceil
import kotlin.math.roundToInt

internal object SchoolClassTimerLogic {
    const val DEFAULT_SCHEDULE =
        "MON=12:35-13:20:HV:ALL;" +
            "TUE=11:40-12:25:IKT:EVEN,12:35-13:20:IKT:EVEN,13:30-15:05:PDT:EVEN;" +
            "WED=11:40-12:25:F:ALL;" +
            "THU=10:45-11:30:F:ALL,13:30-14:15:HV:ALL,14:20-15:05:HV:ALL;" +
            "FRI=;SAT=;SUN="

    const val FIVE_MINUTE_WARNING_SECONDS = 5 * 60L
    const val SHORT_SEGMENT_WARNING_SECONDS = 60L
    const val MAX_BREAK_SECONDS = 30 * 60L
    const val RING_LED_ACTIVE_STATES = 3

    fun stateAt(
        now: LocalDateTime,
        encodedSchedule: String,
        countdownAnchor: LocalDateTime = now
    ): SchoolTimerState? {
        val schedule = parseSchedule(encodedSchedule)
        if (schedule.isEmpty()) return null
        return stateFromOccurrences(now, occurrencesAround(now.toLocalDate(), schedule), countdownAnchor)
    }

    fun stateAt(
        now: LocalDateTime,
        lessons: List<SchoolResolvedLesson>,
        countdownAnchor: LocalDateTime = now
    ): SchoolTimerState? {
        val occurrences = lessons.map {
            SchoolLessonOccurrence(
                date = it.startDateTime.toLocalDate(),
                lesson = SchoolLessonBlock(
                    day = it.startDateTime.dayOfWeek,
                    start = it.startDateTime.toLocalTime(),
                    end = it.endDateTime.toLocalTime(),
                    label = it.label,
                    weekMode = SchoolWeekMode.All,
                    isSubstitution = it.isSubstitution
                ),
                startDateTime = it.startDateTime,
                endDateTime = it.endDateTime
            )
        }.sortedBy { it.startDateTime }
        return stateFromOccurrences(now, occurrences, countdownAnchor)
    }

    fun isBeforeFirstLessonToday(now: LocalDateTime, encodedSchedule: String): Boolean {
        val schedule = parseSchedule(encodedSchedule)
        val weekMode = weekModeFor(now.toLocalDate())
        val first = schedule[now.dayOfWeek]
            .orEmpty()
            .filter { it.weekMode.matches(weekMode) }
            .minByOrNull { it.start }
            ?: return false
        return now.toLocalTime().isBefore(first.start)
    }

    fun isBeforeFirstLessonToday(now: LocalDateTime, lessons: List<SchoolResolvedLesson>): Boolean {
        val first = lessons
            .filter { it.startDateTime.toLocalDate() == now.toLocalDate() }
            .minByOrNull { it.startDateTime }
            ?: return false
        return now.isBefore(first.startDateTime)
    }

    private fun stateFromOccurrences(
        now: LocalDateTime,
        occurrences: List<SchoolLessonOccurrence>,
        countdownAnchor: LocalDateTime
    ): SchoolTimerState? {
        if (occurrences.isEmpty()) return SchoolTimerState.Free

        occurrences.firstOrNull { !now.isBefore(it.startDateTime) && now.isBefore(it.endDateTime) }?.let { current ->
            return SchoolTimerState.Class(
                label = current.lesson.label,
                lesson = lessonIndexFor(current, occurrences),
                isSubstitution = current.lesson.isSubstitution,
                progressRemaining = progressRemaining(now, current.startDateTime, current.endDateTime),
                remainingSeconds = secondsRemaining(now, current.endDateTime),
                totalSeconds = totalSeconds(current.startDateTime, current.endDateTime),
                segmentKey = "${current.startDateTime.toLocalDate()}-${current.lesson.start}-${current.lesson.end}-${current.lesson.label}"
            )
        }

        val previous = occurrences.lastOrNull { !it.endDateTime.isAfter(now) }
        val next = occurrences.firstOrNull { it.startDateTime.isAfter(now) || it.startDateTime == now }
        if (next == null) return SchoolTimerState.Free
        if (next.date != now.toLocalDate()) return SchoolTimerState.Free

        if (previous != null && previous.date == next.date) {
            val gapSeconds = totalSeconds(previous.endDateTime, next.startDateTime)
            if (gapSeconds in 1..MAX_BREAK_SECONDS && !now.isBefore(previous.endDateTime) && now.isBefore(next.startDateTime)) {
                return SchoolTimerState.Break(
                    nextLabel = next.lesson.label,
                    nextIsSubstitution = next.lesson.isSubstitution,
                    progressRemaining = progressRemaining(now, previous.endDateTime, next.startDateTime),
                    remainingSeconds = secondsRemaining(now, next.startDateTime),
                    totalSeconds = gapSeconds,
                    segmentKey = "${previous.date}-${previous.lesson.end}-${next.lesson.start}-BREAK"
                )
            }
        }

        val start = if (previous?.date == next.date) previous.endDateTime else countdownAnchor.coerceAtMost(now)
        return SchoolTimerState.FreeCountdown(
            nextLabel = next.lesson.label,
            nextIsSubstitution = next.lesson.isSubstitution,
            progressRemaining = progressRemaining(now, start, next.startDateTime),
            remainingSeconds = secondsRemaining(now, next.startDateTime),
            totalSeconds = totalSeconds(start, next.startDateTime).coerceAtLeast(1L),
            segmentKey = "${next.startDateTime.toLocalDate()}-${next.lesson.start}-FREE"
        )
    }

    fun parseSchedule(value: String): Map<DayOfWeek, List<SchoolLessonBlock>> {
        if (value.isBlank()) return parseSchedule(DEFAULT_SCHEDULE)
        return value.split(';')
            .mapNotNull { dayBlock ->
                val day = dayAliases[dayBlock.substringBefore('=').trim().uppercase()] ?: return@mapNotNull null
                val lessons = dayBlock.substringAfter('=', "")
                    .split(',', '|')
                    .mapNotNull { parseLessonBlock(day, it) }
                    .filter { it.start < it.end }
                    .sortedBy { it.start }
                day to lessons
            }
            .toMap()
            .filterValues { it.isNotEmpty() }
    }

    fun warningThresholdSeconds(totalSeconds: Long): Long =
        if (totalSeconds <= FIVE_MINUTE_WARNING_SECONDS) SHORT_SEGMENT_WARNING_SECONDS else FIVE_MINUTE_WARNING_SECONDS

    fun shouldWarn(state: SchoolTimerState.Timed, warnedSegmentKey: String?): Boolean =
        warnedSegmentKey != state.segmentKey &&
            state.remainingSeconds in 1..warningThresholdSeconds(state.totalSeconds)

    fun warningStage(
        state: SchoolTimerState.Timed,
        warnedStageKeys: Set<String>
    ): SchoolWarningStage? {
        if (state.remainingSeconds !in 1..state.totalSeconds) return null
        val lastMinuteKey = warningKey(state, SchoolWarningStage.LastMinute)
        if (state.remainingSeconds <= SHORT_SEGMENT_WARNING_SECONDS && lastMinuteKey !in warnedStageKeys) {
            return SchoolWarningStage.LastMinute
        }
        val fiveMinuteKey = warningKey(state, SchoolWarningStage.FiveMinute)
        if (
            state.totalSeconds > FIVE_MINUTE_WARNING_SECONDS &&
            state.remainingSeconds <= FIVE_MINUTE_WARNING_SECONDS &&
            fiveMinuteKey !in warnedStageKeys
        ) {
            return SchoolWarningStage.FiveMinute
        }
        return null
    }

    fun warningKey(state: SchoolTimerState.Timed, stage: SchoolWarningStage): String =
        "${state.segmentKey}:${stage.id}"

    fun ringLedStateUnits(ringPixelCount: Int, progressRemaining: Float): List<Int> {
        if (ringPixelCount <= 0) return emptyList()
        val totalUnits = ringPixelCount * RING_LED_ACTIVE_STATES
        val activeUnits = ceil(progressRemaining.coerceIn(0f, 1f) * totalUnits)
            .toInt()
            .coerceIn(0, totalUnits)
        return List(ringPixelCount) { index ->
            (activeUnits - index * RING_LED_ACTIVE_STATES).coerceIn(0, RING_LED_ACTIVE_STATES)
        }
    }

    fun ringLedIntensity(baseIntensity: Int, stateUnits: Int): Int {
        val base = baseIntensity.coerceIn(0, 100)
        return when (stateUnits.coerceIn(0, RING_LED_ACTIVE_STATES)) {
            3 -> base
            2 -> (base * 0.65f).roundToInt().coerceIn(1, 100)
            1 -> (base * 0.35f).roundToInt().coerceIn(1, 100)
            else -> 0
        }
    }

    private fun parseLessonBlock(day: DayOfWeek, value: String): SchoolLessonBlock? {
        val match = lessonPattern.matchEntire(value.trim()) ?: return null
        val start = parseTime(match.groupValues[1], LocalTime.MIN)
        val end = parseTime(match.groupValues[2], LocalTime.MIN)
        val label = match.groupValues[3].trim().ifBlank { "CLS" }
        val weekMode = SchoolWeekMode.fromId(match.groupValues.getOrNull(4))
        val isSubstitution = match.groupValues.getOrNull(5).equals("SUB", ignoreCase = true)
        return SchoolLessonBlock(day, start, end, label, weekMode, isSubstitution)
    }

    private fun occurrencesAround(date: LocalDate, schedule: Map<DayOfWeek, List<SchoolLessonBlock>>): List<SchoolLessonOccurrence> =
        (-15..15).flatMap { offset ->
            val day = date.plusDays(offset.toLong())
            val weekMode = weekModeFor(day)
            schedule[day.dayOfWeek].orEmpty()
                .filter { it.weekMode.matches(weekMode) }
                .map { lesson ->
                    SchoolLessonOccurrence(
                        date = day,
                        lesson = lesson,
                        startDateTime = LocalDateTime.of(day, lesson.start),
                        endDateTime = LocalDateTime.of(day, lesson.end)
                    )
                }
        }.sortedBy { it.startDateTime }

    private fun lessonIndexFor(current: SchoolLessonOccurrence, occurrences: List<SchoolLessonOccurrence>): Int =
        occurrences.count { it.date == current.date && it.startDateTime <= current.startDateTime }

    private fun progressRemaining(now: LocalDateTime, start: LocalDateTime, end: LocalDateTime): Float {
        val total = totalSeconds(start, end).coerceAtLeast(1L)
        val remaining = secondsRemaining(now, end).coerceIn(0L, total)
        return remaining.toFloat() / total.toFloat()
    }

    private fun secondsRemaining(now: LocalDateTime, end: LocalDateTime): Long {
        val millis = ChronoUnit.MILLIS.between(now, end)
        return if (millis <= 0L) 0L else ((millis + 999L) / 1000L)
    }

    private fun totalSeconds(start: LocalDateTime, end: LocalDateTime): Long =
        ChronoUnit.SECONDS.between(start, end).coerceAtLeast(0L)

    private fun weekModeFor(date: LocalDate): SchoolWeekMode {
        val week = date.get(WeekFields.ISO.weekOfWeekBasedYear())
        return if (week % 2 == 0) SchoolWeekMode.Even else SchoolWeekMode.Odd
    }

    private fun parseTime(value: String, fallback: LocalTime): LocalTime =
        runCatching {
            val parts = value.trim().split(':', '.')
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@runCatching fallback
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        }.getOrDefault(fallback)

    private val lessonPattern = Regex("""(\d{1,2}[:.]\d{2})-(\d{1,2}[:.]\d{2}):([^:;,|]+)(?::(ALL|EVEN|ODD))?(?::(SUB))?""", RegexOption.IGNORE_CASE)

    private val dayAliases = mapOf(
        "MON" to DayOfWeek.MONDAY, "MO" to DayOfWeek.MONDAY, "PO" to DayOfWeek.MONDAY,
        "TUE" to DayOfWeek.TUESDAY, "TU" to DayOfWeek.TUESDAY, "UT" to DayOfWeek.TUESDAY,
        "WED" to DayOfWeek.WEDNESDAY, "WE" to DayOfWeek.WEDNESDAY, "ST" to DayOfWeek.WEDNESDAY,
        "THU" to DayOfWeek.THURSDAY, "TH" to DayOfWeek.THURSDAY, "CT" to DayOfWeek.THURSDAY,
        "FRI" to DayOfWeek.FRIDAY, "FR" to DayOfWeek.FRIDAY, "PA" to DayOfWeek.FRIDAY,
        "SAT" to DayOfWeek.SATURDAY, "SA" to DayOfWeek.SATURDAY, "SO" to DayOfWeek.SATURDAY,
        "SUN" to DayOfWeek.SUNDAY, "SU" to DayOfWeek.SUNDAY, "NE" to DayOfWeek.SUNDAY
    )
}

internal object SchoolMatrixText {
    const val MAX_LABEL_WIDTH = 10
    const val MAX_LABEL_HEIGHT = 10

    fun displayLabel(label: String): String {
        val normalized = GlyphTextRenderer.transliterate(label)
            .filter { it.isLetterOrDigit() }
        if (normalized.isBlank()) return "?"
        val twoChars = normalized.take(2)
        return if (normalized.length <= 2 && GlyphTextRenderer.measureText3x5(twoChars, spacing = 1) <= MAX_LABEL_WIDTH) {
            twoChars
        } else {
            normalized.first().toString()
        }
    }

    fun drawSubjectLabel(label: String, brightness: Int): GlyphFrame {
        val display = displayLabel(label)
        return if (display.length == 1) {
            GlyphTextRenderer.drawSingleGlyph5x7(display.first(), brightness)
        } else {
            val width = GlyphTextRenderer.measureText3x5(display, spacing = 1)
            val xOffset = ((GlyphFrame.MATRIX_SIZE - width) / 2).coerceIn(1, 10)
            GlyphTextRenderer.drawText3x5(display, brightness, yOffset = 4, xOffset = xOffset, spacing = 1)
        }
    }
}

internal sealed interface SchoolTimerState {
    sealed interface Timed : SchoolTimerState {
        val progressRemaining: Float
        val remainingSeconds: Long
        val totalSeconds: Long
        val segmentKey: String
    }

    data class Class(
        val label: String,
        val lesson: Int,
        val isSubstitution: Boolean = false,
        override val progressRemaining: Float,
        override val remainingSeconds: Long,
        override val totalSeconds: Long,
        override val segmentKey: String
    ) : Timed

    data class Break(
        val nextLabel: String,
        val nextIsSubstitution: Boolean = false,
        override val progressRemaining: Float,
        override val remainingSeconds: Long,
        override val totalSeconds: Long,
        override val segmentKey: String
    ) : Timed

    data class FreeCountdown(
        val nextLabel: String,
        val nextIsSubstitution: Boolean = false,
        override val progressRemaining: Float,
        override val remainingSeconds: Long,
        override val totalSeconds: Long,
        override val segmentKey: String
    ) : Timed

    data object Free : SchoolTimerState
}

internal enum class SchoolWarningStage(val id: String) {
    FiveMinute("five_minute"),
    LastMinute("last_minute")
}

internal data class SchoolLessonBlock(
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val label: String,
    val weekMode: SchoolWeekMode,
    val isSubstitution: Boolean = false
)

internal data class SchoolResolvedLesson(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val label: String,
    val isSubstitution: Boolean = false
)

internal enum class SchoolWeekMode(val id: String) {
    All("ALL"),
    Even("EVEN"),
    Odd("ODD");

    fun matches(current: SchoolWeekMode): Boolean =
        this == All || this == current

    companion object {
        fun fromId(value: String?): SchoolWeekMode =
            entries.firstOrNull { it.id.equals(value.orEmpty(), ignoreCase = true) } ?: All
    }
}

private data class SchoolLessonOccurrence(
    val date: LocalDate,
    val lesson: SchoolLessonBlock,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime
)
