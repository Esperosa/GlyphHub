package com.pelikan.glyphhub.glyph

import android.os.SystemClock
import com.pelikan.glyphhub.glyph.validation.GlyphFrameDiff
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidationOptions
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidator
import com.pelikan.glyphhub.glyph.validation.GlyphValidationSeverity

internal class GlyphRenderScheduler(
    private val controller: GlyphMatrixController,
    private val appendLog: (String) -> Unit,
    private val debugMode: () -> Boolean,
    private val allowFullMatrixFlashForDebug: () -> Boolean = { false }
) {
    private var lastFrame: GlyphFrame? = null
    private var lastDispatchAt = 0L

    fun resetTracking() {
        lastFrame = null
        lastDispatchAt = 0L
    }

    fun submit(
        sessionId: Long,
        source: String,
        frame: GlyphFrame,
        transitionId: String? = null,
        now: Long = SystemClock.elapsedRealtime(),
        keepAliveMs: Long = DEFAULT_KEEPALIVE_MS,
        intentionalFullMatrixFlash: Boolean = false,
        allowAllOff: Boolean = false
    ): Boolean {
        val validation = GlyphFrameValidator.validate(
            frame,
            GlyphFrameValidationOptions(
                allowAllOff = allowAllOff,
                allowFullMatrixFlash = intentionalFullMatrixFlash && allowFullMatrixFlashForDebug(),
                allowOffMaskPixels = false,
                particleEffect = source.contains("particle", ignoreCase = true),
                directional = source.contains("compass", ignoreCase = true) || source.contains("needle", ignoreCase = true),
                textRendererSource = source.contains("text", ignoreCase = true)
            )
        )
        if (debugMode()) {
            validation.issues.forEach { issue ->
                appendLog(
                    "render ${issue.severity.name.lowercase()} session=$sessionId source=$source transition=${transitionId ?: "-"} code=${issue.code} ${issue.message}"
                )
            }
            val metrics = validation.metrics
            val diff = GlyphFrameDiff.between(lastFrame, validation.sanitizedFrame)
            appendLog(
                "render metrics session=$sessionId source=$source lit=${metrics.physicalLitPixels}/${metrics.physicalLedCount} " +
                    "bbox=${metrics.boundingBox?.let { "${it.minX},${it.minY}-${it.maxX},${it.maxY}" } ?: "-"} " +
                    "com=${metrics.centerOfMassX?.format1() ?: "-"},${metrics.centerOfMassY?.format1() ?: "-"} " +
                    "intensity=${metrics.minIntensity}-${metrics.maxIntensity} unique=${metrics.uniqueIntensityCount} " +
                    "normalized=true changed=${diff.changedPixels}"
            )
        }
        if (!validation.valid) {
            appendLog(
                "render rejected session=$sessionId source=$source transition=${transitionId ?: "-"} " +
                    "reason=${validation.errors.joinToString(",") { it.code }} kept_previous=${lastFrame != null}"
            )
            return false
        }
        if (validation.issues.any { it.code == "accidental_full_matrix_flash" && it.severity == GlyphValidationSeverity.Error }) {
            return false
        }
        val safeFrame = validation.sanitizedFrame
        if (safeFrame == lastFrame && now - lastDispatchAt < keepAliveMs) {
            return false
        }
        if (debugMode()) {
            val metrics = validation.metrics
            if (metrics.fillRatioPercent >= HIGH_FILL_RATIO_PERCENT) {
                appendLog(
                    "render suspicious session=$sessionId source=$source transition=${transitionId ?: "-"} " +
                        "reason=high_fill_ratio lit=${metrics.physicalLitPixels}/${metrics.physicalLedCount}"
                )
            }
        }
        controller.renderFrame(safeFrame)
        lastFrame = safeFrame
        lastDispatchAt = now
        return true
    }

    private fun Double.format1(): String = ((this * 10.0).toInt() / 10.0).toString()

    private companion object {
        const val DEFAULT_KEEPALIVE_MS = 1000L
        const val HIGH_FILL_RATIO_PERCENT = 85
    }
}
