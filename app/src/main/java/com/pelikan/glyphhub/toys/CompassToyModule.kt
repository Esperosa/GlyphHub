package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary as SharedGlyphIconLibrary
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class CompassToyModule : BaseGlyphToyModule() {
    override val id = "compass"
    override val name = "Compass Toy"
    override val shortName = "NAVI"
    override val description = "Render heading as arrow, cardinal label, or edge dot."
    override val iconAsset = "glyphs/compass/compass_arrow_n.json"
    override val supportsAod = false
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("smoothRotation", "Smooth rotation", "Filter heading movement.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("calibrationWarning", "Calibration warning", "Warn when heading is noisy.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("updateRateMs", "Update rate ms", "Sensor update interval.", ToySettingType.Int, "120", 40, 1000),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            ToySettingDefinition(
                "displayMode",
                "Display mode",
                "Compass matrix style.",
                ToySettingType.Choice,
                "arrow",
                options = listOf(
                    ToySettingOption("arrow", "Arrow"),
                    ToySettingOption("cardinal", "Cardinal"),
                    ToySettingOption("minimal_dot", "Minimal dot")
                )
            ),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var heading = 0f
    private var displayHeading = 0f

    override fun onActivate(context: ToyRuntimeContext) {
        displayHeading = heading
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event is ToySensorEvent.Heading) {
            heading = normalizeHeading(event.degrees)
            displayHeading = if (getSettings().bool("smoothRotation", true)) {
                smoothHeading(displayHeading, heading)
            } else {
                heading
            }
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val activeHeading = if (getSettings().bool("smoothRotation", true)) displayHeading else heading
        return when (getSettings().choice("displayMode", "arrow")) {
            "cardinal" -> drawCenteredText3x5(cardinal8(activeHeading), brightness, yOffset = 4)
            "minimal_dot" -> edgeDot(activeHeading, brightness)
            else -> arrow(activeHeading, brightness)
        }
    }

    private fun cardinal(degrees: Float): String =
        when (((degrees + 45f) / 90f).toInt().floorMod(4)) {
            0 -> "N"
            1 -> "E"
            2 -> "S"
            else -> "W"
        }

    private fun cardinal8(degrees: Float): String =
        when (((degrees + 22.5f) / 45f).toInt().floorMod(8)) {
            0 -> "N"
            1 -> "NE"
            2 -> "E"
            3 -> "SE"
            4 -> "S"
            5 -> "SW"
            6 -> "W"
            else -> "NW"
        }

    private fun arrow(degrees: Float, brightness: Int): GlyphFrame {
        return SharedGlyphIconLibrary.compassNeedle(degrees, brightness)
    }

    private fun edgeDot(degrees: Float, brightness: Int): GlyphFrame {
        val radians = Math.toRadians(degrees.toDouble() - 90.0)
        val x = (6.0 + cos(radians) * GlyphDesignSystem.fullNeedleLength).roundToInt().coerceIn(0, 12)
        val y = (6.0 + sin(radians) * GlyphDesignSystem.fullNeedleLength).roundToInt().coerceIn(0, 12)
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), radius = GlyphDesignSystem.fullRingRadius, intensity = 30, thickness = 0.48)
        frame = GlyphDesignSystem.drawDot(frame, x, y, 100)
        frame = GlyphDesignSystem.drawPixelSafe(frame, 6, 6, 72)
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun smoothHeading(current: Float, target: Float): Float {
        val delta = ((target - current + 540f) % 360f) - 180f
        return normalizeHeading(current + delta * 0.28f)
    }

    private fun normalizeHeading(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
