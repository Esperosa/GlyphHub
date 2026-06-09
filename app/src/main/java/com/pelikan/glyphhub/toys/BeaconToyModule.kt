package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.AudioFeedbackController
import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class BeaconToyModule : BaseGlyphToyModule() {
    override val id = "beacon"
    override val name = "Find Phone / Beacon"
    override val shortName = "FIND"
    override val description = "Strong user-controlled beacon with timeout, optional haptic and sound."
    override val iconAsset = "beacon"
    override val supportsAod = false
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("timeoutSeconds", "Timeout", "Auto-stop timeout.", ToySettingType.Int, "30", 5, 300, scope = ToySettingScope.QUICK),
            ToySettingDefinition("haptics", "Haptic", "Pulse while beacon is active.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("sound", "Sound", "Play periodic beep.", ToySettingType.Boolean, "false", scope = ToySettingScope.QUICK),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "100", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var elapsedMs = 0L
    private var lastPulseMs = 0L
    private var haptics: HapticFeedbackController? = null
    private var audio: AudioFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        elapsedMs = 0L
        lastPulseMs = 0L
        haptics = HapticFeedbackController(context.androidContext)
        audio = AudioFeedbackController()
    }

    override fun onDeactivate() {
        audio?.release()
        audio = null
        haptics = null
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        lastPulseMs += deltaMs
        val brightness = getSettings().int("brightness", 100)
        val timeoutMs = getSettings().int("timeoutSeconds", 30).coerceIn(5, 300) * 1000L
        if (elapsedMs >= timeoutMs) {
            return drawCenteredText3x5("END", brightness)
        }
        if (lastPulseMs >= 900L) {
            lastPulseMs = 0L
            if (getSettings().bool("haptics", true)) haptics?.pulse(80L)
            if (getSettings().bool("sound", false)) audio?.playTickTone(accent = true)
        }
        val phase = ((elapsedMs / 240L) % 3).toInt()
        var frame = GlyphDesignSystem.drawRing(GlyphFrame.empty13(brightness), 0.0, 1.4 + phase * 1.8, 100)
        frame = GlyphDesignSystem.drawCircleApprox(frame, GlyphDesignSystem.fullRingRadius, 62, thickness = 0.55)
        return frame.overlay(drawCenteredText3x5("SOS", brightness, yOffset = 4))
    }
}
