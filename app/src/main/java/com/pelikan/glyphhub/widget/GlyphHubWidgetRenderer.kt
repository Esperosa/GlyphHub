package com.pelikan.glyphhub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.pelikan.glyphhub.MainActivity
import com.pelikan.glyphhub.R
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphFramePreview
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettings
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToyRegistry

object GlyphHubWidgetRenderer {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, GlyphHubWidgetProvider::class.java))
        ids.forEach { id ->
            manager.updateAppWidget(id, render(context, id))
        }
    }

    fun render(context: Context, appWidgetId: Int): RemoteViews {
        val state = SettingsRepository.widgetState(context)
        val appSettings = SettingsRepository.appSettings(context)
        val module = ToyRegistry.byId(state.selectedToyId) ?: ToyRegistry.modules.first()
        val views = RemoteViews(context.packageName, R.layout.glyphhub_widget)
        val active = state.isCurrentToyActive

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (active) R.drawable.widget_card_active else R.drawable.widget_card_inactive
        )
        if (state.widgetPanelMode == GlyphHubWidgetActions.PANEL_TOY ||
            state.widgetPanelMode == GlyphHubWidgetActions.PANEL_APP
        ) {
            views.setViewVisibility(R.id.widget_main_content, View.GONE)
            views.setViewVisibility(R.id.widget_panel_content, View.VISIBLE)
            bindPanel(
                context = context,
                views = views,
                appSettings = appSettings,
                mode = state.widgetPanelMode,
                settingIndex = state.widgetPanelSettingIndex
            )
            bindPanelActions(context, views, appWidgetId)
        } else {
            views.setViewVisibility(R.id.widget_main_content, View.VISIBLE)
            views.setViewVisibility(R.id.widget_panel_content, View.GONE)
            views.setTextViewText(R.id.widget_toy_name, module.shortName)
            views.setTextColor(R.id.widget_toy_name, if (active) Color.rgb(230, 0, 18) else Color.WHITE)
            bindPreview(context, views, module, active)
            bindMainActions(context, views, appWidgetId)
        }
        return views
    }

    private fun bindPreview(context: Context, views: RemoteViews, module: GlyphToyModule, active: Boolean) {
        module.updateSettings(SettingsRepository.getToySettings(context, module.id, module.settingsSchema))
        val frame = runCatching { module.previewFrame() }.getOrDefault(GlyphFrame.empty13())
        views.setImageViewBitmap(
            R.id.widget_preview,
            GlyphFramePreview.ledOnlyBitmap(frame, sizePx = 144, active = active)
        )
    }

    private fun bindMainActions(context: Context, views: RemoteViews, appWidgetId: Int) {
        val previous = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_PREVIOUS_TOY)
        val next = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_NEXT_TOY)
        val toySettings = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_OPEN_TOY_SETTINGS)
        val appSettings = activity(context, appWidgetId, GlyphHubWidgetActions.ROUTE_APP_SETTINGS)
        val toggle = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_TOGGLE_SELECTED_TOY)
        views.setOnClickPendingIntent(R.id.widget_top_hint, previous)
        views.setOnClickPendingIntent(R.id.widget_bottom_hint, next)
        views.setOnClickPendingIntent(R.id.widget_left_hint, toySettings)
        views.setOnClickPendingIntent(R.id.widget_right_hint, appSettings)
        views.setOnClickPendingIntent(R.id.widget_preview, toggle)
        views.setOnClickPendingIntent(R.id.widget_toy_name, toggle)
        views.setOnClickPendingIntent(R.id.widget_center_zone, toggle)
    }

    private fun bindPanelActions(context: Context, views: RemoteViews, appWidgetId: Int) {
        val close = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_CLOSE_PANEL)
        val previous = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_PANEL_PREVIOUS_SETTING)
        val next = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_PANEL_NEXT_SETTING)
        val decrease = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_PANEL_DECREASE)
        val increase = broadcast(context, appWidgetId, GlyphHubWidgetActions.ACTION_WIDGET_PANEL_INCREASE)
        views.setOnClickPendingIntent(R.id.widget_panel_content, close)
        views.setOnClickPendingIntent(R.id.widget_panel_scope, close)
        views.setOnClickPendingIntent(R.id.widget_panel_title, close)
        views.setOnClickPendingIntent(R.id.widget_panel_value, close)
        views.setOnClickPendingIntent(R.id.widget_panel_close, close)
        views.setOnClickPendingIntent(R.id.widget_panel_top_hint, previous)
        views.setOnClickPendingIntent(R.id.widget_panel_bottom_hint, next)
        views.setOnClickPendingIntent(R.id.widget_panel_left_hint, decrease)
        views.setOnClickPendingIntent(R.id.widget_panel_right_hint, increase)
    }

    private fun bindPanel(
        context: Context,
        views: RemoteViews,
        appSettings: AppSettings,
        mode: String,
        settingIndex: Int
    ) {
        val model = if (mode == GlyphHubWidgetActions.PANEL_APP) {
            appPanelModel(appSettings, settingIndex)
        } else {
            toyPanelModel(context, appSettings, settingIndex)
        }
        views.setTextViewText(R.id.widget_panel_scope, model.scope)
        views.setTextViewText(R.id.widget_panel_title, model.title)
        views.setTextViewText(R.id.widget_panel_value, model.value)
        views.setTextColor(R.id.widget_panel_value, if (model.active) Color.rgb(230, 0, 18) else Color.WHITE)
        views.setInt(
            R.id.widget_panel_content,
            "setBackgroundResource",
            if (model.active) R.drawable.widget_card_active else R.drawable.widget_card_inactive
        )
    }

    private fun appPanelModel(appSettings: AppSettings, settingIndex: Int): PanelModel {
        val settings = listOf(
            PanelModel("APP", "AOD", if (appSettings.aodEnabled) "ON" else "OFF", appSettings.aodEnabled),
            PanelModel("APP", "SENSORS", if (appSettings.sensorsEnabled) "ON" else "OFF", appSettings.sensorsEnabled),
            PanelModel("APP", "RETURN", if (appSettings.autoReturnToDefault) "ON" else "OFF", appSettings.autoReturnToDefault),
            PanelModel("APP", "EXPER", if (appSettings.showExperimentalToys) "ON" else "OFF", appSettings.showExperimentalToys)
        )
        return settings[settingIndex.floorMod(settings.size)]
    }

    private fun toyPanelModel(context: Context, appSettings: AppSettings, settingIndex: Int): PanelModel {
        val module = ToyRegistry.byId(appSettings.selectedToyId) ?: ToyRegistry.modules.first()
        val settings = SettingsRepository.getToySettings(context, module.id, module.settingsSchema)
        val definitions = module.settingsSchema
            .quickEntries(appSettings.debugMode)
            .filter { it.type != ToySettingType.Text }
            .take(4)
        if (definitions.isEmpty()) return PanelModel(module.shortName, "NO SET", "--", false)
        val definition = definitions.getOrNull(settingIndex.floorMod(definitions.size))
            ?: return PanelModel(module.shortName, "NO SET", "--", false)
        val value = settings.values[definition.key] ?: definition.defaultValue
        return PanelModel(
            scope = module.shortName,
            title = definition.title.panelTitle(),
            value = definition.displayValue(settings),
            active = definition.isActive(value)
        )
    }

    private fun broadcast(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, GlyphHubWidgetReceiver::class.java)
            .setAction(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 31 + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activity(context: Context, appWidgetId: Int, route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(GlyphHubWidgetActions.EXTRA_ROUTE, route)
        return PendingIntent.getActivity(
            context,
            appWidgetId * 47 + route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class PanelModel(
        val scope: String,
        val title: String,
        val value: String,
        val active: Boolean
    )

    private fun ToySettingDefinition.displayValue(settings: ToySettings): String {
        val value = settings.values[key] ?: defaultValue
        return when (type) {
            ToySettingType.Boolean -> if (value.toBooleanStrictOrNull() == true) "ON" else "OFF"
            ToySettingType.Int -> value
            ToySettingType.Choice -> options.firstOrNull { it.id == value }?.label?.uppercase()?.take(8) ?: value.uppercase().take(8)
            ToySettingType.Text -> value.uppercase().take(8)
        }
    }

    private fun ToySettingDefinition.isActive(value: String): Boolean =
        when (type) {
            ToySettingType.Boolean -> value.toBooleanStrictOrNull() == true
            ToySettingType.Int -> (value.toIntOrNull() ?: 0) > 0
            ToySettingType.Choice -> value != defaultValue
            ToySettingType.Text -> value.isNotBlank()
        }

    private fun String.panelTitle(): String =
        uppercase()
            .split(" ")
            .firstOrNull { it.isNotBlank() }
            ?.take(8)
            ?: uppercase().take(8)

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
