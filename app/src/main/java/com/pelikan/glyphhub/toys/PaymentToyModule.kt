package com.pelikan.glyphhub.toys

import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class PaymentToyModule : BaseGlyphToyModule() {
    override val id = "payment_visual"
    override val name = "Payment Visual"
    override val shortName = "PAY"
    override val description = "Manual wallet/payment-active visual. It does not detect confirmed payment success."
    override val iconAsset = "payment_visual"
    override val supportsAod = false
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "mode",
                "Mode",
                "Manual payment visual mode.",
                ToySettingType.Choice,
                "ready",
                options = listOf(ToySettingOption("ready", "Ready"), ToySettingOption("success", "Manual success")),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "80", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    override fun onSensorEvent(event: ToySensorEvent) {
        if (event == ToySensorEvent.BackTap) {
            updateSettings(getSettings().withValue("mode", if (getSettings().choice("mode", "ready") == "success") "ready" else "success"))
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        return if (getSettings().choice("mode", "ready") == "success") {
            frameFromPoints(setOf(2 to 7, 3 to 8, 4 to 9, 5 to 8, 6 to 7, 7 to 6, 8 to 5, 9 to 4, 10 to 3), brightness)
        } else {
            var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 58, thickness = 0.55)
            frame.overlay(drawCenteredText3x5("PAY", brightness, yOffset = 4))
        }
    }
}
