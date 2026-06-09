package com.pelikan.glyphhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.ui.components.ToyCard

@Composable
fun ToyGridScreen(
    modules: List<GlyphToyModule>,
    appSettings: AppSettings,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onToggleToy: (String) -> Unit,
    onOpenToySettings: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(modules, key = { it.id }) { module ->
            ToyCard(
                module = module,
                settings = settingsRepository.getToySettings(context, module.id, module.settingsSchema),
                active = appSettings.activeToyId == module.id,
                selected = appSettings.selectedToyId == module.id,
                onClick = { onToggleToy(module.id) },
                onLongClick = { onOpenToySettings(module.id) },
                onSettingsClick = { onOpenToySettings(module.id) }
            )
        }
    }
}
