package com.pelikan.glyphhub.schoolonline

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

internal object SkolaOnlineTimetableParser {
    fun parse(
        body: String,
        fetchedAtMs: Long = System.currentTimeMillis(),
        fallbackDateFrom: LocalDate = LocalDate.now(),
        fallbackDateTo: LocalDate = fallbackDateFrom
    ): SkolaOnlineScheduleSnapshot {
        val json = JSONObject(body)
        val days = json.optJSONArray("days")
        val lessons = buildList {
            if (days != null) {
                for (dayIndex in 0 until days.length()) {
                    val day = days.optJSONObject(dayIndex) ?: continue
                    val schedules = day.optJSONArray("schedules") ?: continue
                    for (scheduleIndex in 0 until schedules.length()) {
                        val schedule = schedules.optJSONObject(scheduleIndex) ?: continue
                        parseLesson(schedule)?.let(::add)
                    }
                }
            }
        }.sortedBy { it.startDateTime }
        val firstDate = lessons.firstOrNull()?.startDateTime?.toLocalDate() ?: fallbackDateFrom
        val lastDate = lessons.lastOrNull()?.startDateTime?.toLocalDate() ?: fallbackDateTo
        return SkolaOnlineScheduleSnapshot(
            fetchedAtMs = fetchedAtMs,
            dateFrom = firstDate,
            dateTo = lastDate,
            lessons = lessons
        )
    }

    private fun parseLesson(json: JSONObject): SkolaOnlineLesson? =
        runCatching {
            val subject = json.optJSONObject("subject")
            val hourType = json.optJSONObject("hourType")
            val hourKind = json.optJSONObject("hourKind")
            SkolaOnlineLesson(
                sourceId = json.optString("scheduledHourId"),
                startDateTime = LocalDateTime.parse(json.getString("beginTime")),
                endDateTime = LocalDateTime.parse(json.getString("endTime")),
                label = subject?.optString("abbrev").orEmpty().ifBlank { "?" },
                subjectName = subject?.optString("name").orEmpty(),
                hourTypeId = hourType?.optString("id").orEmpty(),
                hourTypeDescription = hourType?.optString("description").orEmpty(),
                hourKindId = hourKind?.optString("id").orEmpty(),
                hourKindDescription = hourKind?.optString("description").orEmpty()
            )
        }.getOrNull()
}
