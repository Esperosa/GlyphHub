package com.pelikan.glyphhub.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.ui.theme.GlyphLine
import com.pelikan.glyphhub.ui.theme.GlyphPanel
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite

@Composable
fun NothingButton(
    text: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) GlyphRed else GlyphPanel,
        label = "glyphhub_button_container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) GlyphRed else GlyphLine,
        label = "glyphhub_button_border"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 44.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = GlyphWhite,
            disabledContainerColor = GlyphPanel,
            disabledContentColor = Color.Gray
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Text(text.uppercase(), maxLines = 1)
    }
}
