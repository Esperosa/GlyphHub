package com.pelikan.glyphhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.settings.DefaultDisplayMode
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.ui.components.NothingButton

@Composable
fun DefaultDisplaySettingsScreen(
    appSettings: AppSettings,
    modules: List<GlyphToyModule>,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    var modeMenu by remember { mutableStateOf(false) }
    var toyMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DEFAULT DISPLAY", style = MaterialTheme.typography.titleMedium)
        NothingButton(appSettings.defaultDisplayMode.label, onClick = { modeMenu = true })
        DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
            DefaultDisplayMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        modeMenu = false
                        onUpdateSettings { it.copy(defaultDisplayMode = mode) }
                    }
                )
            }
        }
        NothingButton(
            modules.firstOrNull { it.id == appSettings.defaultToyId }?.shortName ?: appSettings.defaultToyId,
            onClick = { toyMenu = true }
        )
        DropdownMenu(expanded = toyMenu, onDismissRequest = { toyMenu = false }) {
            modules.filter { it.supportsAod }.forEach { module ->
                DropdownMenuItem(
                    text = { Text(module.name) },
                    onClick = {
                        toyMenu = false
                        onUpdateSettings { it.copy(defaultToyId = module.id) }
                    }
                )
            }
        }
    }
}
