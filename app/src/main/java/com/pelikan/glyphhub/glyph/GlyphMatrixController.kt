package com.pelikan.glyphhub.glyph

import android.content.Context

interface GlyphMatrixController {
    val matrixWidth: Int
    val matrixHeight: Int
    val isRealSdk: Boolean
    val lastFrame: GlyphFrame?

    fun connect()
    fun renderFrame(frame: GlyphFrame)
    fun close()

    companion object {
        const val DEVICE_IDENTIFIER = "Glyph.DEVICE_25111p"
        const val DEFAULT_MATRIX_SIZE = 13

        fun create(context: Context): GlyphMatrixController =
            if (RealGlyphMatrixController.isSdkAvailable()) {
                RealGlyphMatrixController(context.applicationContext)
            } else {
                FakeGlyphMatrixController(DEFAULT_MATRIX_SIZE, DEFAULT_MATRIX_SIZE)
            }
    }
}
