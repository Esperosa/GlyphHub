package com.pelikan.glyphhub.toys

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.pelikan.glyphhub.glyph.GlyphDesignSystem
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingOption
import com.pelikan.glyphhub.settings.ToySettingScope
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema

class NetworkStatusToyModule : BaseGlyphToyModule() {
    override val id = "network_status"
    override val name = "Network Status"
    override val shortName = "NET"
    override val description = "Shows Wi-Fi/network connectivity without exposing unreadable IP text on Matrix."
    override val iconAsset = "network_status"
    override val supportsAod = true
    override val supportsSensors = false
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition(
                "displayMode",
                "Display",
                "Connectivity visual mode.",
                ToySettingType.Choice,
                "icon",
                options = listOf(ToySettingOption("icon", "Icon"), ToySettingOption("meter", "Meter")),
                scope = ToySettingScope.QUICK
            ),
            ToySettingDefinition("brightness", "Visual intensity", "Relative frame intensity.", ToySettingType.Int, "70", 0, 100, scope = ToySettingScope.ADVANCED, requiresDebugMode = true),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var appContext: Context? = null

    override fun onActivate(context: ToyRuntimeContext) {
        appContext = context.androidContext.applicationContext
    }

    override fun onDeactivate() {
        appContext = null
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 70)
        val state = networkState()
        if (!state.connected) return drawCenteredText3x5("OFF", brightness, yOffset = 4)
        if (getSettings().choice("displayMode", "icon") == "meter") {
            var frame = GlyphDesignSystem.drawCircleApprox(GlyphFrame.empty13(brightness), GlyphDesignSystem.fullRingRadius, 30, thickness = 0.5)
            frame = GlyphDesignSystem.drawProgressRing(frame, if (state.wifi) 1f else 0.55f, 100)
            return frame.overlay(drawCenteredText3x5(if (state.wifi) "WIFI" else "NET", brightness, yOffset = 4))
        }
        return drawCenteredText3x5(if (state.wifi) "WIFI" else "NET", brightness, yOffset = 4)
    }

    private fun networkState(): NetworkState {
        val context = appContext ?: return NetworkState(false, false)
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return NetworkState(false, false)
        return NetworkState(
            connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        )
    }

    private data class NetworkState(val connected: Boolean, val wifi: Boolean)
}
