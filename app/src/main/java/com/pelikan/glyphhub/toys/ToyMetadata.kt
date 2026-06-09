package com.pelikan.glyphhub.toys

enum class ToyCategory {
    Core,
    Sensors,
    Time,
    System,
    Audio,
    Fun,
    Dev,
    Experimental
}

enum class ToyVisibilityState {
    READY,
    NEEDS_SETUP,
    EXPERIMENTAL,
    HIDDEN,
    MISSING
}

data class RegisteredToy(
    val module: GlyphToyModule,
    val category: ToyCategory,
    val visibility: ToyVisibilityState = ToyVisibilityState.READY,
    val setupHint: String? = null
) {
    val isUserVisible: Boolean
        get() = visibility == ToyVisibilityState.READY || visibility == ToyVisibilityState.NEEDS_SETUP
}
