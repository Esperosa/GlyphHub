package com.pelikan.glyphhub.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import com.pelikan.glyphhub.ui.theme.GlyphLine
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite

@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = GlyphWhite,
            checkedTrackColor = GlyphRed,
            uncheckedThumbColor = GlyphWhite,
            uncheckedTrackColor = GlyphLine
        )
    )
}
