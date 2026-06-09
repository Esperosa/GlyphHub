package com.pelikan.glyphhub.toys

import android.content.Context
import com.pelikan.glyphhub.settings.SettingsRepository

data class ToyRuntimeContext(
    val androidContext: Context,
    val settingsRepository: SettingsRepository,
    val matrixWidth: Int,
    val matrixHeight: Int,
    val brightness: Int
)
