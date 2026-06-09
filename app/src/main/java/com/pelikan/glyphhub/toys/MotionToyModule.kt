package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MotionToyModule : BaseGlyphToyModule() {
    override val id = "motion"
    override val name = "Step / Motion"
    override val shortName = "MOVE"
    override val description = "Step detector when available, otherwise live motion intensity."
    override val iconAsset = "motion"
    override val supportsAod = false
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Steps or motion intensity.",
                ToySettingType.Choice,
                "auto",
                options = listOf(ToySettingOption("auto", "Auto"), ToySettingOption("steps", "Steps"), ToySettingOption("motion", "Motion")),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var steps = 0
    private var baseCounter: Float? = null
    private var motion = 0f
    private var lastAccel = 9.81f
    private var hasStepSensor = false

    override fun onActivate(context: ToyRuntimeContext) {
        steps = 0
        baseCounter = null
        motion = 0f
        hasStepSensor = false
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event !is ToySensorEvent.Raw) return
        when (event.sensorType) {
            Sensor.TYPE_STEP_DETECTOR -> {
                hasStepSensor = true
                steps += event.values.firstOrNull()?.roundToInt()?.coerceAtLeast(1) ?: 1
            }
            Sensor.TYPE_STEP_COUNTER -> {
                hasStepSensor = true
                val raw = event.values.firstOrNull() ?: return
                val base = baseCounter ?: raw.also { baseCounter = it }
                steps = (raw - base).roundToInt().coerceAtLeast(0)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size < 3) return
                val magnitude = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                motion = (motion * 0.82f + abs(magnitude - lastAccel) * 0.18f).coerceIn(0f, 12f)
                lastAccel = magnitude
            }
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val mode = getSettings().choice("displayMode", "auto")
        return if ((mode == "auto" && hasStepSensor) || mode == "steps") {
            drawCenteredText3x5(steps.coerceAtMost(999).toString(), brightness, yOffset = 4)
        } else {
            val progress = (motion / 8f).coerceIn(0f, 1f)
            var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 35, thickness = 0.5)
            frame = GlyphDesignSystem.drawProgressRing(frame, progress, 100)
            frame.overlay(drawCenteredText3x5("MOV", brightness, yOffset = 4))
        }
    }
}
