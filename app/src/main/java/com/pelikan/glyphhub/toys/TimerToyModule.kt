package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.AudioFeedbackController
import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.roundToInt

class TimerToyModule : BaseGlyphToyModule() {
    override val id = "timer"
    override val name = "Timer Toy"
    override val shortName = "TMR"
    override val description = "Simple countdown timer with tap pause and optional shake reset."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("durationSeconds", "Duration s", "Countdown duration in seconds.", ToySettingType.Int, "60", 10, 599),
            ToySettingDefinition("autoStart", "Auto start", "Start counting down when activated.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("repeat", "Repeat", "Restart automatically after reaching zero.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("endVibration", "End vibration", "Vibrate when the countdown reaches zero.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("endSound", "End sound", "Play a short local completion tone.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("shakeToReset", "Shake reset", "Reset the timer when shaken.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var totalDurationMs = 0L
    private var remainingMs = 0L
    private var running = false
    private var finished = false
    private var flashElapsedMs = 0L
    private var completionNotified = false
    private var haptic: HapticFeedbackController? = null
    private var audio: AudioFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        haptic = HapticFeedbackController(context.androidContext)
        audio = AudioFeedbackController()
        reset(start = getSettings().bool("autoStart", true))
    }

    override fun onDeactivate() {
        audio?.release()
        audio = null
        haptic = null
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Shake -> if (getSettings().bool("shakeToReset", false)) {
                reset(start = getSettings().bool("autoStart", true))
            }
            ToySensorEvent.BackTap -> {
                if (finished) {
                    reset(start = true)
                } else {
                    running = !running
                }
            }
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        ensureInitialized()
        flashElapsedMs += deltaMs
        if (running && !finished) {
            remainingMs = (remainingMs - deltaMs).coerceAtLeast(0L)
            if (remainingMs == 0L) {
                if (getSettings().bool("repeat", false)) {
                    notifyCompletion()
                    reset(start = true)
                } else {
                    finished = true
                    running = false
                    notifyCompletion()
                }
            }
        }
        return renderTimer()
    }

    private fun ensureInitialized() {
        if (totalDurationMs > 0L) return
        reset(start = getSettings().bool("autoStart", true))
    }

    private fun reset(start: Boolean) {
        totalDurationMs = getSettings().int("durationSeconds", 60).coerceIn(10, 599) * 1000L
        remainingMs = totalDurationMs
        running = start
        finished = false
        flashElapsedMs = 0L
        completionNotified = false
    }

    private fun notifyCompletion() {
        if (completionNotified) return
        completionNotified = true
        if (getSettings().bool("endVibration", true)) haptic?.pulseCompletion()
        if (getSettings().bool("endSound", false)) audio?.playCompletionTone()
    }

    private fun renderTimer(): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val progress = if (totalDurationMs <= 0L) 0f else 1f - (remainingMs.toFloat() / totalDurationMs.toFloat())
        val frame = frameFromPoints(buildSet {
            for (x in 1..11) {
                add(x to 1)
                add(x to 11)
            }
            for (y in 1..11) {
                add(1 to y)
                add(11 to y)
            }
            val filled = (progress * 11f).roundToInt().coerceIn(0, 11)
            for (index in 0 until filled) {
                add((1 + index) to 10)
            }
        }, brightness)
        val remainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
        val label = if (remainingSeconds <= 99) {
            remainingSeconds.toString().padStart(2, '0')
        } else {
            (remainingSeconds / 60).coerceAtMost(99).toString().padStart(2, '0')
        }
        val rendered = frame.overlay(drawCenteredText3x5(label, brightness, yOffset = 4))
        return when {
            finished -> rendered
            !running -> rendered.scaleBrightness(0.72f)
            else -> rendered
        }
    }
}
