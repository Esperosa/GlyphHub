package com.pelikan.glyphhub.toys

import android.util.Log
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphTextRenderer
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettings
import com.pelikan.glyphhub.settings.ToySettingsSchema

interface GlyphToyModule {
    val id: String
    val name: String
    val shortName: String
    val description: String
    val iconAsset: String
    val supportsAod: Boolean
    val supportsSensors: Boolean
    val settingsSchema: ToySettingsSchema

    fun onActivate(context: ToyRuntimeContext)
    fun onDeactivate()
    fun onTick(deltaMs: Long): GlyphFrame
    fun previewFrame(): GlyphFrame
    fun onSensorEvent(event: ToySensorEvent)
    fun getSettings(): ToySettings
    fun updateSettings(settings: ToySettings)
}

abstract class BaseGlyphToyModule : GlyphToyModule {
    private var settings: ToySettings? = null

    override fun onActivate(context: ToyRuntimeContext) = Unit
    override fun onDeactivate() = Unit
    override fun previewFrame(): GlyphFrame {
        val preview = runCatching { ToyPreviewFrames.preview(this, getSettings()) }
            .onFailure { Log.w("GlyphHub", "Preview generator failed for $id: ${it.message}", it) }
            .getOrNull()
        if (preview != null && preview.litPixelCount() > 0) return preview

        val tickFrame = runCatching { onTick(0L) }
            .onFailure { Log.w("GlyphHub", "Preview tick fallback failed for $id: ${it.message}", it) }
            .getOrNull()
        if (tickFrame != null && tickFrame.litPixelCount() > 0) return tickFrame

        return ToyPreviewFrames.fallbackPreview(shortName, brightness = 90)
    }
    override fun onSensorEvent(event: ToySensorEvent) = Unit
    override fun getSettings(): ToySettings = settings ?: ToySettings.defaultFor(settingsSchema)
    override fun updateSettings(settings: ToySettings) {
        this.settings = settings
    }
}

internal fun frameFromRows(rows: List<String>, brightness: Int): GlyphFrame =
    GlyphFrame.fromBinaryRows(rows, brightness.coerceIn(0, 100))

internal fun activationAnimationOverrideDefinition(): ToySettingDefinition =
    ToySettingDefinition(
        "activationAnimationOverride",
        "Activation override",
        "Optional transition id. Leave blank to use the global activation animation.",
        ToySettingType.Text,
        ""
    )

internal fun deactivationAnimationOverrideDefinition(): ToySettingDefinition =
    ToySettingDefinition(
        "deactivationAnimationOverride",
        "Deactivation override",
        "Optional transition id. Leave blank to use the global deactivation animation.",
        ToySettingType.Text,
        ""
    )

internal fun frameFromPoints(points: Set<Pair<Int, Int>>, brightness: Int): GlyphFrame {
    var frame = GlyphFrame.empty13(brightness)
    points.forEach { (x, y) -> frame = frame.withPixel(x, y, true) }
    return frame
}

internal fun drawText3x5(text: String, brightness: Int, yOffset: Int = 4, xOffset: Int = 0): GlyphFrame {
    return GlyphTextRenderer.drawText3x5(text, brightness, yOffset = yOffset, xOffset = xOffset)
}

internal fun textWidth3x5(text: String): Int {
    return GlyphTextRenderer.measureText3x5(text)
}

internal fun drawCenteredText3x5(text: String, brightness: Int, yOffset: Int = 4): GlyphFrame {
    return GlyphTextRenderer.drawCenteredText3x5(text, brightness, yOffset = yOffset)
}
