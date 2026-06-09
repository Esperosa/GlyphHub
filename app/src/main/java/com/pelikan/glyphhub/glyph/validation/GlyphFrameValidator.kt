package com.pelikan.glyphhub.glyph.validation

import com.pelikan.glyphhub.glyph.GlyphFrame

enum class GlyphValidationSeverity {
    Warning,
    Error
}

data class GlyphValidationIssue(
    val code: String,
    val severity: GlyphValidationSeverity,
    val message: String
)

data class GlyphFrameValidationOptions(
    val allowAllOff: Boolean = false,
    val allowOffMaskPixels: Boolean = false,
    val allowFullMatrixFlash: Boolean = false,
    val particleEffect: Boolean = false,
    val directional: Boolean = false,
    val textRendererSource: Boolean = false
)

data class GlyphFrameValidationResult(
    val valid: Boolean,
    val frame: GlyphFrame,
    val sanitizedFrame: GlyphFrame,
    val metrics: GlyphFrameMetrics,
    val issues: List<GlyphValidationIssue>
) {
    val warnings: List<GlyphValidationIssue> get() = issues.filter { it.severity == GlyphValidationSeverity.Warning }
    val errors: List<GlyphValidationIssue> get() = issues.filter { it.severity == GlyphValidationSeverity.Error }
}

object GlyphFrameValidator {
    fun validate(frame: GlyphFrame, options: GlyphFrameValidationOptions = GlyphFrameValidationOptions()): GlyphFrameValidationResult {
        val normalized = GlyphFrameNormalizer.normalize(frame, respectCircularMask = !options.allowOffMaskPixels)
        val originalMetrics = GlyphFrameMetrics.from(frame)
        val metrics = GlyphFrameMetrics.from(normalized)
        val issues = mutableListOf<GlyphValidationIssue>()

        if (frame.width != GlyphFrame.MATRIX_SIZE || frame.height != GlyphFrame.MATRIX_SIZE) {
            issues += error("matrix_size", "Frame must be 13x13; got ${frame.width}x${frame.height}.")
        }
        if (originalMetrics.outOfMaskPixels > 0 && !options.allowOffMaskPixels) {
            issues += warning("off_mask_pixels", "Cleared ${originalMetrics.outOfMaskPixels} off-mask pixel(s).")
        }
        if (metrics.isAllOn && !options.allowFullMatrixFlash) {
            issues += error("accidental_full_matrix_flash", "Frame lights the full physical Matrix.")
        }
        if (metrics.isAllOff && !options.allowAllOff) {
            issues += error("accidental_all_off", "Frame is all-off during active output.")
        }
        if (metrics.isolatedPixels > 0 && !options.particleEffect) {
            issues += warning("isolated_pixels", "Frame contains ${metrics.isolatedPixels} isolated lit pixel(s).")
        }
        if (metrics.isTiny && !options.textRendererSource) {
            issues += warning("too_small", "Main shape is likely too small for readable 13x13 output.")
        }
        val box = metrics.boundingBox
        if (box != null && !options.directional) {
            val centerX = metrics.centerOfMassX ?: 6.0
            val centerY = metrics.centerOfMassY ?: 6.0
            if (kotlin.math.abs(centerX - 6.0) > 1.65 || kotlin.math.abs(centerY - 6.0) > 1.65) {
                issues += warning("off_center", "Center of mass is ${format(centerX)},${format(centerY)}.")
            }
        }
        if (metrics.maxIntensity > 100 || metrics.minIntensity < 0) {
            issues += error("non_normalized_intensity", "Frame intensity must be normalized to 0..100.")
        }

        return GlyphFrameValidationResult(
            valid = issues.none { it.severity == GlyphValidationSeverity.Error },
            frame = frame,
            sanitizedFrame = normalized,
            metrics = metrics,
            issues = issues
        )
    }

    private fun warning(code: String, message: String) =
        GlyphValidationIssue(code, GlyphValidationSeverity.Warning, message)

    private fun error(code: String, message: String) =
        GlyphValidationIssue(code, GlyphValidationSeverity.Error, message)

    private fun format(value: Double): String =
        ((value * 10.0).toInt() / 10.0).toString()
}
