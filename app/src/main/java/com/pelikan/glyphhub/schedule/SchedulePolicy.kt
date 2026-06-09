package com.pelikan.glyphhub.schedule

import com.pelikan.glyphhub.settings.AppSettings
import java.time.LocalTime

object SchedulePolicy {
    fun matrixAllowed(settings: AppSettings, now: LocalTime = LocalTime.now(), exception: ScheduleException = ScheduleException.None): Boolean {
        if (!settings.scheduleEnabled) return true
        if (exception in settings.allowedScheduleExceptions()) return true
        val start = parseTime(settings.nightStart, LocalTime.of(22, 0))
        val end = parseTime(settings.nightEnd, LocalTime.of(7, 0))
        val inQuietWindow = if (start <= end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
        return !inQuietWindow
    }

    fun parseTime(value: String, fallback: LocalTime): LocalTime =
        runCatching { LocalTime.parse(value) }.getOrDefault(fallback)

    private fun AppSettings.allowedScheduleExceptions(): Set<ScheduleException> =
        scheduleExceptionIds.split(",")
            .mapNotNull { id -> ScheduleException.entries.firstOrNull { it.id == id.trim() } }
            .toSet()
            .ifEmpty { setOf(ScheduleException.Timer, ScheduleException.Alarm, ScheduleException.Payment, ScheduleException.ChargingFull) }
}

enum class ScheduleException(val id: String) {
    None("none"),
    Timer("timer"),
    Alarm("alarm"),
    ChargingFull("charging_full"),
    Payment("payment"),
    Beacon("beacon")
}
