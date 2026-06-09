package com.pelikan.glyphhub.schoolonline

import android.content.Context
import java.time.LocalDate

internal class SkolaOnlineRepository(
    context: Context,
    private val client: SkolaOnlineClient = SkolaOnlineClient()
) {
    private val appContext = context.applicationContext
    private val secureStore = SkolaOnlineSecureStore(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun accountStatus(): SkolaOnlineAccountStatus =
        secureStore.accountStatus()

    fun saveAccount(enabled: Boolean, server: SkolaOnlineServer, username: String, password: String?) {
        secureStore.saveAccount(enabled, server, username, password?.takeIf { it.isNotBlank() })
    }

    fun setEnabled(enabled: Boolean) {
        secureStore.setEnabled(enabled)
    }

    fun cachedSchedule(): SkolaOnlineScheduleSnapshot? =
        prefs.getString(KEY_SCHEDULE_CACHE, null)?.let(SkolaOnlineScheduleSnapshot::decode)

    fun refresh(
        dateFrom: LocalDate = LocalDate.now(),
        days: Long = DEFAULT_SYNC_DAYS
    ): SkolaOnlineSyncResult {
        val status = secureStore.accountStatus()
        if (!status.enabled) return SkolaOnlineSyncResult.Disabled
        val server = status.server
        val token = validToken(server)
            ?: return failure("auth_failed")
        val dateTo = dateFrom.plusDays((days - 1).coerceAtLeast(0))
        val body = client.timetable(server, token.accessToken, dateFrom, dateTo)
            ?: return failure("timetable_failed")
        val snapshot = runCatching {
            SkolaOnlineTimetableParser.parse(
                body = body,
                fetchedAtMs = System.currentTimeMillis(),
                fallbackDateFrom = dateFrom,
                fallbackDateTo = dateTo
            )
        }.getOrElse {
            return failure("parse_failed")
        }
        prefs.edit().putString(KEY_SCHEDULE_CACHE, snapshot.encode()).apply()
        secureStore.saveSyncStatus("ok:${snapshot.lessons.size}")
        return SkolaOnlineSyncResult.Success(snapshot)
    }

    private fun validToken(server: SkolaOnlineServer): SkolaOnlineTokenSet? {
        secureStore.tokenSet()?.takeIf { it.isFresh() }?.let { return it }
        val refreshed = secureStore.tokenSet()?.refreshToken?.let { client.refresh(server, it) }
        if (refreshed != null) {
            secureStore.saveTokens(refreshed)
            return refreshed
        }
        val username = secureStore.username()
        val password = secureStore.password().orEmpty()
        val loggedIn = client.login(server, username, password)
        if (loggedIn != null) {
            secureStore.saveTokens(loggedIn)
        }
        return loggedIn
    }

    private fun failure(reason: String): SkolaOnlineSyncResult.Failure {
        secureStore.saveSyncStatus(reason)
        return SkolaOnlineSyncResult.Failure(reason, cachedSchedule())
    }

    private companion object {
        const val PREFS = "glyphhub_school_online"
        const val KEY_SCHEDULE_CACHE = "schedule_cache"
        const val DEFAULT_SYNC_DAYS = 8L
    }
}
