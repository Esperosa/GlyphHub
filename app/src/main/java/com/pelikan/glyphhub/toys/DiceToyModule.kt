package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary as SharedGlyphIconLibrary
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.random.Random

class DiceToyModule : BaseGlyphToyModule() {
    override val id = "dice"
    override val name = "Dice Toy"
    override val shortName = "DICE"
    override val description = "Roll a configurable dice face on the Glyph Matrix."
    override val iconAsset = "glyphs/dice/dice_6.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("sides", "Sides", "Dice side count.", ToySettingType.Int, "6", 2, 20),
            ToySettingDefinition("shakeToRoll", "Shake to roll", "Roll when a shake is detected.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("tapToRoll", "Tap to roll", "Allow tap-triggered rolls.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("animationEnabled", "Animation", "Animate rolls before showing result.", ToySettingType.Boolean, "true"),
            ToySettingDefinition("animationDurationMs", "Animation ms", "Roll animation length.", ToySettingType.Int, "700", 100, 3000),
            ToySettingDefinition("settleDurationMs", "Settle ms", "Final settle hold length after the roll.", ToySettingType.Int, "220", 0, 1200),
            ToySettingDefinition("shakeDebounceMs", "Shake debounce ms", "Ignore repeated triggers during this window.", ToySettingType.Int, "500", 150, 4000),
            ToySettingDefinition("resultDisplayDurationMs", "Result ms", "Result display length.", ToySettingType.Int, "4000", 500, 30000),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var result = 6
    private var animationElapsedMs = 0L
    private var animationValue = 1
    private var settleElapsedMs = 0L
    private var lastRollAt = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        roll()
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Shake -> if (getSettings().bool("shakeToRoll", true)) roll()
            ToySensorEvent.BackTap -> if (getSettings().bool("tapToRoll", true)) roll()
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val sides = getSettings().int("sides", 6).coerceAtLeast(2)
        val duration = getSettings().int("animationDurationMs", 700).toLong().coerceAtLeast(1L)
        if (getSettings().bool("animationEnabled", true) && animationElapsedMs < duration) {
            animationElapsedMs += deltaMs
            animationValue = ((animationElapsedMs / 90L).toInt() % sides) + 1
            return displayFrame(animationValue, sides, brightness)
        }
        val settleDuration = getSettings().int("settleDurationMs", 220).toLong().coerceAtLeast(0L)
        if (settleElapsedMs < settleDuration) {
            settleElapsedMs += deltaMs
            return displayFrame(result, sides, brightness)
        }
        return displayFrame(result, sides, brightness)
    }

    private fun roll() {
        val now = System.currentTimeMillis()
        val debounceMs = getSettings().int("shakeDebounceMs", 500).toLong().coerceAtLeast(150L)
        if (now - lastRollAt < debounceMs) return
        val sides = getSettings().int("sides", 6).coerceAtLeast(2)
        result = Random.nextInt(1, sides + 1)
        animationElapsedMs = 0L
        animationValue = Random.nextInt(1, sides + 1)
        settleElapsedMs = 0L
        lastRollAt = now
    }

    private fun displayFrame(value: Int, sides: Int, brightness: Int): GlyphFrame =
        if (sides <= 6) {
            SharedGlyphIconLibrary.dice(value.coerceIn(1, 6), brightness)
        } else {
            SharedGlyphIconLibrary.text(value.toString().take(3), brightness)
        }
}
