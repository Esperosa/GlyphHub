package com.pelikan.glyphhub.accuracy

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.roundToInt

class HapticFeedbackController(context: Context) {
    private val appContext = context.applicationContext

    fun pulseGuidance(normalizedDistance: Float) {
        val duration = (14 + normalizedDistance.coerceIn(0f, 1.5f) * 32f).roundToInt().toLong()
        pulse(duration)
    }

    fun pulseLevelGuidance(normalizedDistance: Float) {
        val distance = normalizedDistance.coerceIn(0f, 1f)
        val duration = (16f + distance * 72f).roundToInt().coerceIn(16, 90).toLong()
        val amplitude = (90f + distance * 165f).roundToInt().coerceIn(90, 255)
        val vibrator = vibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun pulseSuccess() {
        pulse(18L)
    }

    fun pulseCompletion() {
        pulse(60L)
    }

    fun pulseWarning() {
        vibrateWaveform(
            timings = longArrayOf(0L, 92L, 24L, 118L, 32L, 154L),
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        )
    }

    fun pulseUrgentWarning() {
        vibrateWaveform(
            timings = longArrayOf(0L, 68L, 18L, 78L, 18L, 104L, 24L, 136L, 28L, 190L),
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
        )
    }

    fun pulseSchoolPreStartWarning() {
        vibrateWaveform(
            timings = longArrayOf(0L, 130L, 24L, 150L, 24L, 190L, 32L, 230L, 36L, 280L, 42L, 340L, 54L, 420L, 70L, 310L),
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
        )
    }

    fun pulseTunerLock() {
        vibrateWaveform(
            timings = longArrayOf(0L, 70L, 18L, 78L),
            amplitudes = intArrayOf(0, 255, 0, 255)
        )
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        val vibrator = vibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    fun pulse(durationMs: Long) {
        val vibrator = vibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
