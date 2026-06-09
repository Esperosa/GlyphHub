package com.pelikan.glyphhub.settings

data class ToySettings(
    val values: Map<String, String>
) {
    fun bool(key: String, defaultValue: Boolean = false): Boolean =
        values[key]?.toBooleanStrictOrNull() ?: defaultValue

    fun int(key: String, defaultValue: Int = 0): Int =
        values[key]?.toIntOrNull() ?: defaultValue

    fun text(key: String, defaultValue: String = ""): String =
        values[key] ?: defaultValue

    fun choice(key: String, defaultValue: String = ""): String =
        values[key] ?: defaultValue

    fun withValue(key: String, value: String): ToySettings =
        copy(values = values + (key to value))

    companion object {
        fun defaultFor(schema: ToySettingsSchema): ToySettings =
            ToySettings(schema.entries.associate { it.key to it.defaultValue })
    }
}
