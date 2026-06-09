package com.pelikan.glyphhub.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pelikan.glyphhub.MainActivity
import com.pelikan.glyphhub.glyph.GlyphHubToyService
import com.pelikan.glyphhub.settings.SettingsRepository

class GlyphHubWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SettingsRepository.appendLog(context, "widget action=${intent.action}")
        when (intent.action) {
            GlyphHubWidgetActions.ACTION_WIDGET_TOGGLE_SELECTED_TOY -> toggleSelectedToy(context, null)
            GlyphHubWidgetActions.ACTION_WIDGET_TOGGLE_TOY -> toggleSelectedToy(
                context,
                intent.getStringExtra(GlyphHubWidgetActions.EXTRA_TOY_ID)
            )
            GlyphHubWidgetActions.ACTION_WIDGET_NEXT_TOY -> {
                SettingsRepository.selectNextToy(context)
                GlyphHubToyService.requestSync(context)
            }
            GlyphHubWidgetActions.ACTION_WIDGET_PREVIOUS_TOY -> {
                SettingsRepository.selectPreviousToy(context)
                GlyphHubToyService.requestSync(context)
            }
            GlyphHubWidgetActions.ACTION_WIDGET_OPEN_TOY_SETTINGS -> openToySettings(context)
            GlyphHubWidgetActions.ACTION_WIDGET_OPEN_APP_SETTINGS -> openAppSettings(context)
            GlyphHubWidgetActions.ACTION_WIDGET_REFRESH -> GlyphHubToyService.requestSync(context)
            GlyphHubWidgetActions.ACTION_WIDGET_CLOSE_PANEL -> SettingsRepository.closeWidgetPanel(context)
            GlyphHubWidgetActions.ACTION_WIDGET_PANEL_PREVIOUS_SETTING -> SettingsRepository.selectWidgetPanelSetting(context, -1)
            GlyphHubWidgetActions.ACTION_WIDGET_PANEL_NEXT_SETTING -> SettingsRepository.selectWidgetPanelSetting(context, 1)
            GlyphHubWidgetActions.ACTION_WIDGET_PANEL_DECREASE -> {
                SettingsRepository.adjustWidgetPanelSetting(context, -1)
                GlyphHubToyService.requestSync(context)
            }
            GlyphHubWidgetActions.ACTION_WIDGET_PANEL_INCREASE -> {
                SettingsRepository.adjustWidgetPanelSetting(context, 1)
                GlyphHubToyService.requestSync(context)
            }
            GlyphHubWidgetActions.ACTION_WIDGET_PANEL_TOGGLE -> {
                SettingsRepository.toggleWidgetPanelSetting(context)
                GlyphHubToyService.requestSync(context)
            }
        }
        GlyphHubWidgetRenderer.updateAll(context)
    }

    private fun toggleSelectedToy(context: Context, explicitToyId: String?) {
        val settings = explicitToyId
            ?.takeIf { it.isNotBlank() }
            ?.let { SettingsRepository.selectToy(context, it) }
            ?: SettingsRepository.appSettings(context)
        if (settings.activeToyId == settings.selectedToyId) {
            GlyphHubToyService.requestDeactivate(context, settings.selectedToyId)
            SettingsRepository.deactivateCurrentToy(context)
        } else {
            SettingsRepository.activateToy(context, settings.selectedToyId)
            GlyphHubToyService.requestActivate(context, settings.selectedToyId)
        }
        haptic(context)
    }

    private fun openToySettings(context: Context) {
        SettingsRepository.openWidgetPanel(context, GlyphHubWidgetActions.PANEL_TOY)
    }

    private fun openAppSettings(context: Context) {
        val activityIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(GlyphHubWidgetActions.EXTRA_ROUTE, GlyphHubWidgetActions.ROUTE_APP_SETTINGS)
        context.startActivity(activityIntent)
    }

    private fun haptic(context: Context) {
        if (!SettingsRepository.appSettings(context).widgetHapticsEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }
}
