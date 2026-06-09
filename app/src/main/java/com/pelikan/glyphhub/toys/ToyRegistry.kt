package com.pelikan.glyphhub.toys

object ToyRegistry {
    val entries: List<RegisteredToy> = listOf(
        RegisteredToy(BatteryToyModule(), ToyCategory.Core),
        RegisteredToy(ClockToyModule(), ToyCategory.Time),
        RegisteredToy(DiceToyModule(), ToyCategory.Fun),
        RegisteredToy(CoinToyModule(), ToyCategory.Fun),
        RegisteredToy(TimerToyModule(), ToyCategory.Time),
        RegisteredToy(PomodoroToyModule(), ToyCategory.Time),
        RegisteredToy(LevelToyModule(), ToyCategory.Sensors),
        RegisteredToy(LuxMeterToyModule(), ToyCategory.Sensors),
        RegisteredToy(CompassToyModule(), ToyCategory.Sensors),
        RegisteredToy(OrientationToyModule(), ToyCategory.Sensors),
        RegisteredToy(MotionToyModule(), ToyCategory.Sensors, ToyVisibilityState.NEEDS_SETUP, "Step counting needs Activity Recognition permission; motion fallback still works."),
        RegisteredToy(TunerToyModule(), ToyCategory.Audio, ToyVisibilityState.NEEDS_SETUP, "Microphone permission required for pitch detection."),
        RegisteredToy(SoundMeterToyModule(), ToyCategory.Audio, ToyVisibilityState.NEEDS_SETUP, "Microphone permission required; dB is approximate unless calibrated."),
        RegisteredToy(MetronomeToyModule(), ToyCategory.Audio),
        RegisteredToy(EyeToyModule(), ToyCategory.Fun),
        RegisteredToy(BreathToyModule(), ToyCategory.Fun),
        RegisteredToy(SunMoonToyModule(), ToyCategory.Fun),
        RegisteredToy(RockPaperScissorsToyModule(), ToyCategory.Fun, ToyVisibilityState.EXPERIMENTAL, "Needs manual Matrix validation for countdown readability."),
        RegisteredToy(MazeToyModule(), ToyCategory.Sensors, ToyVisibilityState.EXPERIMENTAL, "Tilt maze is useful but still needs physical Matrix validation."),
        RegisteredToy(SchoolClassTimerToyModule(), ToyCategory.Time),
        RegisteredToy(WeatherToyModule(), ToyCategory.System, ToyVisibilityState.NEEDS_SETUP, "Needs a city and network access for Open-Meteo refresh."),
        RegisteredToy(NetworkStatusToyModule(), ToyCategory.System),
        RegisteredToy(PaymentToyModule(), ToyCategory.System, ToyVisibilityState.NEEDS_SETUP, "Visual trigger only; real payment success cannot be detected."),
        RegisteredToy(BeaconToyModule(), ToyCategory.System),
        RegisteredToy(PixelArtToyModule(), ToyCategory.Fun),
        RegisteredToy(TextScrollToyModule(), ToyCategory.Core),
        RegisteredToy(IdleDefaultToyModule(), ToyCategory.Core, ToyVisibilityState.HIDDEN)
    )

    val modules: List<GlyphToyModule>
        get() = entries.map { it.module }

    fun visibleEntries(showExperimental: Boolean): List<RegisteredToy> =
        entries.filter { entry ->
            when (entry.visibility) {
                ToyVisibilityState.READY,
                ToyVisibilityState.NEEDS_SETUP -> true
                ToyVisibilityState.EXPERIMENTAL -> showExperimental
                ToyVisibilityState.HIDDEN,
                ToyVisibilityState.MISSING -> false
            }
        }

    fun visibleModules(showExperimental: Boolean): List<GlyphToyModule> =
        visibleEntries(showExperimental).map { it.module }

    fun carouselModules(showExperimental: Boolean): List<GlyphToyModule> =
        visibleModules(showExperimental)

    fun entry(id: String?): RegisteredToy? = entries.firstOrNull { it.module.id == id }

    fun byId(id: String?): GlyphToyModule? = entry(id)?.module

    fun indexOf(id: String): Int = modules.indexOfFirst { it.id == id }

    fun defaultToy(): GlyphToyModule = byId("idle_default")!!
}
