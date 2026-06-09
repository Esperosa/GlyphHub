package com.pelikan.glyphhub.settings

enum class ToySettingType {
    Boolean,
    Int,
    Text,
    Choice
}

enum class ToySettingScope {
    QUICK,
    NORMAL,
    ADVANCED
}

data class ToySettingOption(
    val id: String,
    val label: String
)

data class ToySettingDefinition(
    val key: String,
    val title: String,
    val description: String,
    val type: ToySettingType,
    val defaultValue: String,
    val min: Int? = null,
    val max: Int? = null,
    val options: List<ToySettingOption> = emptyList(),
    val scope: ToySettingScope = ToySettingScope.NORMAL,
    val visibleByDefault: Boolean = true,
    val requiresDebugMode: Boolean = false
)

data class ToySettingsSchema(
    val entries: List<ToySettingDefinition>
) {
    fun definition(key: String): ToySettingDefinition? = entries.firstOrNull { it.key == key }

    fun quickEntries(debugMode: Boolean = false): List<ToySettingDefinition> {
        val explicit = entries.filter { it.scope == ToySettingScope.QUICK && it.isVisible(debugMode) }
        if (explicit.isNotEmpty()) return explicit.take(4)
        return entries
            .filter { it.isVisible(debugMode) && it.isPracticalQuickSetting() }
            .take(4)
    }

    fun normalEntries(debugMode: Boolean = false): List<ToySettingDefinition> {
        val quickKeys = quickEntries(debugMode).map { it.key }.toSet()
        return entries.filter { definition ->
            definition.key !in quickKeys &&
                definition.isVisible(debugMode) &&
                definition.scope != ToySettingScope.ADVANCED &&
                !definition.isAdvancedByHeuristic()
        }
    }

    fun advancedEntries(debugMode: Boolean = false): List<ToySettingDefinition> {
        if (!debugMode) return emptyList()
        val quickKeys = quickEntries(debugMode).map { it.key }.toSet()
        return entries.filter { definition ->
            definition.key !in quickKeys &&
                definition.isVisible(debugMode) &&
                (definition.scope == ToySettingScope.ADVANCED || definition.isAdvancedByHeuristic())
        }
    }

    private fun ToySettingDefinition.isVisible(debugMode: Boolean): Boolean =
        visibleByDefault && (!requiresDebugMode || debugMode)

    private fun ToySettingDefinition.isPracticalQuickSetting(): Boolean {
        if (scope == ToySettingScope.QUICK) return true
        return key in quickSettingKeys || title.lowercase() in quickSettingTitles
    }

    private fun ToySettingDefinition.isAdvancedByHeuristic(): Boolean {
        if (scope == ToySettingScope.ADVANCED) return true
        val normalizedKey = key.lowercase()
        return normalizedKey == "brightness" ||
            normalizedKey.contains("threshold") ||
            normalizedKey.contains("cooldown") ||
            normalizedKey.contains("offset") ||
            normalizedKey.contains("smoothing") ||
            normalizedKey.contains("random") ||
            normalizedKey.contains("updaterate") ||
            normalizedKey.contains("frameduration") ||
            normalizedKey.endsWith("ms") ||
            normalizedKey.endsWith("percent")
    }

    private companion object {
        val quickSettingKeys = setOf(
            "sides",
            "shakeToRoll",
            "shakeToFlip",
            "resultSet",
            "displayMode",
            "showPercentage",
            "format24h",
            "selectedGlyphAsset",
            "text",
            "direction",
            "speed",
            "durationSeconds",
            "repeat",
            "hapticGuidance",
            "style",
            "behavior",
            "city",
            "instrument",
            "tuningPreset",
            "activeTimetableProfile"
        )

        val quickSettingTitles = setOf(
            "display",
            "mode",
            "repeat",
            "haptics"
        )
    }
}
