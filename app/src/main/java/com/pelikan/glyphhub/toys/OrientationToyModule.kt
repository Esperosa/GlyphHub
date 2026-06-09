package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import android.hardware.SensorManager
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.roundToInt

class OrientationToyModule : BaseGlyphToyModule() {
    override val id = "orientation"
    override val name = "Orientation Gauge"
    override val shortName = "ANGLE"
    override val description = "Rotation-vector roll angle gauge with calibration offset."
    override val iconAsset = "orientation"
    override val supportsAod = false
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("showDegrees", "Degrees", "Show rounded angle digits.", ToySettingType.Boolean, "true", scope = ToySettingScope.QUICK),
            ToySettingDefinition("calibrationOffset", "Cal", "Roll calibration offset.", ToySettingType.Int, "0", -180, 180),
            ToySettingDefinition("smoothingPercent", "Smoothing", "Low-pass smoothing.", ToySettingType.Int, "70", 0, 95),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private val matrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var rollDegrees = 0f
    private var hasSample = false

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Raw -> if (event.sensorType == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                val smoothing = getSettings().int("smoothingPercent", 70).coerceIn(0, 95) / 100f
                rollDegrees = rollDegrees * smoothing + rawRoll * (1f - smoothing)
                hasSample = true
            }
            ToySensorEvent.BackTap -> updateSettings(getSettings().withValue("calibrationOffset", rollDegrees.roundToInt().toString()))
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        if (!hasSample) return drawCenteredText3x5("WAIT", brightness, yOffset = 4)
        val calibrated = (rollDegrees - getSettings().int("calibrationOffset", 0)).coerceIn(-180f, 180f)
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 36, thickness = 0.5)
        frame = GlyphDesignSystem.drawNeedle(frame, calibrated.toDouble(), GlyphDesignSystem.fullNeedleLength, 100, thickness = 0.35)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 90)
        if (getSettings().bool("showDegrees", true)) {
            frame = frame.overlay(drawCenteredText3x5(kotlin.math.abs(calibrated).roundToInt().coerceAtMost(99).toString().padStart(2, '0'), brightness, yOffset = 8))
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }
}
