package com.pelikan.glyphhub.schoolonline

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate

internal class SkolaOnlineClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun login(server: SkolaOnlineServer, username: String, password: String): SkolaOnlineTokenSet? {
        if (username.isBlank() || password.isBlank()) return null
        val body = FormBody.Builder()
            .add("grant_type", "password")
            .add("client_id", CLIENT_ID)
            .add("username", username)
            .add("password", password)
            .add("scope", SCOPE)
            .build()
        return requestToken(server, body)
    }

    fun refresh(server: SkolaOnlineServer, refreshToken: String): SkolaOnlineTokenSet? {
        if (refreshToken.isBlank()) return null
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", CLIENT_ID)
            .add("refresh_token", refreshToken)
            .add("scope", SCOPE)
            .build()
        return requestToken(server, body)
    }

    fun timetable(server: SkolaOnlineServer, accessToken: String, dateFrom: LocalDate, dateTo: LocalDate): String? {
        val url = "${server.baseUrl}/api/v1/timeTable".toHttpUrl().newBuilder()
            .addQueryParameter("dateFrom", dateFrom.toString())
            .addQueryParameter("dateTo", dateTo.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        return executeText(request)
    }

    private fun requestToken(server: SkolaOnlineServer, body: FormBody): SkolaOnlineTokenSet? {
        val request = Request.Builder()
            .url("${server.baseUrl}/api/connect/token")
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        val responseBody = executeText(request) ?: return null
        val json = JSONObject(responseBody)
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) return null
        val expiresInSeconds = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        return SkolaOnlineTokenSet(
            accessToken = accessToken,
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            expiresAtMs = System.currentTimeMillis() + expiresInSeconds * 1000L
        )
    }

    private fun executeText(request: Request): String? =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        }.getOrNull()

    private companion object {
        const val CLIENT_ID = "test_client"
        const val SCOPE = "offline_access sol_api"
        const val USER_AGENT = "GlyphHub/0.1 SkolaOnline timetable client"
    }
}
