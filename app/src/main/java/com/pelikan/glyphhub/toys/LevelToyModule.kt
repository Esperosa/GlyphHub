package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import com.pelikan.glyphhub.accuracy.HapticFeedbackController
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphMatrixLayout
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class LevelToyModule : BaseGlyphToyModule() {
    override val id = "level"
    override val name = "Level Toy"
    override val shortName = "LVL"
    override val description = "Responsive bubble and edge level with distance-scaled haptic guidance."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("sensitivity", "Sensitivity", "How strongly tilt moves the indicator.", ToySettingType.Int, "82", 35, 100, scope = ToySettingScope.QUICK),
            ToySettingDefinition("hapticGuidance", "Haptics", "Guide leveling with distance-scaled pulses.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("showGuides", "Guides", "Show low-intensity target guides.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("smoothingPercent", "Smoothing", "Low-pass smoothing. Lower values react faster.", ToySettingType.Int, "7", 0, 45, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition("centerTolerance", "Tolerance", "Centered threshold radius.", ToySettingType.Int, "4", 1, 18, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition("calibrationOffsetX", "Cal X", "Calibration offset X.", ToySettingType.Int, "0", -50, 50, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition("calibrationOffsetY", "Cal Y", "Calibration offset Y.", ToySettingType.Int, "0", -50, 50, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "92", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var target = LevelReading()
    private var displayed = LevelReading()
    private var rawX = 0f
    private var rawY = 0f
    private var rawZ = 1f
    private var successPulseMs = 0L
    private var lastGuidanceAtMs = 0L
    private var wasCentered = false
    private var haptics: HapticFeedbackController? = null

    override fun onActivate(context: ToyRuntimeContext) {
        haptics = HapticFeedbackController(context.androidContext)
        target = LevelReading()
        displayed = LevelReading()
        rawX = 0f
        rawY = 0f
        rawZ = 1f
        successPulseMs = 0L
        lastGuidanceAtMs = 0L
        wasCentered = false
    }

    override fun onDeactivate() {
        haptics = null
        lastGuidanceAtMs = 0L
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Raw -> {
                if (event.sensorType != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
                rawX = (event.values[0] / GRAVITY).coerceIn(-1f, 1f)
                rawY = (event.values[1] / GRAVITY).coerceIn(-1f, 1f)
                rawZ = (event.values[2] / GRAVITY).coerceIn(-1f, 1f)
                val next = LevelToyLogic.reading(
                    ax = rawX,
                    ay = rawY,
                    az = rawZ,
                    previousMode = target.mode,
                    previousAxis = target.axis,
                    calibrationX = getSettings().int("calibrationOffsetX", 0) / 100f,
                    calibrationY = getSettings().int("calibrationOffsetY", 0) / 100f,
                    tolerance = tolerance()
                )
                if (next.mode != target.mode || next.axis != target.axis) {
                    displayed = next
                    successPulseMs = 0L
                }
                target = next
            }

            ToySensorEvent.BackTap -> {
                updateSettings(
                    getSettings()
                        .withValue("calibrationOffsetX", (rawX * 100f).roundToInt().coerceIn(-50, 50).toString())
                        .withValue("calibrationOffsetY", ((-rawY) * 100f).roundToInt().coerceIn(-50, 50).toString())
                )
                target = LevelReading(mode = target.mode, centered = true)
                displayed = target
                wasCentered = true
                lastGuidanceAtMs = 0L
            }

            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 92).coerceIn(0, 100)
        val sensitivity = getSettings().int("sensitivity", 82).coerceIn(35, 100) / 100f
        displayed = LevelToyLogic.smooth(
            previous = displayed,
            target = target,
            smoothing = getSettings().int("smoothingPercent", 7).coerceIn(0, 45) / 100f,
            tolerance = tolerance()
        )
        val centered = displayed.centered
        if (centered && !wasCentered) {
            successPulseMs = CENTER_VISUAL_PULSE_MS
            lastGuidanceAtMs = 0L
        }
        if (!centered && getSettings().bool("hapticGuidance", true)) {
            guideHaptics(displayed.normalizedError)
        } else if (centered) {
            lastGuidanceAtMs = 0L
        }
        wasCentered = centered
        successPulseMs = (successPulseMs - deltaMs).coerceAtLeast(0L)

        return LevelToyVisuals.draw(
            reading = displayed,
            brightness = brightness,
            sensitivity = sensitivity,
            showGuides = getSettings().bool("showGuides", false),
            pulseProgress = if (successPulseMs > 0L) 1f - successPulseMs / CENTER_VISUAL_PULSE_MS.toFloat() else -1f
        )
    }

    private fun guideHaptics(normalizedError: Float) {
        val now = System.currentTimeMillis()
        val strength = normalizedError.coerceIn(0f, 1f)
        if (strength <= 0.01f) return
        val interval = (115f - 86f * strength).roundToInt().coerceIn(28, 115).toLong()
        if (now - lastGuidanceAtMs < interval) return
        lastGuidanceAtMs = now
        haptics?.pulseLevelGuidance(strength)
    }

    private fun tolerance(): Float =
        getSettings().int("centerTolerance", 4).coerceIn(1, 18) / 100f

    private companion object {
        const val GRAVITY = 9.81f
        const val CENTER_VISUAL_PULSE_MS = 620L
    }
}

internal enum class LevelVisualMode {
    Bubble,
    Line
}

internal enum class LevelLineAxis {
    Horizontal,
    Vertical
}

internal data class LevelReading(
    val mode: LevelVisualMode = LevelVisualMode.Bubble,
    val axis: LevelLineAxis = LevelLineAxis.Horizontal,
    val bubbleX: Float = 0f,
    val bubbleY: Float = 0f,
    val lineTilt: Float = 0f,
    val normalizedError: Float = 0f,
    val centered: Boolean = true
)

internal object LevelToyLogic {
    fun reading(
        ax: Float,
        ay: Float,
        az: Float,
        previousMode: LevelVisualMode = LevelVisualMode.Bubble,
        previousAxis: LevelLineAxis = LevelLineAxis.Horizontal,
        calibrationX: Float,
        calibrationY: Float,
        tolerance: Float
    ): LevelReading {
        val mode = resolveMode(abs(az), previousMode)
        return if (mode == LevelVisualMode.Bubble) {
            val x = (ax - calibrationX).coerceIn(-1f, 1f)
            val y = (-ay - calibrationY).coerceIn(-1f, 1f)
            val distance = hypot(x.toDouble(), y.toDouble()).toFloat()
            LevelReading(
                mode = LevelVisualMode.Bubble,
                axis = LevelLineAxis.Horizontal,
                bubbleX = x,
                bubbleY = y,
                normalizedError = normalizeError(distance, tolerance),
                centered = distance <= tolerance
            )
        } else {
            val axis = resolveLineAxis(abs(ax), abs(ay), previousAxis)
            val tilt = if (axis == LevelLineAxis.Vertical) {
                (-ay - calibrationY).coerceIn(-1f, 1f)
            } else {
                (ax - calibrationX).coerceIn(-1f, 1f)
            }
            val distance = abs(tilt)
            LevelReading(
                mode = LevelVisualMode.Line,
                axis = axis,
                lineTilt = tilt,
                normalizedError = normalizeError(distance, tolerance),
                centered = distance <= tolerance
            )
        }
    }

    private fun resolveMode(absZ: Float, previousMode: LevelVisualMode): LevelVisualMode =
        when (previousMode) {
            LevelVisualMode.Bubble -> if (absZ <= ENTER_LINE_Z_THRESHOLD) LevelVisualMode.Line else LevelVisualMode.Bubble
            LevelVisualMode.Line -> if (absZ >= ENTER_BUBBLE_Z_THRESHOLD) LevelVisualMode.Bubble else LevelVisualMode.Line
        }

    private fun resolveLineAxis(absX: Float, absY: Float, previousAxis: LevelLineAxis): LevelLineAxis =
        when {
            absX >= 0.72f && absX >= absY * AXIS_DOMINANCE_RATIO -> LevelLineAxis.Vertical
            absY >= 0.72f && absY >= absX * AXIS_DOMINANCE_RATIO -> LevelLineAxis.Horizontal
            else -> previousAxis
        }

    fun smooth(previous: LevelReading, target: LevelReading, smoothing: Float, tolerance: Float): LevelReading {
        if (previous.mode != target.mode || previous.axis != target.axis) return target
        val s = smoothing.coerceIn(0f, 0.8f)
        val adaptive = if (abs(target.normalizedError - previous.normalizedError) > 0.22f) s * 0.45f else s
        val keep = adaptive
        val take = 1f - adaptive
        val bubbleX = previous.bubbleX * keep + target.bubbleX * take
        val bubbleY = previous.bubbleY * keep + target.bubbleY * take
        val lineTilt = previous.lineTilt * keep + target.lineTilt * take
        val error = when (target.mode) {
            LevelVisualMode.Bubble -> normalizeError(hypot(bubbleX.toDouble(), bubbleY.toDouble()).toFloat(), tolerance)
            LevelVisualMode.Line -> normalizeError(abs(lineTilt), tolerance)
        }
        return target.copy(
            bubbleX = bubbleX,
            bubbleY = bubbleY,
            lineTilt = lineTilt,
            normalizedError = error,
            centered = error <= 0f
        )
    }

    private fun normalizeError(distance: Float, tolerance: Float): Float {
        val safeTolerance = tolerance.coerceIn(0.01f, 0.3f)
        if (distance <= safeTolerance) return 0f
        return ((distance - safeTolerance) / (0.82f - safeTolerance).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
    }

    private const val ENTER_LINE_Z_THRESHOLD = 0.22f
    private const val ENTER_BUBBLE_Z_THRESHOLD = 0.92f
    private const val AXIS_DOMINANCE_RATIO = 1.35f
}

internal object LevelToyVisuals {
    fun draw(
        reading: LevelReading,
        brightness: Int,
        sensitivity: Float,
        showGuides: Boolean,
        pulseProgress: Float
    ): GlyphFrame {
        val frame = referenceFrame(brightness.coerceIn(0, 100), showGuides)
        return when (reading.mode) {
            LevelVisualMode.Bubble -> drawBubble(frame, reading, sensitivity, pulseProgress)
            LevelVisualMode.Line -> drawLine(frame, reading, pulseProgress)
        }.let(GlyphMatrixLayout::mask)
    }

    private fun referenceFrame(brightness: Int, showGuides: Boolean): GlyphFrame {
        var frame = GlyphFrame.empty13(brightness)
        if (showGuides) {
            outerRing.forEachIndexed { index, (x, y) ->
                if (index % 3 == 0) frame = setMax(frame, x, y, 10)
            }
        }
        frame = drawCenterCross(frame)
        return frame
    }

    private fun drawBubble(frame: GlyphFrame, reading: LevelReading, sensitivity: Float, pulseProgress: Float): GlyphFrame {
        val travel = 3.15f + sensitivity.coerceIn(0.35f, 1f) * 2.05f
        val centerX = 6f + reading.bubbleX.coerceIn(-1f, 1f) * travel
        val centerY = 6f + reading.bubbleY.coerceIn(-1f, 1f) * travel
        var output = frame
        val ballIntensity = if (reading.centered) 100 else 84 + (reading.normalizedError * 14f).roundToInt()
        output = drawSubpixelDisc(output, centerX, centerY, radius = 0.95f, intensity = ballIntensity)
        if (reading.centered && pulseProgress >= 0f) {
            output = drawSubpixelCircle(output, 6f, 6f, radius = 1.65f + pulseProgress.coerceIn(0f, 1f) * 0.95f, intensity = 36)
        } else {
            output = drawErrorArc(
                frame = output,
                normalizedError = reading.normalizedError,
                intensity = 56,
                centerAngle = vectorAngle(reading.bubbleX, reading.bubbleY)
            )
        }
        return output
    }

    private fun drawLine(frame: GlyphFrame, reading: LevelReading, pulseProgress: Float): GlyphFrame {
        var output = frame
        val tilt = reading.lineTilt.coerceIn(-1f, 1f)
        val baseRadians = if (reading.axis == LevelLineAxis.Vertical) (PI / 2.0).toFloat() else 0f
        val radians = baseRadians + tilt * 0.72f
        val lineIntensity = if (reading.centered) 100 else 82 + (reading.normalizedError * 16f).roundToInt()
        output = drawAntiAliasedLine(output, radians, lineIntensity)
        if (reading.centered && pulseProgress >= 0f) {
            output = drawSubpixelCircle(output, 6f, 6f, radius = 1.65f + pulseProgress.coerceIn(0f, 1f) * 0.85f, intensity = 38)
        } else {
            output = drawErrorArc(
                frame = output,
                normalizedError = reading.normalizedError,
                intensity = 52,
                centerAngle = lineErrorAngle(reading.axis, reading.lineTilt)
            )
        }
        return output
    }

    private fun drawCenterCross(frame: GlyphFrame): GlyphFrame {
        var output = frame
        output = setMax(output, 6, 6, 78)
        listOf(6 to 5, 7 to 6, 6 to 7, 5 to 6).forEach { (x, y) ->
            output = setMax(output, x, y, 42)
        }
        listOf(6 to 4, 8 to 6, 6 to 8, 4 to 6).forEach { (x, y) ->
            output = setMax(output, x, y, 18)
        }
        return output
    }

    private fun drawAntiAliasedLine(frame: GlyphFrame, radians: Float, intensity: Int): GlyphFrame {
        var output = frame
        val sin = sin(radians)
        val cos = cos(radians)
        for (y in 1..11) {
            for (x in 1..11) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val dx = x - 6f
                val dy = y - 6f
                val distance = abs(dx * sin - dy * cos)
                val along = abs(dx * cos + dy * sin)
                if (along > 5.45f || distance > 1.05f) continue
                val value = when {
                    distance <= 0.25f -> intensity
                    distance <= 0.65f -> (intensity * (1f - (distance - 0.25f) / 0.4f * 0.45f)).roundToInt()
                    else -> (intensity * (1f - (distance - 0.65f) / 0.4f) * 0.45f).roundToInt()
                }.coerceIn(0, 100)
                output = setMax(output, x, y, value)
            }
        }
        return output
    }

    private fun drawSubpixelDisc(frame: GlyphFrame, cx: Float, cy: Float, radius: Float, intensity: Int): GlyphFrame {
        var output = frame
        for (y in 1..11) {
            for (x in 1..11) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val distance = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                if (distance > radius + 0.9f) continue
                val value = when {
                    distance <= radius * 0.45f -> intensity
                    distance <= radius -> (intensity * (0.72f + (radius - distance) / radius * 0.28f)).roundToInt()
                    else -> (intensity * (1f - (distance - radius) / 0.9f) * 0.62f).roundToInt()
                }.coerceIn(0, 100)
                output = setMax(output, x, y, value)
            }
        }
        return output
    }

    private fun drawSubpixelCircle(frame: GlyphFrame, cx: Float, cy: Float, radius: Float, intensity: Int): GlyphFrame {
        var output = frame
        for (y in 1..11) {
            for (x in 1..11) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val distance = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                val delta = abs(distance - radius)
                if (delta > 0.65f) continue
                output = setMax(output, x, y, (intensity * (1f - delta / 0.65f)).roundToInt())
            }
        }
        return output
    }

    private fun drawErrorArc(frame: GlyphFrame, normalizedError: Float, intensity: Int, centerAngle: Double): GlyphFrame {
        val active = (2f + normalizedError.coerceIn(0f, 1f) * 8f).roundToInt().coerceIn(2, 10)
        val centerIndex = ringIndexForAngle(centerAngle)
        var output = frame
        for (offset in -(active / 2)..(active / 2)) {
            val index = (centerIndex + offset).floorMod(outerRing.size)
            val (x, y) = outerRing[index]
            val fade = 1f - (abs(offset).toFloat() / (active / 2f + 1f)).coerceIn(0f, 1f) * 0.58f
            val value = (intensity * fade).roundToInt()
            output = setMax(output, x, y, value)
        }
        return output
    }

    private fun vectorAngle(x: Float, y: Float): Double {
        val angle = atan2(y.toDouble(), x.toDouble()) + PI / 2.0
        return if (angle < 0.0) angle + PI * 2.0 else angle
    }

    private fun lineErrorAngle(axis: LevelLineAxis, tilt: Float): Double =
        when (axis) {
            LevelLineAxis.Horizontal -> if (tilt >= 0f) PI / 2.0 else PI * 1.5
            LevelLineAxis.Vertical -> if (tilt >= 0f) PI else 0.0
        }

    private fun ringIndexForAngle(angle: Double): Int {
        val normalized = ((angle % (PI * 2.0)) + PI * 2.0) % (PI * 2.0)
        return (normalized / (PI * 2.0) * outerRing.size).roundToInt().floorMod(outerRing.size)
    }

    private fun setMax(frame: GlyphFrame, x: Int, y: Int, intensity: Int): GlyphFrame {
        if (!GlyphMatrixLayout.isPhysicalLed(x, y)) return frame
        val next = intensity.coerceIn(0, 100)
        if (next <= frame.intensityAt(x, y)) return frame
        return frame.withPixelBrightness(x, y, next)
    }

    private val outerRing = buildList {
        for (y in 0 until GlyphFrame.MATRIX_SIZE) {
            for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                if (!GlyphMatrixLayout.isPhysicalLed(x, y)) continue
                val edge = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
                    .any { (nx, ny) -> !GlyphMatrixLayout.isPhysicalLed(nx, ny) }
                if (edge) add(x to y)
            }
        }
    }.sortedBy { (x, y) ->
        val angle = atan2((y - 6).toDouble(), (x - 6).toDouble()) + PI / 2.0
        if (angle < 0.0) angle + PI * 2.0 else angle
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
