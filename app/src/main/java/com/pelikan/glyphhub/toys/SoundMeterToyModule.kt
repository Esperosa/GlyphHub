package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.audio.SoundLevelMeter
import com.pelikan.glyphhub.audio.SoundLevelReading
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.roundToInt

class SoundMeterToyModule : BaseGlyphToyModule() {
    override val id = "sound_meter"
    override val name = "Sound Level Meter"
    override val shortName = "DB"
    override val description = "Approximate microphone sound level meter with peak hold."
    override val iconAsset = "sound_meter"
    override val supportsAod = false
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Meter display mode.",
                ToySettingType.Choice,
                "ring",
                options = listOf(
                    ToySettingOption("ring", "Ring"),
                    ToySettingOption("bars", "Bars"),
                    ToySettingOption("digits", "Digits")
                ),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("peakHold", "Peak hold", "Show and decay peak level.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("averaging", "Averaging", "Visual averaging window.", ToySettingType.Int, "4", 1, 12),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var meter: SoundLevelMeter? = null
    private var started = false

    override fun onActivate(context: ToyRuntimeContext) {
        meter?.stop()
        meter = SoundLevelMeter(context.androidContext)
        started = meter?.start() == true
    }

    override fun onDeactivate() {
        meter?.stop()
        meter = null
        started = false
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event == ToySensorEvent.BackTap) meter?.resetPeak()
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val activeMeter = meter ?: return drawCenteredText3x5("MIC", brightness, yOffset = 4)
        if (!activeMeter.hasPermission()) return drawCenteredText3x5("MIC", brightness, yOffset = 4)
        if (!started) started = activeMeter.start()
        val reading = activeMeter.latest as? SoundLevelReading.Signal
            ?: return drawCenteredText3x5("WAIT", brightness, yOffset = 4)
        val level = ((reading.approximateDb - SoundLevelMeter.MIN_DB) / (SoundLevelMeter.MAX_DB - SoundLevelMeter.MIN_DB)).coerceIn(0f, 1f)
        return when (getSettings().choice("displayMode", "ring")) {
            "digits" -> drawCenteredText3x5(reading.approximateDb.roundToInt().coerceIn(0, 999).toString(), brightness, yOffset = 4)
            "bars" -> bars(level, brightness)
            else -> ring(level, reading.peakApproximateDb, brightness)
        }
    }

    private fun ring(level: Float, peakDb: Float, brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 30, thickness = 0.5)
        frame = GlyphDesignSystem.drawProgressRing(frame, level, 100)
        val peak = ((peakDb - SoundLevelMeter.MIN_DB) / (SoundLevelMeter.MAX_DB - SoundLevelMeter.MIN_DB)).coerceIn(0f, 1f)
        frame = GlyphDesignSystem.drawArcApprox(frame, GlyphDesignSystem.fullRingRadius, -90.0 + 360.0 * peak, 10.0, 72, thickness = 0.8)
        return frame.overlay(drawCenteredText3x5("dB", brightness, yOffset = 4))
    }

    private fun bars(level: Float, brightness: Int): GlyphFrame =
        frameFromPoints(buildSet {
            val columns = (level * 11f).roundToInt().coerceIn(0, 11)
            for (x in 1..columns) {
                val height = ((x / 11f) * 8f).roundToInt().coerceAtLeast(1)
                for (y in 0 until height) add(x to (10 - y))
            }
        }, brightness)
}
