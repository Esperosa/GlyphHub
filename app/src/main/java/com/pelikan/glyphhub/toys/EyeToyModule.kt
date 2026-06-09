package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

class EyeToyModule : BaseGlyphToyModule() {
    override val id = "eye"
    override val name = "Eye Toy"
    override val shortName = "EYE"
    override val description = "Animated eye with weighted, lifelike behavior states."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "style",
                "Style",
                "Eye visual style.",
                ToySettingType.Choice,
                "normal",
                options = listOf(
                    ToySettingOption("normal", "Normal"),
                    ToySettingOption("cat", "Cat"),
                    ToySettingOption("robot", "Robot"),
                    ToySettingOption("sleepy", "Sleepy"),
                    ToySettingOption("alert", "Alert")
                )
            ),
            ToySettingDefinition(
                "behavior",
                "Behavior",
                "Primary mood / behavior profile.",
                ToySettingType.Choice,
                "curious",
                options = listOf(
                    ToySettingOption("calm", "Calm"),
                    ToySettingOption("curious", "Curious"),
                    ToySettingOption("restless", "Restless"),
                    ToySettingOption("sleepy", "Sleepy"),
                    ToySettingOption("alert", "Alert")
                )
            ),
            ToySettingDefinition("blinkRateMs", "Blink ms", "Average blink cadence.", ToySettingType.Int, "2200", 900, 8000),
            ToySettingDefinition("movementSpeed", "Move speed", "How quickly the pupil tracks targets.", ToySettingType.Int, "55", 15, 100),
            ToySettingDefinition("randomness", "Randomness", "Behavior variation intensity.", ToySettingType.Int, "60", 0, 100),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var rng = Random(42)
    private var state = EyeState.Idle
    private var elapsedMs = 0L
    private var stateElapsedMs = 0L
    private var nextStateChangeMs = 1200L
    private var pupilX = 6.0
    private var pupilY = 6.0
    private var targetX = 6.0
    private var targetY = 6.0
    private var blinkRemainingMs = 0L
    private var doubleBlinkQueued = false
    private var surprisePulseMs = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        rng = Random(System.currentTimeMillis())
        state = EyeState.Idle
        elapsedMs = 0L
        stateElapsedMs = 0L
        nextStateChangeMs = chooseStateDuration(state)
        pupilX = 6.0
        pupilY = 6.0
        targetX = 6.0
        targetY = 6.0
        blinkRemainingMs = 0L
        doubleBlinkQueued = false
        surprisePulseMs = 0L
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event == ToySensorEvent.BackTap) {
            blinkRemainingMs = 180L
            doubleBlinkQueued = true
            state = EyeState.Alert
            stateElapsedMs = 0L
            nextStateChangeMs = 950L
            surprisePulseMs = 400L
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        stateElapsedMs += deltaMs
        blinkRemainingMs = (blinkRemainingMs - deltaMs).coerceAtLeast(0L)
        surprisePulseMs = (surprisePulseMs - deltaMs).coerceAtLeast(0L)
        maybeTriggerBlink()
        updateState()
        updateTargets(deltaMs)
        val brightness = getSettings().int("brightness", 80)
        return renderEye(brightness)
    }

    private fun maybeTriggerBlink() {
        if (blinkRemainingMs > 0L) {
            if (blinkRemainingMs == 0L && doubleBlinkQueued) {
                blinkRemainingMs = 110L
                doubleBlinkQueued = false
            }
            return
        }
        val blinkRateMs = getSettings().int("blinkRateMs", 2200).coerceAtLeast(900)
        val randomness = getSettings().int("randomness", 60).coerceIn(0, 100)
        val threshold = blinkRateMs - (randomness * 6L)
        if (stateElapsedMs >= threshold && rng.nextInt(100) < 12 + randomness / 6) {
            blinkRemainingMs = if (state == EyeState.Sleepy) 240L else 150L
            doubleBlinkQueued = rng.nextInt(100) < randomness / 7
        }
    }

    private fun updateState() {
        if (stateElapsedMs < nextStateChangeMs && blinkRemainingMs == 0L) return
        state = chooseNextState()
        stateElapsedMs = 0L
        nextStateChangeMs = chooseStateDuration(state)
        if (state == EyeState.Surprise) {
            surprisePulseMs = 420L
        }
    }

    private fun updateTargets(deltaMs: Long) {
        val behavior = getSettings().choice("behavior", "curious")
        val movementSpeed = getSettings().int("movementSpeed", 55).coerceIn(15, 100) / 100.0
        val randomness = getSettings().int("randomness", 60).coerceIn(0, 100)
        val (desiredX, desiredY) = when (state) {
            EyeState.Idle -> 6.0 to 6.0
            EyeState.Curious -> 7.4 to 5.1
            EyeState.Sleepy -> 5.3 to 6.7
            EyeState.Alert -> 6.0 to 4.8
            EyeState.Scanning -> scanTarget()
            EyeState.MicroSaccade -> randomTarget(radius = 1.4)
            EyeState.LookAround -> randomTarget(radius = if (behavior == "restless") 3.1 else 2.4)
            EyeState.Stare -> 6.0 to 6.0
            EyeState.Surprise -> 6.0 to 5.2
        }
        targetX = desiredX
        targetY = desiredY
        val smoothing = 0.10 + movementSpeed * 0.32 + randomness / 500.0
        pupilX += (targetX - pupilX) * smoothing * (deltaMs / 33.0).coerceAtLeast(0.45)
        pupilY += (targetY - pupilY) * smoothing * (deltaMs / 33.0).coerceAtLeast(0.45)
        pupilX = pupilX.coerceIn(4.0, 8.0)
        pupilY = pupilY.coerceIn(4.0, 8.0)
    }

    private fun renderEye(brightness: Int): GlyphFrame {
        val style = getSettings().choice("style", "normal")
        val openness = openness()
        val base = when (style) {
            "cat" -> catOutline(brightness, openness)
            "robot" -> robotOutline(brightness, openness)
            "sleepy" -> sleepyOutline(brightness, openness)
            "alert" -> alertOutline(brightness, openness)
            else -> normalOutline(brightness, openness)
        }
        if (openness < 0.25) return GlyphDesignSystem.clean(base)
        val pupilIntensity = if (surprisePulseMs > 0L) brightness else (brightness * 0.9f).roundToInt()
        val pupil = when (style) {
            "cat" -> catPupil(pupilIntensity)
            "robot" -> robotPupil(pupilIntensity)
            else -> normalPupil(pupilIntensity)
        }
        return GlyphDesignSystem.clean(base.overlay(pupil))
    }

    private fun openness(): Double {
        if (blinkRemainingMs <= 0L) return if (state == EyeState.Sleepy) 0.74 else 1.0
        val duration = if (state == EyeState.Sleepy) 240.0 else 180.0
        val ratio = blinkRemainingMs / duration
        return if (ratio > 0.5) {
            (1.0 - (ratio - 0.5) * 2.0).coerceIn(0.12, 1.0)
        } else {
            (ratio * 2.0).coerceIn(0.12, 1.0)
        }
    }

    private fun normalOutline(brightness: Int, openness: Double): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawArcApprox(frame, 4.9, -118.0, 236.0, 90)
        val lowerSweep = 160.0 * openness.coerceAtLeast(0.18)
        frame = GlyphDesignSystem.drawArcApprox(frame, 4.9, 200.0 - lowerSweep / 2.0, lowerSweep, 84)
        return frame
    }

    private fun catOutline(brightness: Int, openness: Double): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawArcApprox(frame, 4.8, -140.0, 280.0, 88)
        frame = GlyphDesignSystem.drawArcApprox(frame, 4.4, 218.0, 104.0 * openness.coerceAtLeast(0.2), 84)
        frame = frame.withPixelBrightness(3, 5, 88).withPixelBrightness(9, 5, 88)
        return frame
    }

    private fun robotOutline(brightness: Int, openness: Double): GlyphFrame {
        return frameFromRows(
            listOf(
                "0000000000000",
                "0001111111000",
                "0011000001100",
                "0110000000110",
                "1100000000011",
                if (openness > 0.6) "1100000000011" else "1111111111111",
                if (openness > 0.6) "1100000000011" else "1111111111111",
                "1100000000011",
                "0110000000110",
                "0011000001100",
                "0001111111000",
                "0000000000000",
                "0000000000000"
            ),
            brightness
        )
    }

    private fun sleepyOutline(brightness: Int, openness: Double): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawArcApprox(frame, 4.8, -108.0, 216.0, 82)
        frame = GlyphDesignSystem.drawArcApprox(frame, 3.4, 210.0, 100.0 * openness.coerceAtLeast(0.18), 74)
        return frame
    }

    private fun alertOutline(brightness: Int, openness: Double): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawArcApprox(frame, 5.1, -126.0, 252.0, 96)
        frame = GlyphDesignSystem.drawArcApprox(frame, 5.0, 206.0, 150.0 * openness.coerceAtLeast(0.2), 90)
        return frame
    }

    private fun normalPupil(brightness: Int): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        frame = GlyphDesignSystem.drawSoftDot(frame, pupilX.roundToInt(), pupilY.roundToInt(), 100)
        return frame.scaleIntensity(brightness / 100f)
    }

    private fun catPupil(brightness: Int): GlyphFrame {
        val x = pupilX.roundToInt().coerceIn(4, 8)
        val y = pupilY.roundToInt().coerceIn(4, 8)
        return frameFromPoints(
            setOf(x to y, x to (y - 1), x to (y + 1), x to (y + 2), x to (y - 2)),
            brightness
        )
    }

    private fun robotPupil(brightness: Int): GlyphFrame {
        val x = pupilX.roundToInt().coerceIn(4, 8)
        val y = pupilY.roundToInt().coerceIn(4, 8)
        return frameFromPoints(
            setOf(
                x to y,
                (x - 1) to y,
                (x + 1) to y,
                x to (y - 1),
                x to (y + 1)
            ),
            brightness
        )
    }

    private fun chooseNextState(): EyeState {
        val behavior = getSettings().choice("behavior", "curious")
        val weighted = when (behavior) {
            "calm" -> listOf(EyeState.Idle, EyeState.Stare, EyeState.Curious, EyeState.MicroSaccade)
            "sleepy" -> listOf(EyeState.Sleepy, EyeState.Idle, EyeState.Stare, EyeState.Curious)
            "restless" -> listOf(EyeState.LookAround, EyeState.Scanning, EyeState.MicroSaccade, EyeState.Curious)
            "alert" -> listOf(EyeState.Alert, EyeState.Scanning, EyeState.Stare, EyeState.Surprise)
            else -> listOf(EyeState.Curious, EyeState.LookAround, EyeState.Scanning, EyeState.Stare)
        }
        return weighted[rng.nextInt(weighted.size)]
    }

    private fun chooseStateDuration(state: EyeState): Long =
        when (state) {
            EyeState.Idle -> 900L + rng.nextLong(1400L)
            EyeState.Curious -> 650L + rng.nextLong(950L)
            EyeState.Sleepy -> 1400L + rng.nextLong(1700L)
            EyeState.Alert -> 800L + rng.nextLong(900L)
            EyeState.Scanning -> 700L + rng.nextLong(1200L)
            EyeState.MicroSaccade -> 180L + rng.nextLong(180L)
            EyeState.LookAround -> 450L + rng.nextLong(850L)
            EyeState.Stare -> 1200L + rng.nextLong(1600L)
            EyeState.Surprise -> 380L + rng.nextLong(240L)
        }

    private fun scanTarget(): Pair<Double, Double> {
        val phase = (elapsedMs / 250L).toInt() % 6
        val path = listOf(4.1 to 6.0, 5.0 to 5.2, 6.0 to 5.0, 7.2 to 5.4, 8.0 to 6.0, 6.0 to 6.4)
        return path[phase]
    }

    private fun randomTarget(radius: Double): Pair<Double, Double> {
        val x = 6.0 + rng.nextDouble(-radius, radius)
        val y = 6.0 + rng.nextDouble(-radius, radius)
        return x.coerceIn(4.0, 8.0) to y.coerceIn(4.0, 8.0)
    }

    private enum class EyeState {
        Idle,
        Curious,
        Sleepy,
        Alert,
        Scanning,
        MicroSaccade,
        LookAround,
        Stare,
        Surprise
    }
}