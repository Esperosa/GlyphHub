package com.pelikan.glyphhub.settings

object SettingsSerializer {
    fun sanitizeBrightness(value: Int): Int = value.coerceIn(0, 100)

    fun sanitizeDelaySeconds(value: Int): Int = value.coerceIn(0, 3600)

    fun parseAccentColor(value: String): String =
        if (Regex("^#[0-9A-Fa-f]{6}$").matches(value)) value.uppercase() else "#E60012"
}
