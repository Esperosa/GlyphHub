package com.pelikan.glyphhub.weather

import android.content.Context
import org.json.JSONObject

class WeatherRepository(
    context: Context,
    private val client: OpenMeteoWeatherClient = OpenMeteoWeatherClient()
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cached(): CurrentWeather? =
        prefs.getString(KEY_CACHE, null)?.let(::decode)

    fun refresh(city: String): WeatherResult {
        val location = client.geocode(city)
            ?: return WeatherResult.Failure("city_not_found", cached())
        val weather = client.currentWeather(location)
            ?: return WeatherResult.Failure("fetch_failed", cached())
        save(weather)
        return WeatherResult.Success(weather)
    }

    fun save(weather: CurrentWeather) {
        prefs.edit().putString(KEY_CACHE, encode(weather)).apply()
    }

    private fun encode(weather: CurrentWeather): String =
        JSONObject()
            .put("city", weather.city)
            .put("temperatureC", weather.temperatureC)
            .put("weatherCode", weather.weatherCode)
            .put("fetchedAtMs", weather.fetchedAtMs)
            .toString()

    private fun decode(value: String): CurrentWeather? =
        runCatching {
            val json = JSONObject(value)
            CurrentWeather(
                city = json.optString("city", ""),
                temperatureC = json.optInt("temperatureC", 0),
                weatherCode = json.optInt("weatherCode", -1),
                fetchedAtMs = json.optLong("fetchedAtMs", 0L),
                source = WeatherSource.Cache
            )
        }.getOrNull()

    private companion object {
        const val PREFS = "glyphhub_weather"
        const val KEY_CACHE = "last_weather"
    }
}
