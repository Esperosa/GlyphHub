package com.pelikan.glyphhub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.settings.ToySettings
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.toys.ToyVisibilityState
import com.pelikan.glyphhub.ui.theme.GlyphLine
import com.pelikan.glyphhub.ui.theme.GlyphMuted
import com.pelikan.glyphhub.ui.theme.GlyphPanel
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToyCard(
    module: GlyphToyModule,
    settings: ToySettings,
    active: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    module.updateSettings(settings)
    val frame = module.previewFrame()
    val entry = ToyRegistry.entry(module.id)
    val state = entry?.visibility ?: ToyVisibilityState.READY
    val statusLabel = when {
        active -> "ACTIVE"
        state == ToyVisibilityState.NEEDS_SETUP -> "SETUP"
        state == ToyVisibilityState.EXPERIMENTAL -> "EXPER"
        selected -> "SEL"
        else -> "READY"
    }
    val statusColor = when {
        active -> GlyphRed
        state == ToyVisibilityState.NEEDS_SETUP || state == ToyVisibilityState.EXPERIMENTAL -> GlyphWhite.copy(alpha = 0.86f)
        else -> GlyphMuted
    }
    val shape = RoundedCornerShape(8.dp)
    val background by animateColorAsState(
        targetValue = if (active) GlyphRed.copy(alpha = 0.18f) else GlyphPanel,
        label = "toy_card_background"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            active -> GlyphRed
            selected -> GlyphWhite.copy(alpha = 0.72f)
            else -> GlyphLine
        },
        label = "toy_card_border"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MatrixPreview(frame = frame, modifier = Modifier.size(70.dp), active = active)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(module.shortName, style = MaterialTheme.typography.titleMedium, color = GlyphWhite)
                Text(statusLabel, color = statusColor)
            }
            NothingButton(text = "SET", onClick = onSettingsClick, active = selected)
        }
        if (entry?.setupHint != null && (state == ToyVisibilityState.NEEDS_SETUP || state == ToyVisibilityState.EXPERIMENTAL)) {
            Text(entry.setupHint, style = MaterialTheme.typography.bodySmall, color = GlyphMuted.copy(alpha = 0.82f))
        }
        Text(module.description, style = MaterialTheme.typography.bodySmall, color = GlyphMuted.copy(alpha = 0.72f))
    }
}
