package com.pelikan.glyphhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Colors = darkColorScheme(
    primary = GlyphRed,
    onPrimary = GlyphWhite,
    background = GlyphBlack,
    onBackground = GlyphWhite,
    surface = GlyphPanel,
    onSurface = GlyphWhite,
    surfaceVariant = GlyphPanelSoft,
    outline = GlyphLine
)

@Composable
fun GlyphHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = GlyphTypography,
        content = content
    )
}
