package com.pelikan.glyphhub.audio

data class PitchDetectionResult(
    val frequencyHz: Float,
    val confidence: Float,
    val rms: Float
)

interface PitchDetector {
    fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult?
}
