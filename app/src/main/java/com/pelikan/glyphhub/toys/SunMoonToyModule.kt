package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class SunMoonToyModule : BaseGlyphToyModule() {
    override val id = "sun_moon"
    override val name = "Sun / Moon Phase"
    override val shortName = "MOON"
    override val description = "Date-based moon phase and simple day/night symbol."
    override val iconAsset = "sun_moon"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Sun, moon, or automatic day/night.",
                ToySettingType.Choice,
                "auto",
                options = listOf(ToySettingOption("auto", "Auto"), ToySettingOption("moon", "Moon"), ToySettingOption("sun", "Sun")),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "70", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 70)
        return when (getSettings().choice("displayMode", "auto")) {
            "sun" -> sun(brightness)
            "moon" -> moon(brightness)
            else -> if (LocalTime.now().hour in 7..18) sun(brightness) else moon(brightness)
        }
    }

    private fun sun(brightness: Int): GlyphFrame {
        var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), 2.2, 100, thickness = 1.2)
        listOf(6 to 0, 6 to 12, 0 to 6, 12 to 6, 2 to 2, 10 to 2, 2 to 10, 10 to 10).forEach { (x, y) ->
            frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 82)
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun moon(brightness: Int): GlyphFrame {
        val phase = moonPhase()
        var frame = GlyphFrame.empty13(brightness)
        for (y in 1..11) {
            for (x in 1..11) {
                val dx = x - 6
                val dy = y - 6
                if (dx * dx + dy * dy <= 25) {
                    val lit = when {
                        phase < 0.25f -> x >= 6 - (phase * 16).roundToInt()
                        phase < 0.5f -> x >= 2 + ((phase - 0.25f) * 12).roundToInt()
                        phase < 0.75f -> x <= 10 - ((phase - 0.5f) * 12).roundToInt()
                        else -> x <= 6 + ((1f - phase) * 16).roundToInt()
                    }
                    if (lit) frame = GlyphDesignSystem.drawPixelSafe(frame, x, y, 88)
                }
            }
        }
        return GlyphDesignSystem.clean(frame, removeStrays = false)
    }

    private fun moonPhase(): Float {
        val knownNewMoon = LocalDate.of(2000, 1, 6)
        val days = ChronoUnit.DAYS.between(knownNewMoon, LocalDate.now()).toDouble()
        return ((days % 29.53058867) / 29.53058867).toFloat().let { if (it < 0f) it + 1f else it }
    }
}
