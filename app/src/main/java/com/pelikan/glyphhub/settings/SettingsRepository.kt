package com.pelikan.glyphhub.settings

import android.content.Context
import android.util.Log
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.widget.GlyphHubWidgetActions
import com.pelikan.glyphhub.widget.GlyphHubWidgetState

object SettingsRepository {
    const val LOG_TAG = "GlyphHub"
    private const val PREFS = "glyphhub_settings"
    private const val LOGS_KEY = "debug_logs"
    private const val MAX_LOG_LINES = 80

    fun appSettings(context: Context): AppSettings {
        val prefs = prefs(context)
        return normalize(
            AppSettings(
                selectedToyId = prefs.getString("selectedToyId", "dice") ?: "dice",
                activeToyId = prefs.getString("activeToyId", null),
                lastActiveToyId = prefs.getString("lastActiveToyId", null),
                defaultDisplayMode = DefaultDisplayMode.fromId(
                    prefs.getString("defaultDisplayMode", DefaultDisplayMode.Clock.id) ?: DefaultDisplayMode.Clock.id
                ),
                defaultToyId = prefs.getString("defaultToyId", "clock") ?: "clock",
                autoReturnToDefault = prefs.getBoolean("autoReturnToDefault", false),
                autoReturnDelaySeconds = prefs.getInt("autoReturnDelaySeconds", 30),
                globalBrightness = prefs.getInt("globalBrightness", 80),
                aodEnabled = prefs.getBoolean("aodEnabled", true),
                sensorsEnabled = prefs.getBoolean("sensorsEnabled", true),
                widgetHapticsEnabled = prefs.getBoolean("widgetHapticsEnabled", false),
                activationAnimation = prefs.getString("activationAnimation", "appear_from_center") ?: "appear_from_center",
                deactivationAnimation = prefs.getString("deactivationAnimation", "disappear_to_center") ?: "disappear_to_center",
                activeAccentColor = prefs.getString("activeAccentColor", "#E60012") ?: "#E60012",
                debugMode = prefs.getBoolean("debugMode", false),
                showExperimentalToys = prefs.getBoolean("showExperimentalToys", false),
                scheduleEnabled = prefs.getBoolean("scheduleEnabled", false),
                nightStart = prefs.getString("nightStart", "22:00") ?: "22:00",
                nightEnd = prefs.getString("nightEnd", "07:00") ?: "07:00",
                scheduleExceptionIds = prefs.getString("scheduleExceptionIds", "timer,alarm,charging_full,payment")
                    ?: "timer,alarm,charging_full,payment",
                webPortalEnabled = prefs.getBoolean("webPortalEnabled", false),
                webPortalPort = prefs.getInt("webPortalPort", 8765),
                webPortalToken = prefs.getString("webPortalToken", "") ?: "",
                lastActivationTimestamp = prefs.getLong("lastActivationTimestamp", 0L),
                lastSelectedToyIndex = prefs.getInt("lastSelectedToyIndex", 0),
                widgetCarouselIndex = prefs.getInt("widgetCarouselIndex", 0),
                widgetPanelMode = prefs.getString("widgetPanelMode", GlyphHubWidgetActions.PANEL_MAIN)
                    ?: GlyphHubWidgetActions.PANEL_MAIN,
                widgetPanelSettingIndex = prefs.getInt("widgetPanelSettingIndex", 0)
            )
        )
    }

    fun updateAppSettings(context: Context, transform: (AppSettings) -> AppSettings): AppSettings {
        val updated = normalize(transform(appSettings(context)))
        prefs(context).edit()
            .putString("selectedToyId", updated.selectedToyId)
            .putString("activeToyId", updated.activeToyId)
            .putString("lastActiveToyId", updated.lastActiveToyId)
            .putString("defaultDisplayMode", updated.defaultDisplayMode.id)
            .putString("defaultToyId", updated.defaultToyId)
            .putBoolean("autoReturnToDefault", updated.autoReturnToDefault)
            .putInt("autoReturnDelaySeconds", SettingsSerializer.sanitizeDelaySeconds(updated.autoReturnDelaySeconds))
            .putInt("globalBrightness", SettingsSerializer.sanitizeBrightness(updated.globalBrightness))
            .putBoolean("aodEnabled", updated.aodEnabled)
            .putBoolean("sensorsEnabled", updated.sensorsEnabled)
            .putBoolean("widgetHapticsEnabled", updated.widgetHapticsEnabled)
            .putString("activationAnimation", updated.activationAnimation)
            .putString("deactivationAnimation", updated.deactivationAnimation)
            .putString("activeAccentColor", SettingsSerializer.parseAccentColor(updated.activeAccentColor))
            .putBoolean("debugMode", updated.debugMode)
            .putBoolean("showExperimentalToys", updated.showExperimentalToys)
            .putBoolean("scheduleEnabled", updated.scheduleEnabled)
            .putString("nightStart", updated.nightStart)
            .putString("nightEnd", updated.nightEnd)
            .putString("scheduleExceptionIds", updated.scheduleExceptionIds)
            .putBoolean("webPortalEnabled", updated.webPortalEnabled)
            .putInt("webPortalPort", updated.webPortalPort.coerceIn(1024, 65535))
            .putString("webPortalToken", updated.webPortalToken)
            .putLong("lastActivationTimestamp", updated.lastActivationTimestamp)
            .putInt("lastSelectedToyIndex", updated.lastSelectedToyIndex)
            .putInt("widgetCarouselIndex", updated.widgetCarouselIndex)
            .putString("widgetPanelMode", updated.widgetPanelMode)
            .putInt("widgetPanelSettingIndex", updated.widgetPanelSettingIndex)
            .apply()
        appendLog(context, "settings updated selected=${updated.selectedToyId} active=${updated.activeToyId}")
        return updated
    }

    fun selectToy(context: Context, toyId: String): AppSettings {
        val current = appSettings(context)
        val availableModules = availableCarouselModules(current)
        val resolvedToyId = availableModules.firstOrNull { it.id == toyId }?.id
            ?: availableModules.firstOrNull()?.id
            ?: ToyRegistry.defaultToy().id
        val index = availableModules.indexOfFirst { it.id == resolvedToyId }.coerceAtLeast(0)
        return updateAppSettings(context) {
            it.copy(
                selectedToyId = resolvedToyId,
                lastSelectedToyIndex = index,
                widgetCarouselIndex = index,
                widgetPanelMode = GlyphHubWidgetActions.PANEL_MAIN,
                widgetPanelSettingIndex = 0
            )
        }
    }

    fun selectNextToy(context: Context): AppSettings {
        val settings = appSettings(context)
        val modules = availableCarouselModules(settings)
        if (modules.isEmpty()) return appSettings(context)
        val currentIndex = carouselIndex(settings)
        return selectToy(context, modules[(currentIndex + 1).floorMod(modules.size)].id)
    }

    fun selectPreviousToy(context: Context): AppSettings {
        val settings = appSettings(context)
        val modules = availableCarouselModules(settings)
        if (modules.isEmpty()) return appSettings(context)
        val currentIndex = carouselIndex(settings)
        return selectToy(context, modules[(currentIndex - 1).floorMod(modules.size)].id)
    }

    fun activateToy(context: Context, toyId: String): AppSettings =
        updateAppSettings(context) {
            val clockAsDefault = toyId == "clock"
            it.copy(
                selectedToyId = toyId,
                activeToyId = toyId,
                lastActiveToyId = toyId,
                defaultDisplayMode = if (clockAsDefault) DefaultDisplayMode.Clock else it.defaultDisplayMode,
                defaultToyId = if (clockAsDefault) "clock" else it.defaultToyId,
                aodEnabled = if (clockAsDefault) true else it.aodEnabled,
                lastActivationTimestamp = System.currentTimeMillis(),
                lastSelectedToyIndex = availableCarouselModules(it).indexOfFirst { module -> module.id == toyId }.coerceAtLeast(0),
                widgetCarouselIndex = availableCarouselModules(it).indexOfFirst { module -> module.id == toyId }.coerceAtLeast(0)
            )
        }

    fun deactivateCurrentToy(context: Context): AppSettings =
        updateAppSettings(context) {
            it.copy(activeToyId = null)
        }

    fun openWidgetPanel(context: Context, mode: String): AppSettings =
        updateAppSettings(context) {
            it.copy(
                widgetPanelMode = mode.takeIf { panelMode ->
                    panelMode == GlyphHubWidgetActions.PANEL_TOY || panelMode == GlyphHubWidgetActions.PANEL_APP
                } ?: GlyphHubWidgetActions.PANEL_MAIN,
                widgetPanelSettingIndex = 0
            )
        }

    fun closeWidgetPanel(context: Context): AppSettings =
        updateAppSettings(context) {
            it.copy(widgetPanelMode = GlyphHubWidgetActions.PANEL_MAIN, widgetPanelSettingIndex = 0)
        }

    fun selectWidgetPanelSetting(context: Context, direction: Int): AppSettings {
        val settings = appSettings(context)
        val count = widgetPanelSettingCount(context, settings)
        if (count <= 0) return settings
        return updateAppSettings(context) {
            it.copy(widgetPanelSettingIndex = (settings.widgetPanelSettingIndex + direction).floorMod(count))
        }
    }

    fun adjustWidgetPanelSetting(context: Context, delta: Int) {
        val settings = appSettings(context)
        when (settings.widgetPanelMode) {
            GlyphHubWidgetActions.PANEL_APP -> adjustAppWidgetSetting(context, settings, delta)
            GlyphHubWidgetActions.PANEL_TOY -> adjustToyWidgetSetting(context, settings, delta)
        }
    }

    fun toggleWidgetPanelSetting(context: Context) {
        val settings = appSettings(context)
        when (settings.widgetPanelMode) {
            GlyphHubWidgetActions.PANEL_APP -> adjustAppWidgetSetting(context, settings, 1)
            GlyphHubWidgetActions.PANEL_TOY -> adjustToyWidgetSetting(context, settings, 1)
        }
    }

    fun getToySettings(context: Context, toyId: String, schema: ToySettingsSchema): ToySettings {
        val prefs = prefs(context)
        val values = schema.entries.associate { definition ->
            val key = toyKey(toyId, definition.key)
            definition.key to (prefs.getString(key, definition.defaultValue) ?: definition.defaultValue)
        }
        return ToySettings(values)
    }

    fun updateToySettings(context: Context, toyId: String, settings: ToySettings) {
        val edit = prefs(context).edit()
        settings.values.forEach { (key, value) ->
            edit.putString(toyKey(toyId, key), value)
        }
        edit.apply()
        appendLog(context, "toy settings updated toy=$toyId")
    }

    fun updateToySetting(context: Context, toyId: String, key: String, value: String) {
        prefs(context).edit().putString(toyKey(toyId, key), value).apply()
        appendLog(context, "toy setting updated toy=$toyId key=$key value=$value")
    }

    fun widgetState(context: Context): GlyphHubWidgetState {
        val settings = appSettings(context)
        return GlyphHubWidgetState(
            selectedToyId = settings.selectedToyId,
            activeToyId = settings.activeToyId,
            isCurrentToyActive = settings.activeToyId == settings.selectedToyId,
            lastActivationTimestamp = settings.lastActivationTimestamp,
            lastSelectedToyIndex = settings.lastSelectedToyIndex,
            widgetCarouselIndex = settings.widgetCarouselIndex,
            widgetPanelMode = settings.widgetPanelMode,
            widgetPanelSettingIndex = settings.widgetPanelSettingIndex
        )
    }

    fun appendLog(context: Context, message: String) {
        Log.d(LOG_TAG, message)
        val prefs = prefs(context)
        val lines = debugLogs(context).toMutableList()
        lines += "${System.currentTimeMillis()}: $message"
        prefs.edit().putString(LOGS_KEY, lines.takeLast(MAX_LOG_LINES).joinToString("\n")).apply()
    }

    fun debugLogs(context: Context): List<String> =
        prefs(context).getString(LOGS_KEY, null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            ?: emptyList()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun toyKey(toyId: String, key: String) = "toy.$toyId.$key"

    private fun normalize(settings: AppSettings): AppSettings {
        val carouselModules = availableCarouselModules(settings)
        if (carouselModules.isEmpty()) return settings
        val selectedIndex = carouselModules.indexOfFirst { it.id == settings.selectedToyId }.takeIf { it >= 0 }
            ?: settings.widgetCarouselIndex.floorMod(carouselModules.size)
        val selectedToyId = carouselModules[selectedIndex].id
        val activeToyId = settings.activeToyId?.takeIf { ToyRegistry.byId(it) != null }
        val lastActiveToyId = settings.lastActiveToyId?.takeIf { ToyRegistry.byId(it) != null }
        val defaultToyId = settings.defaultToyId.takeIf { ToyRegistry.byId(it)?.supportsAod == true }
            ?: ToyRegistry.defaultToy().id
        return settings.copy(
            selectedToyId = selectedToyId,
            activeToyId = activeToyId,
            lastActiveToyId = lastActiveToyId,
            defaultToyId = defaultToyId,
            lastSelectedToyIndex = selectedIndex,
            widgetCarouselIndex = selectedIndex,
            nightStart = settings.nightStart.takeIf { Regex("^\\d{2}:\\d{2}$").matches(it) } ?: "22:00",
            nightEnd = settings.nightEnd.takeIf { Regex("^\\d{2}:\\d{2}$").matches(it) } ?: "07:00",
            webPortalPort = settings.webPortalPort.coerceIn(1024, 65535),
            widgetPanelMode = settings.widgetPanelMode.takeIf {
                it == GlyphHubWidgetActions.PANEL_MAIN ||
                    it == GlyphHubWidgetActions.PANEL_TOY ||
                    it == GlyphHubWidgetActions.PANEL_APP
            } ?: GlyphHubWidgetActions.PANEL_MAIN,
            widgetPanelSettingIndex = settings.widgetPanelSettingIndex.coerceAtLeast(0)
        )
    }

    private fun carouselIndex(settings: AppSettings): Int {
        val modules = availableCarouselModules(settings)
        if (modules.isEmpty()) return 0
        return modules.indexOfFirst { it.id == settings.selectedToyId }.takeIf { it >= 0 }
            ?: settings.widgetCarouselIndex.floorMod(modules.size)
    }

    private fun widgetPanelSettingCount(context: Context, settings: AppSettings): Int =
        when (settings.widgetPanelMode) {
            GlyphHubWidgetActions.PANEL_APP -> APP_WIDGET_SETTINGS.size
            GlyphHubWidgetActions.PANEL_TOY -> {
                val module = ToyRegistry.byId(settings.selectedToyId) ?: return 0
                module.settingsSchema.widgetEntries(settings.debugMode).size
            }
            else -> 0
        }

    private fun adjustAppWidgetSetting(context: Context, settings: AppSettings, delta: Int) {
        when (APP_WIDGET_SETTINGS[settings.widgetPanelSettingIndex.floorMod(APP_WIDGET_SETTINGS.size)]) {
            "aod" -> updateAppSettings(context) { it.copy(aodEnabled = !it.aodEnabled) }
            "sensors" -> updateAppSettings(context) { it.copy(sensorsEnabled = !it.sensorsEnabled) }
            "autoReturn" -> updateAppSettings(context) { it.copy(autoReturnToDefault = !it.autoReturnToDefault) }
            "experimental" -> updateAppSettings(context) { it.copy(showExperimentalToys = !it.showExperimentalToys) }
        }
    }

    private fun adjustToyWidgetSetting(context: Context, settings: AppSettings, delta: Int) {
        val module = ToyRegistry.byId(settings.selectedToyId) ?: return
        val definitions = module.settingsSchema.widgetEntries(settings.debugMode)
        if (definitions.isEmpty()) return
        val definition = definitions.getOrNull(settings.widgetPanelSettingIndex.floorMod(definitions.size)) ?: return
        val toySettings = getToySettings(context, module.id, module.settingsSchema)
        val current = toySettings.values[definition.key] ?: definition.defaultValue
        val next = when (definition.type) {
            ToySettingType.Boolean -> (!(current.toBooleanStrictOrNull() ?: current.equals("true", true))).toString()
            ToySettingType.Int -> {
                val min = definition.min ?: 0
                val max = definition.max ?: 100
                val step = if (max - min > 50) 10 else 1
                (current.toIntOrNull() ?: definition.defaultValue.toIntOrNull() ?: min)
                    .plus(delta * step)
                    .coerceIn(min, max)
                    .toString()
            }
            ToySettingType.Choice -> {
                val options = definition.options
                if (options.isEmpty()) current else {
                    val index = options.indexOfFirst { it.id == current }.takeIf { it >= 0 } ?: 0
                    options[(index + delta).floorMod(options.size)].id
                }
            }
            ToySettingType.Text -> current
        }
        updateToySetting(context, module.id, definition.key, next)
    }

    private fun ToySettingsSchema.widgetEntries(debugMode: Boolean) =
        quickEntries(debugMode)
            .filter { it.type != ToySettingType.Text }
            .take(TOY_WIDGET_SETTING_LIMIT)

    private fun availableCarouselModules(settings: AppSettings): List<com.pelikan.glyphhub.toys.GlyphToyModule> {
        val modules = ToyRegistry.carouselModules(settings.showExperimentalToys)
        return if (modules.isNotEmpty()) modules else ToyRegistry.modules.filter { it.id != "idle_default" }
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

    private val APP_WIDGET_SETTINGS = listOf("aod", "sensors", "autoReturn", "experimental")
    private const val TOY_WIDGET_SETTING_LIMIT = 4
}
