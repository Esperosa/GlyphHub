package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary as SharedGlyphIconLibrary
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.random.Random

class CoinToyModule : BaseGlyphToyModule() {
    override val id = "coin"
    override val name = "Coin Toy"
    override val shortName = "COIN"
    override val description = "Flip heads or tails with a compact matrix mark."
    override val iconAsset = "glyphs/coin/coin_heads.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("shakeToFlip", "Shake to flip", "Flip when a shake is detected.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("animationEnabled", "Animation", "Animate flips before result.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("animationDurationMs", "Animation ms", "Flip animation length.", ToySettingType.Int, "800", 100, 3000),
            ToySettingDefinition(
                "resultSet",
                "Result set",
                "What pair of outcomes the flip uses.",
                ToySettingType.Choice,
                "heads_tails",
                options = listOf(
                    ToySettingOption("heads_tails", "Heads / Tails"),
                    ToySettingOption("check_x", "Check / X"),
                    ToySettingOption("happy_sad", "Happy / Sad"),
                    ToySettingOption("yes_no", "Yes / No"),
                    ToySettingOption("sun_moon", "Sun / Moon"),
                    ToySettingOption("custom_ab", "Custom A / B")
                )
            ),
            ToySettingDefinition("shakeDebounceMs", "Shake debounce ms", "Ignore repeated triggers during this window.", ToySettingType.Int, "500", 150, 4000),
            ToySettingDefinition("headsLabel", "Heads label", "Short heads label.", ToySettingType.Text, "H"),
            ToySettingDefinition("tailsLabel", "Tails label", "Short tails label.", ToySettingType.Text, "T"),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var heads = true
    private var animationElapsedMs = 0L
    private var lastFlipAt = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        flip()
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Shake -> if (getSettings().bool("shakeToFlip", true)) flip()
            ToySensorEvent.BackTap -> flip()
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val resultSet = getSettings().choice("resultSet", "heads_tails")
        val duration = getSettings().int("animationDurationMs", 800).toLong().coerceAtLeast(1L)
        if (getSettings().bool("animationEnabled", true) && animationElapsedMs < duration) {
            animationElapsedMs += deltaMs
            val phase = ((animationElapsedMs / 80L).toInt() % 8)
            val label = if (phase % 2 == 0) labelsFor(resultSet).first else labelsFor(resultSet).second
            val frame = if (phase in setOf(1, 3, 5)) SharedGlyphIconLibrary.coinEdge(brightness) else coinFrame(label, brightness)
            return frame
        }
        val labels = labelsFor(resultSet)
        val label = if (heads) labels.first else labels.second
        return coinFrame(label, brightness)
    }

    private fun flip() {
        val now = System.currentTimeMillis()
        val debounceMs = getSettings().int("shakeDebounceMs", 500).toLong().coerceAtLeast(150L)
        if (now - lastFlipAt < debounceMs) return
        heads = Random.nextBoolean()
        animationElapsedMs = 0L
        lastFlipAt = now
    }

    private fun labelsFor(resultSet: String): Pair<String, String> =
        when (resultSet) {
            "check_x" -> "OK" to "X"
            "happy_sad" -> "HI" to "LO"
            "yes_no" -> "YES" to "NO"
            "sun_moon" -> "SUN" to "MOON"
            "custom_ab" -> getSettings().text("headsLabel", "A") to getSettings().text("tailsLabel", "B")
            else -> getSettings().text("headsLabel", "H") to getSettings().text("tailsLabel", "T")
        }

    private fun coinFrame(label: String, brightness: Int): GlyphFrame =
        SharedGlyphIconLibrary.coin(label, brightness)
}
