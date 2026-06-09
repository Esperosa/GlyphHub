package com.pelikan.glyphhub.glyph

import android.util.Log
import com.pelikan.glyphhub.settings.SettingsRepository

class FakeGlyphMatrixController(
    override val matrixWidth: Int = GlyphMatrixController.DEFAULT_MATRIX_SIZE,
    override val matrixHeight: Int = GlyphMatrixController.DEFAULT_MATRIX_SIZE
) : GlyphMatrixController {
    override val isRealSdk: Boolean = false
    override var lastFrame: GlyphFrame? = null
        private set

    override fun connect() {
        Log.d(SettingsRepository.LOG_TAG, "Fake Glyph Matrix connected ${matrixWidth}x$matrixHeight")
    }

    override fun renderFrame(frame: GlyphFrame) {
        lastFrame = frame
        Log.d(SettingsRepository.LOG_TAG, "Fake render brightness=${frame.brightness}")
    }

    override fun close() {
        Log.d(SettingsRepository.LOG_TAG, "Fake Glyph Matrix closed")
    }
}
