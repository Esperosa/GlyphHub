package com.pelikan.glyphhub.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioInputEngine(
    private val context: Context,
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val onSamples: (FloatArray, Int, AudioMetrics) -> Unit
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    private var recorder: AudioRecord? = null

    fun start(): Boolean {
        if (running.get()) return true
        if (!hasRecordAudioPermission()) return false
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val actualBuffer = maxOf(minBuffer * 2, bufferSize)
        val created = createAudioRecord(actualBuffer)
        if (created == null || created.state != AudioRecord.STATE_INITIALIZED) {
            Log.w("GlyphHub", "audio input start failed initialized=${created?.state}")
            created?.release()
            return false
        }
        recorder = created
        running.set(true)
        thread = Thread({ readLoop(created) }, "GlyphHubAudioInput").also { it.start() }
        return true
    }

    private fun createAudioRecord(actualBuffer: Int): AudioRecord? {
        val sources = listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)
        for (source in sources) {
            val created = runCatching {
                @Suppress("MissingPermission")
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    actualBuffer
                )
            }
                .onFailure { Log.w("GlyphHub", "audio input create failed source=$source ${it.message}") }
                .getOrNull()
            if (created != null && created.state == AudioRecord.STATE_INITIALIZED) {
                Log.d("GlyphHub", "audio input initialized source=$source sampleRate=$sampleRate buffer=$actualBuffer")
                return created
            }
            created?.release()
        }
        return null
    }

    fun stop() {
        running.set(false)
        runCatching { recorder?.stop() }
        thread?.join(250L)
        thread = null
        runCatching { recorder?.release() }
        recorder = null
    }

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun readLoop(audioRecord: AudioRecord) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val pcm = ShortArray(bufferSize)
        val samples = FloatArray(bufferSize)
        runCatching { audioRecord.startRecording() }
            .onSuccess { Log.d("GlyphHub", "audio input recording started state=${audioRecord.recordingState}") }
            .onFailure {
                Log.w("GlyphHub", "audio input recording failed ${it.message}")
                running.set(false)
            }
        while (running.get()) {
            val read = audioRecord.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
            if (read <= 0) continue
            var peak = 0f
            var sum = 0f
            for (index in 0 until read) {
                val value = pcm[index] / 32768f
                samples[index] = value
                val abs = kotlin.math.abs(value)
                if (abs > peak) peak = abs
                sum += value * value
            }
            val rms = sqrt(sum / read)
            onSamples(samples.copyOf(read), sampleRate, AudioMetrics(rms = rms, peak = peak))
        }
    }

    data class AudioMetrics(
        val rms: Float,
        val peak: Float
    )

    companion object {
        const val DEFAULT_SAMPLE_RATE = 22_050
        const val DEFAULT_BUFFER_SIZE = 2_048
    }
}
