package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class PixelArtToyModule : BaseGlyphToyModule() {
    override val id = "pixel_art"
    override val name = "Pixel Art Toy"
    override val shortName = "PIX"
    override val description = "Static or animated 13x13 glyph art."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "selectedGlyphAsset",
                "Glyph asset",
                "Built-in icon or the custom editor output.",
                ToySettingType.Choice,
                "heart",
                options = GlyphIconLibrary.settingOptions(),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition(
                "customGlyphRows",
                "Custom rows",
                "13 encoded rows edited in the app.",
                ToySettingType.Text,
                "",
                visibleByDefault = false
            ),
            ToySettingDefinition(
                "animationMode",
                "Animation",
                "Playback mode.",
                ToySettingType.Choice,
                "once",
                options = listOf(
                    com.pelikan.glyphhub.settings.ToySettingOption("once", "Once"),
                    com.pelikan.glyphhub.settings.ToySettingOption("loop", "Loop"),
                    com.pelikan.glyphhub.settings.ToySettingOption("pingpong", "Pingpong")
                )
            ),
            ToySettingDefinition("frameDurationMs", "Frame ms", "Frame duration.", ToySettingType.Int, "250", 40, 3000),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var elapsedMs = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        elapsedMs = 0L
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsedMs += deltaMs
        val brightness = getSettings().int("brightness", 80)
        val selectedAsset = getSettings().choice("selectedGlyphAsset", "heart")
        val customRows = getSettings().text("customGlyphRows", "")
        val frameDurationMs = getSettings().int("frameDurationMs", 250).toLong().coerceAtLeast(40L)
        return when (getSettings().choice("animationMode", "once")) {
            "loop",
            "pingpong" -> GlyphIconLibrary.animatedFrame(selectedAsset, customRows, brightness, elapsedMs, frameDurationMs)
            else -> GlyphIconLibrary.frame(selectedAsset, customRows, brightness)
        }
    }
}
