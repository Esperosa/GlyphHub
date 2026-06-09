package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

class LuxMeterToyModule : BaseGlyphToyModule() {
    override val id = "lux_meter"
    override val name = "Lux Meter Toy"
    override val shortName = "LUX"
    override val description = "Ambient light meter using the device light sensor when available."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Show bars, digits, or both.",
                ToySettingType.Choice,
                "hybrid",
                options = listOf(
                    ToySettingOption("bar", "Bar"),
                    ToySettingOption("digits", "Digits"),
                    ToySettingOption("hybrid", "Hybrid")
                )
            ),
            ToySettingDefinition("peakHold", "Peak hold", "Keep and slowly decay the peak level.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var currentLux = 0f
    private var peakLux = 0f
    private var hasReading = false

    override fun onActivate(context: ToyRuntimeContext) {
        currentLux = 0f
        peakLux = 0f
        hasReading = false
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Raw -> {
                if (event.sensorType != Sensor.TYPE_LIGHT || event.values.isEmpty()) return
                currentLux = event.values[0].coerceAtLeast(0f)
                peakLux = max(peakLux, currentLux)
                hasReading = true
            }
            ToySensorEvent.BackTap -> peakLux = currentLux
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        if (!hasReading) {
            return drawCenteredText3x5("NO", brightness, yOffset = 4)
        }
        if (getSettings().bool("peakHold", true) && peakLux > currentLux) {
            peakLux = max(currentLux, peakLux - deltaMs * 0.4f)
        } else {
            peakLux = currentLux
        }
        val displayMode = getSettings().choice("displayMode", "hybrid")
        val currentScale = normalizeLux(currentLux)
        val peakScale = normalizeLux(peakLux)
        val frame = frameFromPoints(buildSet {
            for (x in 1..11) {
                add(x to 1)
                add(x to 11)
            }
            for (y in 1..11) {
                add(1 to y)
                add(11 to y)
            }
            if (displayMode != "digits") {
                val currentHeight = (currentScale * 7f).roundToInt().coerceIn(0, 7)
                val peakHeight = (peakScale * 7f).roundToInt().coerceIn(0, 7)
                for (step in 0 until currentHeight) {
                    for (x in 2..4) add(x to (10 - step))
                }
                if (getSettings().bool("peakHold", true)) {
                    for (step in 0 until peakHeight) {
                        for (x in 8..10) add(x to (10 - step))
                    }
                }
            }
        }, brightness)
        return if (displayMode == "bar") {
            frame
        } else {
            frame.overlay(drawCenteredText3x5(currentLux.roundToInt().coerceAtMost(999).toString(), brightness, yOffset = 3))
        }
    }

    private fun normalizeLux(value: Float): Float =
        (log10(value + 1f) / 3f).coerceIn(0f, 1f)
}