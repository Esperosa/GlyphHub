package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.AudioFeedbackController
import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class PomodoroToyModule : BaseGlyphToyModule() {
    override val id = "pomodoro"
    override val name = "Pomodoro Focus"
    override val shortName = "FOCUS"
    override val description = "Focus/break cycle timer with haptic and sound transition cues."
    override val iconAsset = "pomodoro"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("focusMinutes", "Focus min", "Focus duration.", ToySettingType.Int, "25", 1, 120, scope = ToySettingScope.QUICK),
            ToySettingDefinition("breakMinutes", "Break min", "Short break duration.", ToySettingType.Int, "5", 1, 60, scope = ToySettingScope.QUICK),
            ToySettingDefinition("longBreakMinutes", "Long break", "Long break duration.", ToySettingType.Int, "15", 5, 90),
            ToySettingDefinition("cycles", "Cycles", "Cycles before long break.", ToySettingType.Int, "4", 1, 12),
            ToySettingDefinition("hapticTransitions", "Haptic", "Vibrate on transitions.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("soundTransitions", "Sound", "Play tone on transitions.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var phase = Phase.Focus
    private var cycle = 1
    private var remainingMs = 0L
    private var totalMs = 0L
    private var running = true
    private var haptics: HapticFeedbackController? = null
    private var audio: AudioFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        haptics = HapticFeedbackController(context.androidContext)
        audio = AudioFeedbackController()
        cycle = 1
        startPhase(Phase.Focus, running = true, notify = false)
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
        if (running) {
            remainingMs = (remainingMs - deltaMs).coerceAtLeast(0L)
            if (remainingMs == 0L) advance()
        }
        return render()
    }

    private fun advance() {
        when (phase) {
            Phase.Focus -> startPhase(if (cycle >= getSettings().int("cycles", 4).coerceIn(1, 12)) Phase.LongBreak else Phase.Break)
            Phase.Break -> {
                cycle += 1
                startPhase(Phase.Focus)
            }
            Phase.LongBreak -> {
                cycle = 1
                startPhase(Phase.Focus)
            }
        }
    }

    private fun startPhase(next: Phase, running: Boolean = this.running, notify: Boolean = true) {
        phase = next
        totalMs = when (next) {
            Phase.Focus -> getSettings().int("focusMinutes", 25).coerceIn(1, 120) * 60_000L
            Phase.Break -> getSettings().int("breakMinutes", 5).coerceIn(1, 60) * 60_000L
            Phase.LongBreak -> getSettings().int("longBreakMinutes", 15).coerceIn(5, 90) * 60_000L
        }
        remainingMs = totalMs
        this.running = running
        if (notify && getSettings().bool("hapticTransitions", true)) haptics?.pulseCompletion()
        if (notify && getSettings().bool("soundTransitions", false)) audio?.playCompletionTone()
    }

    private fun render(): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val progress = if (totalMs <= 0L) 0f else 1f - remainingMs / totalMs.toFloat()
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 28, thickness = 0.5)
        frame = GlyphDesignSystem.drawProgressRing(frame, progress, if (phase == Phase.Focus) 100 else 72)
        val label = when (phase) {
            Phase.Focus -> "FOC"
            Phase.Break -> "BRK"
            Phase.LongBreak -> "LBR"
        }
        frame = frame.overlay(drawCenteredText3x5(label, brightness, yOffset = 4))
        if (!running) frame = frame.scaleBrightness(0.65f)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private enum class Phase { Focus, Break, LongBreak }
}
