package com.pelikan.glyphhub.audio

import android.content.Context
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.roundToInt

class PitchTracker(
    context: Context,
    private val detector: PitchDetector = YinPitchDetector(),
    private val smoothingWindow: Int = 5,
    private val minConfidence: Float = 0.78f,
    bufferSize: Int = AudioInputEngine.DEFAULT_BUFFER_SIZE
) {
    @Volatile private var latest: TunerReading = TunerReading.NoSignal
    private val recentFrequencies = ArrayDeque<Float>()
    private val engine = AudioInputEngine(context, bufferSize = bufferSize) { samples, sampleRate, metrics ->
        val detection = detector.detect(samples, sampleRate)
        latest = if (detection == null || detection.confidence < minConfidence) {
            TunerReading.NoSignal.copy(rms = metrics.rms)
        } else {
            recentFrequencies.addLast(detection.frequencyHz)
            while (recentFrequencies.size > smoothingWindow.coerceAtLeast(1)) recentFrequencies.removeFirst()
            val smoothed = recentFrequencies.toList().sorted().let { sorted ->
                sorted[sorted.size / 2]
            }
            TunerReading.Signal(
                frequencyHz = smoothed,
                confidence = detection.confidence,
                rms = metrics.rms,
                detectedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun start(): Boolean = engine.start()

    fun stop() = engine.stop()

    fun hasPermission(): Boolean = engine.hasRecordAudioPermission()

    fun reading(): TunerReading = latest
}

sealed class TunerReading {
    abstract val rms: Float

    data class Signal(
        val frequencyHz: Float,
        val confidence: Float,
        override val rms: Float,
        val detectedAtMs: Long
    ) : TunerReading()

    data class Silent(override val rms: Float = 0f) : TunerReading()

    companion object {
        val NoSignal = Silent()
    }
}

data class TuningTarget(
    val label: String,
    val frequencyHz: Float
)

object TuningMath {
    fun frequencyForMidi(midi: Int, a4Reference: Float): Float =
        (a4Reference * 2.0.pow((midi - 69) / 12.0)).toFloat()

    fun centsBetween(frequency: Float, target: Float): Int {
        if (frequency <= 0f || target <= 0f) return 0
        return (1200.0 * kotlin.math.log2(frequency / target)).roundToInt()
    }

    fun nearestTarget(frequency: Float, targets: List<TuningTarget>): TuningTarget? =
        targets.minByOrNull { kotlin.math.abs(centsBetween(frequency, it.frequencyHz)) }
}
