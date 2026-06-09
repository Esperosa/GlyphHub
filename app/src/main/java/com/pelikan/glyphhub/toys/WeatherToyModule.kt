package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import com.pelikan.glyphhub.weather.CurrentWeather
import com.pelikan.glyphhub.weather.WeatherCondition
import com.pelikan.glyphhub.weather.WeatherRepository
import com.pelikan.glyphhub.weather.WeatherResult

class WeatherToyModule : BaseGlyphToyModule() {
    override val id = "weather"
    override val name = "Weather Toy"
    override val shortName = "WX"
    override val description = "Open-Meteo weather panel with cached offline fallback."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "city",
                "City",
                "City name used for Open-Meteo geocoding.",
                ToySettingType.Text,
                "Prague",
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Weather Matrix display mode.",
                ToySettingType.Choice,
                "icon_temp",
                options = listOf(
                    ToySettingOption("icon_temp", "Icon + temp"),
                    ToySettingOption("icon", "Icon"),
                    ToySettingOption("temp", "Temp")
                ),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("units", "Units", "Displayed temperature units.", ToySettingType.Choice, "c", options = listOf(ToySettingOption("c", "C"), ToySettingOption("f", "F"))),
            ToySettingDefinition("refreshMinutes", "Refresh min", "Minimum interval between network refreshes.", ToySettingType.Int, "30", 5, 240),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var elapsedMs = 0L
    @Volatile private var weather: CurrentWeather? = null
    @Volatile private var refreshInFlight = false
    @Volatile private var lastError: String? = null
    private var repository: WeatherRepository? = null
    private var lastRefreshAtMs = 0L
    private var lastCity = ""
    @Volatile private var generation = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        generation += 1
        elapsedMs = 0L
        repository = WeatherRepository(context.androidContext)
        weather = repository?.cached()
        maybeRefresh(force = true)
    }

    override fun onDeactivate() {
        generation += 1
        repository = null
        refreshInFlight = false
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        val brightness = getSettings().int("brightness", 80)
        maybeRefresh(force = false)
        val current = weather
        if (current == null) {
            return if (refreshInFlight) drawCenteredText3x5("WX", brightness) else drawCenteredText3x5("SET", brightness)
        }
        val condition = current.condition
        val icon = when (condition) {
            WeatherCondition.Cloudy,
            WeatherCondition.Fog -> cloudyFrame(brightness)
            WeatherCondition.Rain -> rainFrame(brightness)
            WeatherCondition.Storm -> stormFrame(brightness)
            WeatherCondition.Snow -> snowFrame(brightness)
            WeatherCondition.Night -> nightFrame(brightness)
            WeatherCondition.Sunny,
            WeatherCondition.Unknown -> sunnyFrame(brightness)
        }
        val temp = displayTemperature(current)
        return when (getSettings().choice("displayMode", "icon_temp")) {
            "icon" -> icon
            "temp" -> drawCenteredText3x5(temp, brightness)
            else -> icon.overlay(drawCenteredText3x5(temp, brightness, yOffset = 8))
        }
    }

    private fun maybeRefresh(force: Boolean) {
        val repo = repository ?: return
        val city = getSettings().text("city", "Prague").trim()
        if (city.isBlank()) return
        val now = System.currentTimeMillis()
        val interval = getSettings().int("refreshMinutes", 30).coerceIn(5, 240) * 60_000L
        if (!force && city == lastCity && now - lastRefreshAtMs < interval) return
        if (refreshInFlight) return
        refreshInFlight = true
        lastCity = city
        val refreshGeneration = generation
        Thread({
            when (val result = repo.refresh(city)) {
                is WeatherResult.Success -> {
                    if (generation == refreshGeneration) {
                        weather = result.weather
                        lastError = null
                    }
                }
                is WeatherResult.Failure -> {
                    if (generation == refreshGeneration) {
                        weather = result.cached ?: weather
                        lastError = result.reason
                    }
                }
            }
            if (generation == refreshGeneration) {
                lastRefreshAtMs = System.currentTimeMillis()
                refreshInFlight = false
            }
        }, "GlyphHubWeather").apply {
            isDaemon = true
            start()
        }
    }

    private fun displayTemperature(weather: CurrentWeather): String {
        val celsius = weather.temperatureC
        val value = if (getSettings().choice("units", "c") == "f") {
            (celsius * 9 / 5) + 32
        } else {
            celsius
        }
        return value.coerceIn(-99, 99).toString()
    }

    private fun sunnyFrame(brightness: Int): GlyphFrame {
        return frameFromRows(
            listOf(
                "0000001000000",
                "0000011100000",
                "0000111110000",
                "0001110111000",
                "0011100011100",
                "0001110111000",
                "0000111110000",
                "0000011100000",
                "0000001000000",
                "0000000000000",
                "0000000000000",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        )
    }

    private fun cloudyFrame(brightness: Int): GlyphFrame =
        frameFromRows(
            listOf(
                "0000000000000",
                "0000000000000",
                "0000011100000",
                "0001111110000",
                "0011111111000",
                "0111111111100",
                "0111111111110",
                "0011111111110",
                "0001111111100",
                "0000011111000",
                "0000000000000",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        )

    private fun rainFrame(brightness: Int): GlyphFrame {
        val drops = setOf(5 to 9, 7 to 10, 9 to 9)
        return cloudyFrame(brightness).overlay(frameFromPoints(drops, brightness))
    }

    private fun stormFrame(brightness: Int): GlyphFrame {
        val bolt = setOf(6 to 8, 7 to 8, 6 to 9, 7 to 10, 8 to 10)
        return cloudyFrame(brightness).overlay(frameFromPoints(bolt, brightness))
    }

    private fun snowFrame(brightness: Int): GlyphFrame {
        val flakes = setOf(5 to 9, 7 to 9, 6 to 10)
        return frameFromRows(
            listOf(
                "0000001000000",
                "0000011100000",
                "0001001001000",
                "0010101010100",
                "0001001001000",
                "0000011100000",
                "0000001000000",
                "0000000000000",
                "0000000000000",
                "0000000000000",
                "0000000000000",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        ).overlay(frameFromPoints(flakes, brightness))
    }

    private fun nightFrame(brightness: Int): GlyphFrame =
        frameFromRows(
            listOf(
                "0000011100000",
                "0001111110000",
                "0011110111000",
                "0011100011000",
                "0011000001000",
                "0011000001000",
                "0011100011000",
                "0011110111000",
                "0001111110000",
                "0000011100000",
                "0000000000000",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        )
}
