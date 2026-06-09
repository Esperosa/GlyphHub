package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import java.time.LocalDate

class IdleDefaultToyModule : BaseGlyphToyModule() {
    override val id = "idle_default"
    override val name = "Idle / Default Display Toy"
    override val shortName = "IDLE"
    override val description = "Default AOD-friendly fallback display."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "selectedDefaultMode",
                "Default mode",
                "Fallback display mode.",
                ToySettingType.Choice,
                "off",
                options = listOf(
                    ToySettingOption("off", "Off"),
                    ToySettingOption("clock", "Clock"),
                    ToySettingOption("battery", "Battery"),
                    ToySettingOption("date", "Date"),
                    ToySettingOption("custom_glyph", "Custom glyph"),
                    ToySettingOption("last_active", "Last active"),
                    ToySettingOption("selected_toy", "Selected toy")
                )
            ),
            ToySettingDefinition("selectedDefaultToyId", "Default toy", "Fallback Toy id.", ToySettingType.Text, ""),
            ToySettingDefinition("idleAnimationEnabled", "Idle animation", "Animate idle display.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("idleBrightness", "Brightness", "Idle brightness.", ToySettingType.Int, "35", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("idleBrightness", 35)
        return when (getSettings().choice("selectedDefaultMode", "off")) {
            "date" -> {
                val day = LocalDate.now().dayOfMonth.toString().padStart(2, '0')
                drawText3x5(day, brightness, yOffset = 4, xOffset = 2)
            }
            "clock" -> ClockToyModule().also {
                it.updateSettings(it.getSettings().withValue("brightness", brightness.toString()))
            }.onTick(deltaMs)
            "battery" -> BatteryToyModule().also {
                it.updateSettings(it.getSettings().withValue("brightness", brightness.toString()))
            }.onTick(deltaMs)
            "custom_glyph" -> frameFromRows(
                listOf(
                    "0000000000000",
                    "0000011100000",
                    "0000100010000",
                    "0001000001000",
                    "0010001000100",
                    "0010011100100",
                    "0010001000100",
                    "0001000001000",
                    "0000100010000",
                    "0000011100000",
                    "0000000000000",
                    "0000000000000",
                    "0000000000000"
                ),
                brightness
            )
            else -> GlyphFrame.empty13(0)
        }
    }
}
