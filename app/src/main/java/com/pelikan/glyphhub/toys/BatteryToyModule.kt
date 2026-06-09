package com.pelikan.glyphhub.toys

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary as SharedGlyphIconLibrary
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class BatteryToyModule : BaseGlyphToyModule() {
    override val id = "battery"
    override val name = "Battery Toy"
    override val shortName = "BATT"
    override val description = "Battery level meter with low-level warning."
    override val iconAsset = "glyphs/battery/battery_icon.json"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("lowBatteryWarning", "Low warning", "Blink the lowest filled battery line under 15%.", ToySettingType.Boolean, "true"),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var context: Context? = null
    private var animationElapsedMs = 0L

    override fun onActivate(context: ToyRuntimeContext) {
        this.context = context.androidContext.applicationContext
    }

    override fun onDeactivate() {
        context = null
        animationElapsedMs = 0L
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        animationElapsedMs += deltaMs
        val batteryState = batteryState()
        val level = batteryState.level.coerceIn(0, 100)
        return SharedGlyphIconLibrary.battery(
            level = level,
            brightness = 100,
            charging = false,
            elapsedMs = animationElapsedMs,
            lowWarning = getSettings().bool("lowBatteryWarning", true),
            animateCharging = false
        )
    }

    private fun batteryState(): BatteryState {
        val batteryIntent = context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
            val percent = if (level >= 0) (level * 100 / scale).coerceIn(0, 100) else 75
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
            return BatteryState(level = percent, isCharging = charging)
        }
        val manager = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return BatteryState(level = 75, isCharging = false)
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it >= 0 } ?: 75
        return BatteryState(level = level, isCharging = manager.isCharging)
    }

    private data class BatteryState(val level: Int, val isCharging: Boolean)
}
