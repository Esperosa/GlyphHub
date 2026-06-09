package com.pelikan.glyphhub.settings

enum class DefaultDisplayMode(val id: String, val label: String) {
    Off("off", "Off"),
    Clock("clock", "Clock"),
    Battery("battery", "Battery"),
    Date("date", "Date"),
    CustomGlyph("custom_glyph", "Custom glyph"),
    LastActive("last_active", "Last active"),
    SelectedToy("selected_toy", "Selected toy");

    companion object {
        fun fromId(id: String): DefaultDisplayMode =
            entries.firstOrNull { it.id == id } ?: Off
    }
}

data class AppSettings(
    val selectedToyId: String = "dice",
    val activeToyId: String? = null,
    val lastActiveToyId: String? = null,
    val defaultDisplayMode: DefaultDisplayMode = DefaultDisplayMode.Clock,
    val defaultToyId: String = "clock",
    val autoReturnToDefault: Boolean = false,
    val autoReturnDelaySeconds: Int = 30,
    val globalBrightness: Int = 80,
    val aodEnabled: Boolean = true,
    val sensorsEnabled: Boolean = true,
    val widgetHapticsEnabled: Boolean = false,
    val activationAnimation: String = "appear_from_center",
    val deactivationAnimation: String = "disappear_to_center",
    val activeAccentColor: String = "#E60012",
    val debugMode: Boolean = false,
    val showExperimentalToys: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val nightStart: String = "22:00",
    val nightEnd: String = "07:00",
    val scheduleExceptionIds: String = "timer,alarm,charging_full,payment",
    val webPortalEnabled: Boolean = false,
    val webPortalPort: Int = 8765,
    val webPortalToken: String = "",
    val lastActivationTimestamp: Long = 0L,
    val lastSelectedToyIndex: Int = 0,
    val widgetCarouselIndex: Int = 0,
    val widgetPanelMode: String = "main",
    val widgetPanelSettingIndex: Int = 0
)
