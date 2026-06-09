package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.sqrt
import kotlin.random.Random

class RockPaperScissorsToyModule : BaseGlyphToyModule() {
    override val id = "rps"
    override val name = "Rock Paper Scissors Toy"
    override val shortName = "RPS"
    override val description = "Throw rock, paper, or scissors with shake or tap."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("shakeToThrow", "Shake", "Throw on shake.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("tapToThrow", "Tap", "Throw on back tap.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("countdownMs", "Countdown ms", "Countdown duration before reveal.", ToySettingType.Int, "1200", 600, 3000),
            ToySettingDefinition("stableMs", "Stable ms", "Required stillness after shake before countdown.", ToySettingType.Int, "320", 120, 1200),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var result = Throw.ROCK
    private var phase = Phase.IDLE
    private var phaseElapsedMs = 0L
    private var stableElapsedMs = 0L
    private var lastSensorAtMs = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        startRound(waitForStability = false)
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Shake -> if (getSettings().bool("shakeToThrow", true)) startRound(waitForStability = true)
            is ToySensorEvent.Raw -> updateStability(event)
            ToySensorEvent.BackTap -> if (getSettings().bool("tapToThrow", true)) startRound(waitForStability = false)
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        phaseElapsedMs += deltaMs
        return when (phase) {
            Phase.IDLE -> drawCenteredText3x5("GO", brightness)
            Phase.STABILIZING -> drawCenteredText3x5("STB", brightness)
            Phase.COUNTDOWN -> {
                val duration = getSettings().int("countdownMs", 1200).toLong().coerceIn(600L, 3000L)
                if (phaseElapsedMs >= duration) {
                    phase = Phase.REVEAL
                    phaseElapsedMs = 0L
                    result.frame(brightness)
                } else {
                    val remaining = 3 - ((phaseElapsedMs * 3L) / duration).toInt().coerceIn(0, 2)
                    drawCenteredText3x5(remaining.toString(), brightness)
                }
            }
            Phase.REVEAL -> result.frame(brightness)
        }
    }

    private fun startRound(waitForStability: Boolean) {
        result = Throw.entries.random(Random)
        phase = if (waitForStability) Phase.STABILIZING else Phase.COUNTDOWN
        phaseElapsedMs = 0L
        stableElapsedMs = 0L
        lastSensorAtMs = 0L
    }

    private fun updateStability(event: ToySensorEvent.Raw) {
        if (phase != Phase.STABILIZING || event.sensorType != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val dt = if (lastSensorAtMs == 0L) 0L else (event.timestamp - lastSensorAtMs).coerceIn(0L, 80L)
        lastSensorAtMs = event.timestamp
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        if (kotlin.math.abs(magnitude - 9.81f) < 1.15f) {
            stableElapsedMs += dt
        } else {
            stableElapsedMs = 0L
        }
        if (stableElapsedMs >= getSettings().int("stableMs", 320).toLong().coerceIn(120L, 1200L)) {
            phase = Phase.COUNTDOWN
            phaseElapsedMs = 0L
        }
    }

    private enum class Phase {
        IDLE,
        STABILIZING,
        COUNTDOWN,
        REVEAL
    }

    private enum class Throw {
        ROCK,
        PAPER,
        SCISSORS;

        fun frame(brightness: Int): GlyphFrame = when (this) {
            ROCK -> frameFromRows(
                listOf(
                    "0000000000000",
                    "0000011110000",
                    "0001111111100",
                    "0011111111110",
                    "0011111111110",
                    "0111110111110",
                    "0111111111110",
                    "0011111111110",
                    "0011111111100",
                    "0001111111000",
                    "0000111110000",
                    "0000000000000",
                    "0000000000000"
                ),
                brightness
            )
            PAPER -> frameFromRows(
                listOf(
                    "0000011111000",
                    "0000110001100",
                    "0000111001100",
                    "0000110101100",
                    "0000110001100",
                    "0000110001100",
                    "0000110001100",
                    "0000110001100",
                    "0000110001100",
                    "0000110001100",
                    "0000011111000",
                    "0000000000000",
                    "0000000000000"
                ),
                brightness
            )
            SCISSORS -> frameFromRows(
                listOf(
                    "0000000000000",
                    "0010000000100",
                    "0001000001000",
                    "0000100010000",
                    "0000011100000",
                    "0000011100000",
                    "0000100010000",
                    "0001000001000",
                    "0010000000100",
                    "0100000000010",
                    "0010000000100",
                    "0000000000000",
                    "0000000000000"
                ),
                brightness
            )
        }
    }
}
