package com.pelikan.glyphhub.audio

import kotlin.math.sqrt

class YinPitchDetector(
    private val minFrequencyHz: Float = 50f,
    private val maxFrequencyHz: Float = 1_200f,
    private val threshold: Float = 0.16f,
    private val minRms: Float = 0.008f
) : PitchDetector {
    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (samples.size < 256 || sampleRate <= 0) return null
        val rms = rms(samples)
        if (rms < minRms) return null

        val minTau = (sampleRate / maxFrequencyHz).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / minFrequencyHz).toInt().coerceIn(minTau + 1, samples.size / 2)
        val difference = FloatArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0f
            var index = 0
            val limit = samples.size - tau
            while (index < limit) {
                val delta = samples[index] - samples[index + tau]
                sum += delta * delta
                index += 1
            }
            difference[tau] = sum
        }

        val cmnd = FloatArray(maxTau + 1)
        cmnd[0] = 1f
        var runningSum = 0f
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            cmnd[tau] = if (runningSum > 0f) difference[tau] * tau / runningSum else 1f
        }

        var tauEstimate = -1
        var tau = minTau
        while (tau <= maxTau) {
            if (cmnd[tau] < threshold) {
                while (tau + 1 <= maxTau && cmnd[tau + 1] < cmnd[tau]) tau += 1
                tauEstimate = tau
                break
            }
            tau += 1
        }
        if (tauEstimate < 0) {
            tauEstimate = (minTau..maxTau).minByOrNull { cmnd[it] } ?: return null
            if (cmnd[tauEstimate] > 0.32f) return null
        }

        val betterTau = parabolicTau(cmnd, tauEstimate)
        val frequency = sampleRate / betterTau
        if (frequency !in minFrequencyHz..maxFrequencyHz) return null
        return PitchDetectionResult(
            frequencyHz = frequency,
            confidence = (1f - cmnd[tauEstimate]).coerceIn(0f, 1f),
            rms = rms
        )
    }

    private fun rms(samples: FloatArray): Float {
        var sum = 0f
        samples.forEach { sum += it * it }
        return sqrt(sum / samples.size)
    }

    private fun parabolicTau(values: FloatArray, tau: Int): Float {
        if (tau <= 0 || tau >= values.lastIndex) return tau.toFloat()
        val left = values[tau - 1]
        val center = values[tau]
        val right = values[tau + 1]
        val denominator = (2f * center - left - right)
        if (denominator == 0f) return tau.toFloat()
        return tau + (right - left) / (2f * denominator)
    }
}
