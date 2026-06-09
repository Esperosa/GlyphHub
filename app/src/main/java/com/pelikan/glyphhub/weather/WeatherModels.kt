package com.pelikan.glyphhub.weather

data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class CurrentWeather(
    val city: String,
    val temperatureC: Int,
    val weatherCode: Int,
    val fetchedAtMs: Long,
    val source: WeatherSource
) {
    val condition: WeatherCondition
        get() = WeatherCondition.fromCode(weatherCode)
}

enum class WeatherSource {
    Network,
    Cache,
    ManualFallback
}

enum class WeatherCondition {
    Sunny,
    Cloudy,
    Rain,
    Storm,
    Snow,
    Fog,
    Night,
    Unknown;

    companion object {
        fun fromCode(code: Int): WeatherCondition = when (code) {
            0 -> Sunny
            1, 2, 3 -> Cloudy
            45, 48 -> Fog
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Rain
            71, 73, 75, 77, 85, 86 -> Snow
            95, 96, 99 -> Storm
            else -> Unknown
        }
    }
}

sealed class WeatherResult {
    data class Success(val weather: CurrentWeather) : WeatherResult()
    data class Failure(val reason: String, val cached: CurrentWeather?) : WeatherResult()
}
