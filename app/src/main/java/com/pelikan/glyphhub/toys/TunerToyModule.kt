package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.audio.PitchTracker
import com.pelikan.glyphhub.audio.TunerReading
import com.pelikan.glyphhub.audio.TuningMath
import com.pelikan.glyphhub.audio.TuningTarget
import com.pelikan.glyphhub.audio.YinPitchDetector
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.abs
import kotlin.math.ln

class TunerToyModule : BaseGlyphToyModule() {
    override val id = "tuner"
    override val name = "Tuner Toy"
    override val shortName = "TUNER"
    override val description = "Microphone tuner with guitar, ukulele, and chromatic targets."
    override val iconAsset = "tuner_manual"
    override val supportsAod = false
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "instrument",
                "Instrument",
                "Instrument or chromatic target set.",
                ToySettingType.Choice,
                "guitar",
                options = listOf(
                    ToySettingOption("guitar", "Guitar"),
                    ToySettingOption("ukulele", "Ukulele"),
                    ToySettingOption("chromatic", "Chromatic")
                ),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition(
                "tuningPreset",
                "Tuning",
                "Preset tuning for the selected instrument.",
                ToySettingType.Choice,
                "standard",
                options = listOf(
                    ToySettingOption("standard", "Guitar standard / Uke high G"),
                    ToySettingOption("drop_d", "Guitar Drop D"),
                    ToySettingOption("uke_low_g", "Ukulele low G"),
                    ToySettingOption("uke_baritone", "Ukulele baritone")
                ),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("a4Reference", "A4 reference", "A4 reference frequency.", ToySettingType.Int, "440", 400, 480),
            ToySettingDefinition("toleranceCents", "Tolerance", "Lock tolerance in cents.", ToySettingType.Int, "3", 1, 20),
            ToySettingDefinition("hapticLock", "Lock haptic", "Short pulse after stable in-tune detection.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("smoothing", "Smoothing", "Median smoothing window.", ToySettingType.Int, "3", 1, 9, scope = ToySettingScope.ADVANCED),
            ToySettingDefinition("inputThreshold", "Input threshold", "Minimum input level.", ToySettingType.Int, "3", 1, 40, scope = ToySettingScope.ADVANCED),
            ToySettingDefinition("bufferSize", "Buffer size", "Audio analysis buffer size.", ToySettingType.Int, "2048", 1024, 4096, scope = ToySettingScope.ADVANCED),
            ToySettingDefinition(
                "meterStyle",
                "Meter style",
                "Bottom LED meter style.",
                ToySettingType.Choice,
                "dot",
                options = listOf(
                    ToySettingOption("dot", "Dot")
                )
            ),
            ToySettingDefinition(
                "brightness",
                "Visual intensity",
                "Relative frame intensity, not system Glyph brightness.",
                ToySettingType.Int,
                "80",
                0,
                100,
                scope = ToySettingScope.ADVANCED,
                requiresDebugMode = true
            )
        )
    )

    private var tracker: PitchTracker? = null
    private var microphoneStarted = false
    private var haptics: HapticFeedbackController? = null
    private var smoothedCents: Float? = null
    private var lastTargetLabel: String? = null
    private var stableTargetLabel: String? = null
    private var stableLockMs = 0L
    private var lastLockPulseAt = 0L
    private var lastSignalFrame: LastSignalFrame? = null

    override fun onActivate(context: ToyRuntimeContext) {
        haptics = HapticFeedbackController(context.androidContext)
        smoothedCents = null
        lastTargetLabel = null
        stableTargetLabel = null
        stableLockMs = 0L
        lastLockPulseAt = 0L
        lastSignalFrame = null
        tracker?.stop()
        val inputThreshold = getSettings().int("inputThreshold", 3).coerceIn(1, 40) / 1000f
        tracker = PitchTracker(
            context = context.androidContext,
            detector = YinPitchDetector(minRms = inputThreshold),
            smoothingWindow = getSettings().int("smoothing", 3).coerceIn(1, 9),
            minConfidence = 0.66f,
            bufferSize = getSettings().int("bufferSize", 2048).coerceIn(1024, 4096)
        )
        microphoneStarted = tracker?.start() == true
    }

    override fun onDeactivate() {
        tracker?.stop()
        tracker = null
        microphoneStarted = false
        haptics = null
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val settings = getSettings()
        val brightness = settings.int("brightness", 80)
        val pitchTracker = tracker
        if (pitchTracker == null || !pitchTracker.hasPermission()) {
            return TunerToyVisuals.drawWaiting(brightness)
        }
        if (!microphoneStarted) {
            microphoneStarted = pitchTracker.start()
            if (!microphoneStarted) return TunerToyVisuals.drawWaiting(brightness)
        }
        val reading = pitchTracker.reading() as? TunerReading.Signal
            ?: return heldSignalFrame(brightness)
        val targets = TunerToyLogic.targets(
            instrument = settings.choice("instrument", "guitar"),
            preset = settings.choice("tuningPreset", "standard"),
            a4 = settings.int("a4Reference", 440).toFloat()
        )
        val nearest = TuningMath.nearestTarget(reading.frequencyHz, targets)
            ?: return TunerToyVisuals.drawNoSignal(brightness)
        val target = chooseStableTarget(reading.frequencyHz, nearest, targets)
        val label = TunerToyLogic.displayLabel(target)
        val rawOffset = centsBetween(reading.frequencyHz, target.frequencyHz)
            .coerceIn(-TunerToyLogic.MAX_DISPLAY_CENTS.toFloat(), TunerToyLogic.MAX_DISPLAY_CENTS.toFloat())
        val offset = smoothOffset(label, rawOffset, deltaMs)
        maybePulseLock(offset, deltaMs)
        lastSignalFrame = LastSignalFrame(label, offset, System.currentTimeMillis())
        return TunerToyVisuals.drawTuner(
            label = label,
            cents = offset,
            toleranceCents = lockToleranceCents(),
            brightness = brightness
        )
    }

    private fun chooseStableTarget(
        frequencyHz: Float,
        nearest: TuningTarget,
        targets: List<TuningTarget>
    ): TuningTarget {
        val previous = stableTargetLabel?.let { label -> targets.firstOrNull { it.label == label } }
        if (previous == null) {
            stableTargetLabel = nearest.label
            return nearest
        }
        val previousError = abs(centsBetween(frequencyHz, previous.frequencyHz))
        val nearestError = abs(centsBetween(frequencyHz, nearest.frequencyHz))
        val shouldSwitch = nearest.label != previous.label &&
            (previousError > TunerToyLogic.MAX_DISPLAY_CENTS || nearestError + TARGET_SWITCH_MARGIN_CENTS < previousError)
        val selected = if (shouldSwitch) nearest else previous
        stableTargetLabel = selected.label
        return selected
    }

    private fun smoothOffset(label: String, rawOffset: Float, deltaMs: Long): Float {
        if (label != lastTargetLabel) {
            lastTargetLabel = label
            smoothedCents = rawOffset
            return rawOffset
        }
        val previous = smoothedCents ?: rawOffset
        val elapsed = deltaMs.coerceIn(1L, 120L) / 33f
        val distance = abs(rawOffset - previous)
        val baseAlpha = when {
            distance <= 2f -> 0.16f
            distance <= 7f -> 0.24f
            else -> 0.46f
        }
        val alpha = (baseAlpha * elapsed).coerceIn(0.10f, 0.70f)
        val next = previous + (rawOffset - previous) * alpha
        smoothedCents = next
        return next
    }

    private fun heldSignalFrame(brightness: Int): GlyphFrame {
        val held = lastSignalFrame
        val now = System.currentTimeMillis()
        if (held != null && now - held.atMs <= NO_SIGNAL_HOLD_MS) {
            stableLockMs = 0L
            return TunerToyVisuals.drawTuner(
                label = held.label,
                cents = held.cents,
                toleranceCents = lockToleranceCents(),
                brightness = (brightness * 0.72f).toInt().coerceIn(35, 100)
            )
        }
        stableLockMs = 0L
        return TunerToyVisuals.drawNoSignal(brightness)
    }

    private fun maybePulseLock(offset: Float, deltaMs: Long) {
        if (!getSettings().bool("hapticLock", true)) return
        val tolerance = lockToleranceCents()
        if (abs(offset) <= tolerance) {
            stableLockMs += deltaMs
        } else {
            stableLockMs = 0L
            return
        }
        val now = System.currentTimeMillis()
        if (stableLockMs >= 120L && now - lastLockPulseAt > 1200L) {
            haptics?.pulseTunerLock()
            lastLockPulseAt = now
        }
    }

    private fun lockToleranceCents(): Int =
        getSettings().int("toleranceCents", 3).coerceIn(1, 20).coerceAtMost(3)

    private fun centsBetween(frequency: Float, target: Float): Float {
        if (frequency <= 0f || target <= 0f) return 0f
        return 1200f * (ln(frequency / target) / ln(2f))
    }

    private companion object {
        const val TARGET_SWITCH_MARGIN_CENTS = 18f
        const val NO_SIGNAL_HOLD_MS = 2_000L
    }

    private data class LastSignalFrame(
        val label: String,
        val cents: Float,
        val atMs: Long
    )
}
