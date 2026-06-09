package com.pelikan.glyphhub.accuracy

import android.media.AudioManager
import android.media.ToneGenerator

class AudioFeedbackController {
    @Suppress("DEPRECATION")
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)

    fun playTickTone(accent: Boolean = false) {
        toneGenerator.startTone(
            if (accent) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
            if (accent) 90 else 55
        )
    }

    fun playCompletionTone() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
    }

    fun release() {
        toneGenerator.release()
    }
}
