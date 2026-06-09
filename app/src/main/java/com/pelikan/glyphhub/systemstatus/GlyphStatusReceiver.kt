package com.pelikan.glyphhub.systemstatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.pelikan.glyphhub.glyph.GlyphHubToyService
import com.pelikan.glyphhub.settings.SettingsRepository

class GlyphStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SettingsRepository.appendLog(context, "status receiver action=${intent.action}")
        val event = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> GlyphStatusEvent(GlyphStatusEventType.ChargingConnected)
            Intent.ACTION_POWER_DISCONNECTED -> GlyphStatusEvent(GlyphStatusEventType.ChargingDisconnected)
            VOLUME_CHANGED_ACTION -> volumeEvent(intent)
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                GlyphHubToyService.requestSync(context)
                null
            }
            else -> null
        } ?: return
        GlyphHubToyService.requestStatusEvent(context, event)
    }

    private fun volumeEvent(intent: Intent): GlyphStatusEvent? {
        val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
        if (stream != AudioManager.STREAM_MUSIC && stream != AudioManager.STREAM_NOTIFICATION) return null
        val value = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
        val max = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_MAX", 15).takeIf { it > 0 } ?: 15
        if (value < 0) return null
        return GlyphStatusEvent(GlyphStatusEventType.Volume, value = value * 100 / max)
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
