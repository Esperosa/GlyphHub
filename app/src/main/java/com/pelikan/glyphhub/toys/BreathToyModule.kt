package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class BreathToyModule : BaseGlyphToyModule() {
    override val id = "breath"
    override val name = "Breath / Meditation"
    override val shortName = "BREATHE"
    override val description = "Inhale-hold-exhale breathing guide with optional haptic phase cues."
    override val iconAsset = "breath"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "preset",
                "Preset",
                "Breathing cycle preset.",
                ToySettingType.Choice,
                "box",
                options = listOf(ToySettingOption("box", "Box 4-4-4"), ToySettingOption("calm", "Calm 4-2-6")),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("hapticPhases", "Haptic", "Pulse at phase changes.", ToySettingType.Boolean, "false", scope = ToySettingScope.QUICK),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "60", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var elapsedMs = 0L
    private var lastPhase = ""
    private var haptics: HapticFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        elapsedMs = 0L
        lastPhase = ""
        haptics = HapticFeedbackController(context.androidContext)
    }

    override fun onDeactivate() {
        haptics = null
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        val brightness = getSettings().int("brightness", 60)
        val phases = if (getSettings().choice("preset", "box") == "calm") {
            listOf(Phase("IN", 4_000L, 0f, 1f), Phase("H", 2_000L, 1f, 1f), Phase("OUT", 6_000L, 1f, 0f))
        } else {
            listOf(Phase("IN", 4_000L, 0f, 1f), Phase("H", 4_000L, 1f, 1f), Phase("OUT", 4_000L, 1f, 0f))
        }
        val cycle = phases.sumOf { it.durationMs }
        var within = elapsedMs % cycle
        val phase = phases.first {
            if (within < it.durationMs) true else {
                within -= it.durationMs
                false
            }
        }
        if (phase.label != lastPhase) {
            lastPhase = phase.label
            if (getSettings().bool("hapticPhases", false)) haptics?.pulseSuccess()
        }
        val ratio = (within / phase.durationMs.toFloat()).coerceIn(0f, 1f)
        val size = phase.from + (phase.to - phase.from) * ratio
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), 1.2 + size * 4.5, 90, thickness = 0.65)
        frame = frame.overlay(drawCenteredText3x5(phase.label, brightness, yOffset = 4))
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private data class Phase(val label: String, val durationMs: Long, val from: Float, val to: Float)
}
