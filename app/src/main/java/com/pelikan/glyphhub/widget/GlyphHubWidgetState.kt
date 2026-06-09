package com.pelikan.glyphhub.widget

data class GlyphHubWidgetState(
    val selectedToyId: String,
    val activeToyId: String?,
    val isCurrentToyActive: Boolean,
    val lastActivationTimestamp: Long,
    val lastSelectedToyIndex: Int,
    val widgetCarouselIndex: Int,
    val widgetPanelMode: String,
    val widgetPanelSettingIndex: Int
)
