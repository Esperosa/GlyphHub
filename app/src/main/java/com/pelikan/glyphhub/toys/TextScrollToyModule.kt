package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class TextScrollToyModule : BaseGlyphToyModule() {
    override val id = "text_scroll"
    override val name = "Text Scroll Toy"
    override val shortName = "TEXT"
    override val description = "Scroll short text through the matrix."
    override val iconAsset = "glyphs/text/text_scroll.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("text", "Text", "Message to scroll.", ToySettingType.Text, "GLYPHHUB"),
            ToySettingDefinition("speed", "Speed", "Scroll speed.", ToySettingType.Int, "2", 1, 10),
            ToySettingDefinition(
                "font",
                "Font",
                "Glyph font.",
                ToySettingType.Choice,
                "compact_3x5",
                options = listOf(
                    ToySettingOption("compact_3x5", "Compact 3x5"),
                    ToySettingOption("digits_4x5", "Readable digits"),
                    ToySettingOption("single_5x7", "Single letter 5x7")
                )
            ),
            ToySettingDefinition(
                "direction",
                "Direction",
                "Scroll direction.",
                ToySettingType.Choice,
                "right_to_left",
                options = listOf(
                    ToySettingOption("right_to_left", "Right to left"),
                    ToySettingOption("left_to_right", "Left to right"),
                    ToySettingOption("top_to_bottom", "Top to bottom"),
                    ToySettingOption("bottom_to_top", "Bottom to top")
                )
            ),
            ToySettingDefinition("spacing", "Spacing", "Pixels between glyphs.", ToySettingType.Int, "1", 0, 3),
            ToySettingDefinition("pauseAtEdgesMs", "Edge pause ms", "Pause when the text reaches an edge.", ToySettingType.Int, "350", 0, 2000),
            ToySettingDefinition("loop", "Loop", "Loop text.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var elapsed = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        elapsed = 0L
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        elapsed += deltaMs
        val speed = getSettings().int("speed", 2).coerceIn(1, 10)
        val brightness = getSettings().int("brightness", 80)
        val text = GlyphTextRenderer.transliterate(getSettings().text("text", "GLYPHHUB").ifBlank { "GLYPH" })
        val font = getSettings().choice("font", "compact_3x5")
        val spacing = getSettings().int("spacing", 1).coerceIn(0, 3)
        val direction = getSettings().choice("direction", "right_to_left")
        val pauseAtEdgesMs = getSettings().int("pauseAtEdgesMs", 350).coerceAtLeast(0)
        val horizontal = direction == "right_to_left" || direction == "left_to_right"
        val contentLength = when (font) {
            "digits_4x5" -> text.filter { it.isDigit() }.take(2).ifEmpty { "0" }.let { digits -> digits.length * 5 - 1 }
            "single_5x7" -> 5
            else -> if (horizontal) GlyphTextRenderer.measureText3x5(text, spacing) else verticalTextHeight(text, spacing)
        }
        val travel = contentLength + GlyphFrame.MATRIX_SIZE + pauseDistance(pauseAtEdgesMs, speed)
        val stepMs = (180L / speed).coerceAtLeast(18L)
        val steps = (elapsed / stepMs).toInt()
        val offset = if (getSettings().bool("loop", true)) {
            loopingOffset(direction, steps, travel, contentLength)
        } else {
            nonLoopingOffset(direction, steps, contentLength)
        }
        return drawText(text, brightness, font, direction, offset, spacing)
    }

    private fun drawText(
        text: String,
        brightness: Int,
        font: String,
        direction: String,
        offset: Int,
        spacing: Int
    ): GlyphFrame =
        when (font) {
            "digits_4x5" -> GlyphTextRenderer.drawReadableDigits(text, brightness).shift(offset, 0)
            "single_5x7" -> GlyphTextRenderer.drawSingleGlyph5x7(text.firstOrNull() ?: ' ', brightness).shift(
                if (direction == "left_to_right") offset - 6 else if (direction == "right_to_left") offset else 0,
                if (direction == "bottom_to_top") offset - 6 else if (direction == "top_to_bottom") offset else 0
            )
            else -> if (direction == "top_to_bottom" || direction == "bottom_to_top") {
                GlyphTextRenderer.drawText3x5Vertical(text, brightness, xOffset = 5, yOffset = offset, spacing = spacing)
            } else {
                GlyphTextRenderer.drawText3x5(text, brightness, yOffset = 4, xOffset = offset, spacing = spacing)
            }
        }

    private fun verticalTextHeight(text: String, spacing: Int): Int {
        val normalized = GlyphTextRenderer.transliterate(text)
        if (normalized.isEmpty()) return 0
        return normalized.length * 5 + (normalized.length - 1) * spacing
    }

    private fun loopingOffset(direction: String, steps: Int, travel: Int, contentLength: Int): Int =
        when (direction) {
            "left_to_right" -> -contentLength + (steps % travel)
            "top_to_bottom" -> -contentLength + (steps % travel)
            "bottom_to_top" -> GlyphFrame.MATRIX_SIZE - (steps % travel)
            else -> GlyphFrame.MATRIX_SIZE - (steps % travel)
        }

    private fun nonLoopingOffset(direction: String, steps: Int, contentLength: Int): Int =
        when (direction) {
            "left_to_right" -> (-contentLength + steps).coerceAtMost(GlyphFrame.MATRIX_SIZE)
            "top_to_bottom" -> (-contentLength + steps).coerceAtMost(GlyphFrame.MATRIX_SIZE)
            "bottom_to_top" -> (GlyphFrame.MATRIX_SIZE - steps).coerceAtLeast(-contentLength)
            else -> (GlyphFrame.MATRIX_SIZE - steps).coerceAtLeast(-contentLength)
        }

    private fun pauseDistance(pauseAtEdgesMs: Int, speed: Int): Int {
        if (pauseAtEdgesMs <= 0) return 0
        val stepMs = (180L / speed).coerceAtLeast(18L)
        return (pauseAtEdgesMs / stepMs).toInt().coerceAtLeast(0)
    }
}
