package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.AudioFeedbackController
import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class MetronomeToyModule : BaseGlyphToyModule() {
    override val id = "metronome"
    override val name = "Metronome"
    override val shortName = "BPM"
    override val description = "Drift-compensated visual beat with optional haptic and sound ticks."
    override val iconAsset = "metronome"
    override val supportsAod = false
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("bpm", "BPM", "Tempo in beats per minute.", ToySettingType.Int, "120", 30, 240, scope = ToySettingScope.QUICK),
            ToySettingDefinition("beatsPerBar", "Beats/bar", "Accent every N beats.", ToySettingType.Int, "4", 1, 12, scope = ToySettingScope.QUICK),
            ToySettingDefinition("hapticTick", "Haptic", "Vibrate on beat.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("soundTick", "Sound", "Play a short tick tone.", ToySettingType.Boolean, "false", scope = ToySettingScope.QUICK),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var running = true
    private var nextBeatAtMs = 0L
    private var lastBeatAtMs = 0L
    private var beatIndex = 0
    private var haptics: HapticFeedbackController? = null
    private var audio: AudioFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        running = true
        val now = android.os.SystemClock.elapsedRealtime()
        nextBeatAtMs = now
        lastBeatAtMs = 0L
        beatIndex = 0
        haptics = HapticFeedbackController(context.androidContext)
        audio = AudioFeedbackController()
    }

    override fun onDeactivate() {
        audio?.release()
        audio = null
        haptics = null
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event == ToySensorEvent.BackTap) running = !running
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val now = android.os.SystemClock.elapsedRealtime()
        val interval = (60_000L / getSettings().int("bpm", 120).coerceIn(30, 240))
        if (running && now >= nextBeatAtMs) {
            val beatsPerBar = getSettings().int("beatsPerBar", 4).coerceIn(1, 12)
            val accent = beatIndex % beatsPerBar == 0
            lastBeatAtMs = now
            beatIndex += 1
            while (nextBeatAtMs <= now) nextBeatAtMs += interval
            if (getSettings().bool("hapticTick", true)) haptics?.pulse(if (accent) 38L else 18L)
            if (getSettings().bool("soundTick", false)) audio?.playTickTone(accent)
        }
        return render(now, interval)
    }

    private fun render(now: Long, interval: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val sinceBeat = if (lastBeatAtMs == 0L) interval else now - lastBeatAtMs
        val beatPulse = (1f - (sinceBeat / 220f)).coerceIn(0f, 1f)
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 32, thickness = 0.5)
        frame = GlyphDesignSystem.drawProgressRing(frame, ((interval - (nextBeatAtMs - now)) / interval.toFloat()).coerceIn(0f, 1f), 60)
        frame = GlyphDesignSystem.drawRing(frame, 0.0, 1.4 + beatPulse * 2.4, if (beatPulse > 0f) 100 else 45)
        return frame.overlay(drawCenteredText3x5(getSettings().int("bpm", 120).toString().take(3), brightness, yOffset = 4))
    }
}
