package com.pelikan.glyphhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.toys.ToyVisibilityState
import com.pelikan.glyphhub.ui.components.MatrixPreview
import com.pelikan.glyphhub.ui.components.NothingButton
import com.pelikan.glyphhub.ui.components.NothingSwitch
import com.pelikan.glyphhub.ui.components.SettingsRow
import com.pelikan.glyphhub.ui.theme.GlyphBlack
import com.pelikan.glyphhub.ui.theme.GlyphMuted
import com.pelikan.glyphhub.ui.theme.GlyphPanel
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite

@Composable
fun HomeScreen(
    modules: List<GlyphToyModule>,
    appSettings: AppSettings,
    settingsRepository: SettingsRepository,
    onToggleToy: (String) -> Unit,
    onSelectPrevious: () -> Unit,
    onSelectNext: () -> Unit,
    onOpenToySettings: (String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selected = modules.firstOrNull { it.id == appSettings.selectedToyId } ?: modules.firstOrNull() ?: ToyRegistry.defaultToy()
    val selectedEntry = ToyRegistry.entry(selected.id)
    val selectedState = selectedEntry?.visibility ?: ToyVisibilityState.READY
    val statusText = when {
        appSettings.activeToyId == selected.id -> "ACTIVE"
        selectedState == ToyVisibilityState.NEEDS_SETUP -> "NEEDS SETUP"
        selectedState == ToyVisibilityState.EXPERIMENTAL -> "EXPERIMENTAL"
        else -> "READY"
    }
    selected.updateSettings(settingsRepository.getToySettings(context, selected.id, selected.settingsSchema))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("GlyphHub", style = MaterialTheme.typography.headlineMedium, color = GlyphWhite)
        Text(
            statusText,
            color = if (appSettings.activeToyId == selected.id) GlyphRed else GlyphMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlyphPanel, MaterialTheme.shapes.medium)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MatrixPreview(
                frame = selected.previewFrame(),
                active = appSettings.activeToyId == selected.id,
                modifier = Modifier.size(104.dp)
            )
            Text(selected.name, style = MaterialTheme.typography.titleLarge, color = GlyphWhite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                NothingButton("PREV", onClick = onSelectPrevious)
                NothingButton(
                    text = if (appSettings.activeToyId == selected.id) "STOP" else "START",
                    onClick = { onToggleToy(selected.id) },
                    active = appSettings.activeToyId == selected.id
                )
                NothingButton("NEXT", onClick = onSelectNext)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                NothingButton("SET", onClick = { onOpenToySettings(selected.id) })
            }
        }

        SettingsRow("AOD", "Default display when no Toy is active") {
            NothingSwitch(appSettings.aodEnabled) { checked -> onUpdateSettings { it.copy(aodEnabled = checked) } }
        }
        SettingsRow("Sensors", "Shake and compass input") {
            NothingSwitch(appSettings.sensorsEnabled) { checked -> onUpdateSettings { it.copy(sensorsEnabled = checked) } }
        }
        SettingsRow("Auto return", "Return to default after delay") {
            NothingSwitch(appSettings.autoReturnToDefault) { checked -> onUpdateSettings { it.copy(autoReturnToDefault = checked) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("APP SETTINGS", onClick = onOpenAppSettings)
            if (appSettings.debugMode) {
                NothingButton("DEBUG", onClick = onOpenDebug)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("TOYS", style = MaterialTheme.typography.titleMedium, color = GlyphWhite)
        ToyGridScreen(
            modules = modules,
            appSettings = appSettings,
            settingsRepository = settingsRepository,
            modifier = Modifier.height(560.dp),
            onToggleToy = onToggleToy,
            onOpenToySettings = onOpenToySettings
        )
    }
}
