package com.pelikan.glyphhub.widget

object GlyphHubWidgetActions {
    const val ACTION_WIDGET_TOGGLE_SELECTED_TOY = "com.pelikan.glyphhub.widget.TOGGLE_SELECTED_TOY"
    const val ACTION_WIDGET_TOGGLE_TOY = "com.pelikan.glyphhub.widget.TOGGLE_TOY"
    const val ACTION_WIDGET_NEXT_TOY = "com.pelikan.glyphhub.widget.NEXT_TOY"
    const val ACTION_WIDGET_PREVIOUS_TOY = "com.pelikan.glyphhub.widget.PREVIOUS_TOY"
    const val ACTION_WIDGET_OPEN_TOY_SETTINGS = "com.pelikan.glyphhub.widget.OPEN_TOY_SETTINGS"
    const val ACTION_WIDGET_OPEN_APP_SETTINGS = "com.pelikan.glyphhub.widget.OPEN_APP_SETTINGS"
    const val ACTION_WIDGET_REFRESH = "com.pelikan.glyphhub.widget.REFRESH"
    const val ACTION_WIDGET_CLOSE_PANEL = "com.pelikan.glyphhub.widget.CLOSE_PANEL"
    const val ACTION_WIDGET_PANEL_PREVIOUS_SETTING = "com.pelikan.glyphhub.widget.PANEL_PREVIOUS_SETTING"
    const val ACTION_WIDGET_PANEL_NEXT_SETTING = "com.pelikan.glyphhub.widget.PANEL_NEXT_SETTING"
    const val ACTION_WIDGET_PANEL_DECREASE = "com.pelikan.glyphhub.widget.PANEL_DECREASE"
    const val ACTION_WIDGET_PANEL_INCREASE = "com.pelikan.glyphhub.widget.PANEL_INCREASE"
    const val ACTION_WIDGET_PANEL_TOGGLE = "com.pelikan.glyphhub.widget.PANEL_TOGGLE"

    const val EXTRA_TOY_ID = "toyId"
    const val EXTRA_ROUTE = "route"
    const val EXTRA_SETTINGS_SCOPE = "settingsScope"
    const val ROUTE_TOY_SETTINGS = "toy_settings"
    const val ROUTE_APP_SETTINGS = "app_settings"
    const val ROUTE_DEBUG = "debug"

    const val SETTINGS_SCOPE_QUICK = "quick"
    const val SETTINGS_SCOPE_FULL = "full"

    const val PANEL_MAIN = "main"
    const val PANEL_TOY = "toy"
    const val PANEL_APP = "app"
}
