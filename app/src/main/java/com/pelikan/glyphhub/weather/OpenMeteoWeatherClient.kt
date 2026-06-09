package com.pelikan.glyphhub.weather

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.roundToInt

class OpenMeteoWeatherClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun geocode(city: String): WeatherLocation? {
        val query = city.trim()
        if (query.isBlank()) return null
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        val body = execute(url.toString()) ?: return null
        val results = JSONObject(body).optJSONArray("results") ?: return null
        val first = results.optJSONObject(0) ?: return null
        return WeatherLocation(
            name = first.optString("name", query),
            latitude = first.optDouble("latitude"),
            longitude = first.optDouble("longitude")
        )
    }

    fun currentWeather(location: WeatherLocation): CurrentWeather? {
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", location.latitude.toString())
            .addQueryParameter("longitude", location.longitude.toString())
            .addQueryParameter("current", "temperature_2m,weather_code")
            .addQueryParameter("timezone", "auto")
            .build()
        val body = execute(url.toString()) ?: return null
        val current = JSONObject(body).optJSONObject("current") ?: return null
        return CurrentWeather(
            city = location.name,
            temperatureC = current.optDouble("temperature_2m", 0.0).roundToInt(),
            weatherCode = current.optInt("weather_code", -1),
            fetchedAtMs = System.currentTimeMillis(),
            source = WeatherSource.Network
        )
    }

    private fun execute(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GlyphHub/0.1 Open-Meteo client")
            .build()
        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        }.getOrNull()
    }
}
