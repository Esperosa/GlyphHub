package com.pelikan.glyphhub.schoolonline

import com.pelikan.glyphhub.toys.SchoolResolvedLesson
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

internal enum class SkolaOnlineServer(val id: String, val label: String, val baseUrl: String) {
    Default("default", "aplikace.skolaonline.cz", "https://aplikace.skolaonline.cz/solapi"),
    Plzen("plzen", "skola.plzen-edu.cz", "https://skola.plzen-edu.cz/solapi");

    companion object {
        fun fromId(value: String?): SkolaOnlineServer =
            entries.firstOrNull { it.id == value } ?: Default
    }
}

internal data class SkolaOnlineAccountStatus(
    val enabled: Boolean,
    val server: SkolaOnlineServer,
    val username: String,
    val hasPassword: Boolean,
    val hasRefreshToken: Boolean,
    val lastSyncAtMs: Long,
    val lastSyncStatus: String
)

internal data class SkolaOnlineTokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMs: Long
) {
    fun isFresh(nowMs: Long = System.currentTimeMillis()): Boolean =
        accessToken.isNotBlank() && expiresAtMs - nowMs > TOKEN_SAFETY_WINDOW_MS

    private companion object {
        const val TOKEN_SAFETY_WINDOW_MS = 90_000L
    }
}

internal data class SkolaOnlineScheduleSnapshot(
    val fetchedAtMs: Long,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val lessons: List<SkolaOnlineLesson>
) {
    fun activeSchoolLessons(): List<SchoolResolvedLesson> =
        lessons
            .filter { it.isRegular || it.isSubstitution }
            .map {
                SchoolResolvedLesson(
                    startDateTime = it.startDateTime,
                    endDateTime = it.endDateTime,
                    label = it.label,
                    isSubstitution = it.isSubstitution
                )
            }
            .sortedBy { it.startDateTime }

    fun daySummary(date: LocalDate): SkolaOnlineDaySummary {
        val active = lessons.filter { it.startDateTime.toLocalDate() == date && (it.isRegular || it.isSubstitution) }
        val substitutions = active.filter { it.isSubstitution }
        return SkolaOnlineDaySummary(
            regularCount = active.count { it.isRegular },
            substitutionCount = substitutions.size,
            substitutionLabels = substitutions.map { it.label }.distinct()
        )
    }

    fun encode(): String =
        JSONObject()
            .put("fetchedAtMs", fetchedAtMs)
            .put("dateFrom", dateFrom.toString())
            .put("dateTo", dateTo.toString())
            .put("lessons", JSONArray().also { array ->
                lessons.forEach { array.put(it.encode()) }
            })
            .toString()

    companion object {
        fun decode(value: String): SkolaOnlineScheduleSnapshot? =
            runCatching {
                val json = JSONObject(value)
                val lessonsJson = json.optJSONArray("lessons") ?: JSONArray()
                SkolaOnlineScheduleSnapshot(
                    fetchedAtMs = json.optLong("fetchedAtMs", 0L),
                    dateFrom = LocalDate.parse(json.getString("dateFrom")),
                    dateTo = LocalDate.parse(json.getString("dateTo")),
                    lessons = List(lessonsJson.length()) { index ->
                        SkolaOnlineLesson.decode(lessonsJson.getJSONObject(index))
                    }.filterNotNull()
                )
            }.getOrNull()
    }
}

internal data class SkolaOnlineDaySummary(
    val regularCount: Int,
    val substitutionCount: Int,
    val substitutionLabels: List<String>
)

internal data class SkolaOnlineLesson(
    val sourceId: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val label: String,
    val subjectName: String,
    val hourTypeId: String,
    val hourTypeDescription: String,
    val hourKindId: String,
    val hourKindDescription: String
) {
    val isRegular: Boolean
        get() = hourTypeId == "ROZVRH"

    val isSubstitution: Boolean
        get() = hourTypeId == "SUPLOVANI"

    val isReplacedBySubstitution: Boolean
        get() = hourTypeId == "SUPLOVANA"

    fun encode(): JSONObject =
        JSONObject()
            .put("sourceId", sourceId)
            .put("startDateTime", startDateTime.toString())
            .put("endDateTime", endDateTime.toString())
            .put("label", label)
            .put("subjectName", subjectName)
            .put("hourTypeId", hourTypeId)
            .put("hourTypeDescription", hourTypeDescription)
            .put("hourKindId", hourKindId)
            .put("hourKindDescription", hourKindDescription)

    companion object {
        fun decode(json: JSONObject): SkolaOnlineLesson? =
            runCatching {
                SkolaOnlineLesson(
                    sourceId = json.optString("sourceId"),
                    startDateTime = LocalDateTime.parse(json.getString("startDateTime")),
                    endDateTime = LocalDateTime.parse(json.getString("endDateTime")),
                    label = json.optString("label", "?"),
                    subjectName = json.optString("subjectName"),
                    hourTypeId = json.optString("hourTypeId"),
                    hourTypeDescription = json.optString("hourTypeDescription"),
                    hourKindId = json.optString("hourKindId"),
                    hourKindDescription = json.optString("hourKindDescription")
                )
            }.getOrNull()
    }
}

internal sealed interface SkolaOnlineSyncResult {
    data class Success(val snapshot: SkolaOnlineScheduleSnapshot) : SkolaOnlineSyncResult
    data class Failure(val reason: String, val cached: SkolaOnlineScheduleSnapshot?) : SkolaOnlineSyncResult
    data object Disabled : SkolaOnlineSyncResult
}
