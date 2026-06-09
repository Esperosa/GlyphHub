package com.pelikan.glyphhub.accuracy

import android.content.Context

class CalibrationRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFloat(toolId: String, key: String, defaultValue: Float = 0f): Float =
        prefs.getFloat(prefKey(toolId, key), defaultValue)

    fun saveFloat(toolId: String, key: String, value: Float) {
        prefs.edit().putFloat(prefKey(toolId, key), value).apply()
    }

    fun savePair(toolId: String, xKey: String, xValue: Float, yKey: String, yValue: Float) {
        prefs.edit()
            .putFloat(prefKey(toolId, xKey), xValue)
            .putFloat(prefKey(toolId, yKey), yValue)
            .apply()
    }

    fun clear(toolId: String, vararg keys: String) {
        val edit = prefs.edit()
        keys.forEach { key -> edit.remove(prefKey(toolId, key)) }
        edit.apply()
    }

    private fun prefKey(toolId: String, key: String): String = "tool.$toolId.$key"

    private companion object {
        const val PREFS = "glyphhub_calibration"
    }
}