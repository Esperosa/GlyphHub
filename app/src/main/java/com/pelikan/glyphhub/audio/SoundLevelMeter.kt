package com.pelikan.glyphhub.audio

import android.content.Context
import kotlin.math.log10
import kotlin.math.max

class SoundLevelMeter(
    context: Context,
    bufferSize: Int = AudioInputEngine.DEFAULT_BUFFER_SIZE
) {
    @Volatile var latest: SoundLevelReading = SoundLevelReading.NoSignal
        private set

    private var peakDb = MIN_DB
    private val engine = AudioInputEngine(context, bufferSize = bufferSize) { _, _, metrics ->
        val db = approximateDb(metrics.rms)
        peakDb = max(peakDb - PEAK_DECAY_DB, db)
        latest = SoundLevelReading.Signal(
            rms = metrics.rms,
            peak = metrics.peak,
            approximateDb = db,
            peakApproximateDb = peakDb,
            detectedAtMs = System.currentTimeMillis()
        )
    }

    fun start(): Boolean = engine.start()

    fun stop() = engine.stop()

    fun hasPermission(): Boolean = engine.hasRecordAudioPermission()

    fun resetPeak() {
        peakDb = MIN_DB
    }

    private fun approximateDb(rms: Float): Float {
        if (rms <= 0.00001f) return MIN_DB
        return (20f * log10(rms) + REFERENCE_OFFSET_DB).coerceIn(MIN_DB, MAX_DB)
    }

    companion object {
        const val MIN_DB = 20f
        const val MAX_DB = 100f
        private const val REFERENCE_OFFSET_DB = 94f
        private const val PEAK_DECAY_DB = 0.5f
    }
}

sealed class SoundLevelReading {
    data class Signal(
        val rms: Float,
        val peak: Float,
        val approximateDb: Float,
        val peakApproximateDb: Float,
        val detectedAtMs: Long
    ) : SoundLevelReading()

    data object NoSignal : SoundLevelReading()
}
