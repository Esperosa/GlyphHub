package com.pelikan.glyphhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.glyph.GlyphTransitionEngine
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.ui.components.NothingButton
import com.pelikan.glyphhub.ui.components.NothingSwitch
import com.pelikan.glyphhub.ui.components.SettingsRow
import com.pelikan.glyphhub.ui.theme.GlyphBlack
import com.pelikan.glyphhub.ui.theme.GlyphMuted
import java.util.UUID

@Composable
fun AppSettingsScreen(
    appSettings: AppSettings,
    modules: List<GlyphToyModule>,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("BACK", onClick = onBack)
            Text("App Settings", style = MaterialTheme.typography.headlineMedium)
        }
        DefaultDisplaySettingsScreen(appSettings, modules, onUpdateSettings)
        SettingsRow("AOD", null) {
            NothingSwitch(appSettings.aodEnabled) { onUpdateSettings { settings -> settings.copy(aodEnabled = it) } }
        }
        SettingsRow("Sensors", null) {
            NothingSwitch(appSettings.sensorsEnabled) { onUpdateSettings { settings -> settings.copy(sensorsEnabled = it) } }
        }
        SettingsRow("Widget haptics", null) {
            NothingSwitch(appSettings.widgetHapticsEnabled) { onUpdateSettings { settings -> settings.copy(widgetHapticsEnabled = it) } }
        }
        SettingsRow("Debug mode", null) {
            NothingSwitch(appSettings.debugMode) { onUpdateSettings { settings -> settings.copy(debugMode = it) } }
        }
        SettingsRow("Experimental tools", "Show incomplete or setup-required Toys in the carousel") {
            NothingSwitch(appSettings.showExperimentalToys) {
                onUpdateSettings { settings -> settings.copy(showExperimentalToys = it) }
            }
        }
        Text("Schedule / Night", style = MaterialTheme.typography.titleMedium)
        SettingsRow("Night schedule", "Limit Matrix output during the custom quiet window") {
            NothingSwitch(appSettings.scheduleEnabled) {
                onUpdateSettings { settings -> settings.copy(scheduleEnabled = it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = appSettings.nightStart,
                onValueChange = { value -> onUpdateSettings { it.copy(nightStart = value.take(5)) } },
                label = { Text("Start") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = appSettings.nightEnd,
                onValueChange = { value -> onUpdateSettings { it.copy(nightEnd = value.take(5)) } },
                label = { Text("End") },
                modifier = Modifier.weight(1f)
            )
        }
        Text("Exceptions: ${appSettings.scheduleExceptionIds}", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
        Text("Web Portal", style = MaterialTheme.typography.titleMedium)
        SettingsRow("Local portal", "LAN-only NanoHTTPD portal with token required") {
            NothingSwitch(appSettings.webPortalEnabled) { enabled ->
                val token = appSettings.webPortalToken.ifBlank { UUID.randomUUID().toString().take(8) }
                onUpdateSettings { settings -> settings.copy(webPortalEnabled = enabled, webPortalToken = token) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = appSettings.webPortalPort.toString(),
                onValueChange = { value ->
                    onUpdateSettings { settings ->
                        settings.copy(webPortalPort = value.toIntOrNull()?.coerceIn(1024, 65535) ?: settings.webPortalPort)
                    }
                },
                label = { Text("Port") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = appSettings.webPortalToken,
                onValueChange = { value -> onUpdateSettings { it.copy(webPortalToken = value.take(32)) } },
                label = { Text("Token") },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            "Portal: http://device-ip:${appSettings.webPortalPort}/?token=${appSettings.webPortalToken.ifBlank { "generated-on-enable" }}",
            color = GlyphMuted,
            style = MaterialTheme.typography.bodySmall
        )
        if (appSettings.debugMode) {
            Text("Debug frame intensity: ${appSettings.globalBrightness}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = appSettings.globalBrightness.toFloat(),
                onValueChange = { value -> onUpdateSettings { it.copy(globalBrightness = value.toInt()) } },
                valueRange = 1f..100f,
                steps = 98
            )
        }
        TransitionChoice(
            label = "Activation",
            value = appSettings.activationAnimation,
            onChange = { transition -> onUpdateSettings { it.copy(activationAnimation = transition) } }
        )
        TransitionChoice(
            label = "Deactivation",
            value = appSettings.deactivationAnimation,
            onChange = { transition -> onUpdateSettings { it.copy(deactivationAnimation = transition) } }
        )
    }
}

@Composable
private fun TransitionChoice(label: String, value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("$label animation", style = MaterialTheme.typography.bodyLarge)
        NothingButton(value, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GlyphTransitionEngine.transitionIds.forEach { transition ->
                DropdownMenuItem(
                    text = { Text(transition) },
                    onClick = {
                        expanded = false
                        onChange(transition)
                    }
                )
            }
        }
    }
}
