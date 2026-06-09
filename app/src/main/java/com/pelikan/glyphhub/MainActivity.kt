package com.pelikan.glyphhub

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pelikan.glyphhub.glyph.GlyphHubToyService
import com.pelikan.glyphhub.schoolonline.SkolaOnlineRepository
import com.pelikan.glyphhub.schoolonline.SkolaOnlineServer
import com.pelikan.glyphhub.schoolonline.SkolaOnlineSyncResult
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.ui.screens.AppSettingsScreen
import com.pelikan.glyphhub.ui.screens.DebugScreen
import com.pelikan.glyphhub.ui.screens.HomeScreen
import com.pelikan.glyphhub.ui.screens.ToySettingsScreen
import com.pelikan.glyphhub.ui.theme.GlyphHubTheme
import com.pelikan.glyphhub.web.GlyphWebPortalManager
import com.pelikan.glyphhub.widget.GlyphHubWidgetActions
import com.pelikan.glyphhub.widget.GlyphHubWidgetRenderer
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val routeRequest = mutableStateOf(RouteRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDebugCommand(intent)
        routeRequest.value = RouteRequest.from(intent)
        setContent {
            GlyphHubTheme {
                GlyphHubApp(routeRequest = routeRequest.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDebugCommand(intent)
        routeRequest.value = RouteRequest.from(intent)
    }

    private fun handleDebugCommand(intent: Intent?) {
        if (!isDebuggable() || intent?.action != ACTION_DEBUG_COMMAND) return
        val command = intent.getStringExtra(EXTRA_DEBUG_COMMAND).orEmpty()
        val toyId = intent.getStringExtra(GlyphHubWidgetActions.EXTRA_TOY_ID)
            ?: intent.getStringExtra(EXTRA_DEBUG_TOY_ID)
            ?: SettingsRepository.appSettings(this).selectedToyId
        when (command) {
            COMMAND_ACTIVATE -> {
                SettingsRepository.activateToy(this, toyId)
                GlyphHubToyService.requestActivate(this, toyId)
            }
            COMMAND_DEACTIVATE -> {
                GlyphHubToyService.requestDeactivate(this, toyId)
                SettingsRepository.deactivateCurrentToy(this)
            }
            COMMAND_CLEAR -> GlyphHubToyService.requestClear(this)
            COMMAND_SYNC -> GlyphHubToyService.requestSync(this)
            COMMAND_TEST_ACTIVATION -> GlyphHubToyService.requestTestActivation(this)
            COMMAND_TEST_DEACTIVATION -> GlyphHubToyService.requestTestDeactivation(this)
            COMMAND_REFRESH_WIDGET -> GlyphHubWidgetRenderer.updateAll(this)
            COMMAND_SYNC_SCHOOL_ONLINE -> syncSchoolOnlineDebug(intent)
        }
        SettingsRepository.appendLog(this, "debug command=$command toy=$toyId")
    }

    private fun syncSchoolOnlineDebug(intent: Intent) {
        val username = intent.getStringExtra(EXTRA_SCHOOL_ONLINE_USERNAME).orEmpty()
        val password = intent.getStringExtra(EXTRA_SCHOOL_ONLINE_PASSWORD).orEmpty()
        val server = SkolaOnlineServer.fromId(intent.getStringExtra(EXTRA_SCHOOL_ONLINE_SERVER))
        if (username.isBlank() || password.isBlank()) {
            SettingsRepository.appendLog(this, "school online debug sync skipped missing credentials")
            return
        }
        val appContext = applicationContext
        Thread({
            val repository = SkolaOnlineRepository(appContext)
            repository.saveAccount(
                enabled = true,
                server = server,
                username = username,
                password = password
            )
            val result = repository.refresh()
            val resultName = when (result) {
                SkolaOnlineSyncResult.Disabled -> "disabled"
                is SkolaOnlineSyncResult.Failure -> "failure:${result.reason}"
                is SkolaOnlineSyncResult.Success -> "success:${result.snapshot.lessons.size}"
            }
            SettingsRepository.appendLog(appContext, "school online debug sync result=$resultName")
        }, "GlyphHubSchoolOnlineDebug").apply {
            isDaemon = true
            start()
        }
    }

    private fun isDebuggable(): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    companion object {
        const val ACTION_DEBUG_COMMAND = "com.pelikan.glyphhub.DEBUG_COMMAND"
        const val EXTRA_DEBUG_COMMAND = "command"
        const val EXTRA_DEBUG_TOY_ID = "toyId"
        const val COMMAND_ACTIVATE = "activate"
        const val COMMAND_DEACTIVATE = "deactivate"
        const val COMMAND_CLEAR = "clear"
        const val COMMAND_SYNC = "sync"
        const val COMMAND_TEST_ACTIVATION = "test_activation"
        const val COMMAND_TEST_DEACTIVATION = "test_deactivation"
        const val COMMAND_REFRESH_WIDGET = "refresh_widget"
        const val COMMAND_SYNC_SCHOOL_ONLINE = "sync_school_online"
        const val EXTRA_SCHOOL_ONLINE_USERNAME = "schoolOnlineUsername"
        const val EXTRA_SCHOOL_ONLINE_PASSWORD = "schoolOnlinePassword"
        const val EXTRA_SCHOOL_ONLINE_SERVER = "schoolOnlineServer"
    }
}

private enum class Route(val id: String) {
    Home("home"),
    ToySettings(GlyphHubWidgetActions.ROUTE_TOY_SETTINGS),
    AppSettings(GlyphHubWidgetActions.ROUTE_APP_SETTINGS),
    Debug(GlyphHubWidgetActions.ROUTE_DEBUG)
}

private data class RouteRequest(
    val routeId: String = Route.Home.id,
    val toyId: String? = null,
    val settingsScope: String = GlyphHubWidgetActions.SETTINGS_SCOPE_FULL,
    val nonce: Long = 0L
) {
    companion object {
        fun from(intent: Intent?): RouteRequest =
            RouteRequest(
                routeId = intent?.getStringExtra(GlyphHubWidgetActions.EXTRA_ROUTE) ?: Route.Home.id,
                toyId = intent?.getStringExtra(GlyphHubWidgetActions.EXTRA_TOY_ID),
                settingsScope = intent?.getStringExtra(GlyphHubWidgetActions.EXTRA_SETTINGS_SCOPE)
                    ?: GlyphHubWidgetActions.SETTINGS_SCOPE_FULL,
                nonce = System.nanoTime()
            )
    }
}

@Composable
private fun GlyphHubApp(routeRequest: RouteRequest) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var route by remember { mutableStateOf(Route.entries.firstOrNull { it.id == routeRequest.routeId } ?: Route.Home) }
    var toySettingsId by remember {
        mutableStateOf(routeRequest.toyId ?: SettingsRepository.appSettings(context).selectedToyId)
    }
    var toySettingsScope by remember { mutableStateOf(routeRequest.settingsScope) }
    var refreshToken by remember { mutableIntStateOf(0) }
    val appSettings = remember(refreshToken) { SettingsRepository.appSettings(context) }

    LaunchedEffect(routeRequest.nonce) {
        route = Route.entries.firstOrNull { it.id == routeRequest.routeId } ?: Route.Home
        toySettingsId = routeRequest.toyId ?: SettingsRepository.appSettings(context).selectedToyId
        toySettingsScope = routeRequest.settingsScope
    }

    fun refresh() {
        GlyphHubWidgetRenderer.updateAll(context)
        refreshToken += 1
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        SettingsRepository.updateAppSettings(context, transform)
        GlyphHubToyService.requestSync(context)
        refresh()
    }

    fun toggleToy(toyId: String) {
        val settings = SettingsRepository.appSettings(context)
        if (settings.activeToyId == toyId) {
            GlyphHubToyService.requestDeactivate(context, toyId)
            SettingsRepository.deactivateCurrentToy(context)
        } else {
            SettingsRepository.activateToy(context, toyId)
            GlyphHubToyService.requestActivate(context, toyId)
        }
        refresh()
    }

    LaunchedEffect(appSettings.webPortalEnabled, appSettings.webPortalPort, appSettings.webPortalToken) {
        if (appSettings.webPortalEnabled) {
            val token = appSettings.webPortalToken.ifBlank { UUID.randomUUID().toString().take(8) }
            if (token != appSettings.webPortalToken) {
                updateSettings { it.copy(webPortalToken = token) }
            } else {
                GlyphWebPortalManager.start(context, appSettings.webPortalPort, token)
            }
        } else if (GlyphWebPortalManager.running()) {
            GlyphWebPortalManager.stop(context)
        }
    }

    when (route) {
        Route.Home -> HomeScreen(
            modules = ToyRegistry.visibleModules(appSettings.showExperimentalToys),
            appSettings = appSettings,
            settingsRepository = SettingsRepository,
            onToggleToy = ::toggleToy,
            onSelectPrevious = {
                SettingsRepository.selectPreviousToy(context)
                GlyphHubToyService.requestSync(context)
                refresh()
            },
            onSelectNext = {
                SettingsRepository.selectNextToy(context)
                GlyphHubToyService.requestSync(context)
                refresh()
            },
            onOpenToySettings = {
                toySettingsId = it
                toySettingsScope = GlyphHubWidgetActions.SETTINGS_SCOPE_FULL
                route = Route.ToySettings
            },
            onOpenAppSettings = { route = Route.AppSettings },
            onOpenDebug = { route = Route.Debug },
            onUpdateSettings = ::updateSettings
        )
        Route.ToySettings -> ToySettingsScreen(
            module = ToyRegistry.byId(toySettingsId) ?: ToyRegistry.modules.first(),
            settingsRepository = SettingsRepository,
            isActive = appSettings.activeToyId == toySettingsId,
            quickOnly = toySettingsScope == GlyphHubWidgetActions.SETTINGS_SCOPE_QUICK,
            debugMode = appSettings.debugMode,
            onBack = { route = Route.Home },
            onSettingsChanged = {
                GlyphHubToyService.requestSync(context)
                refresh()
            }
        )
        Route.AppSettings -> AppSettingsScreen(
            appSettings = appSettings,
            modules = ToyRegistry.modules,
            onBack = { route = Route.Home },
            onUpdateSettings = ::updateSettings
        )
        Route.Debug -> DebugScreen(
            appSettings = appSettings,
            settingsRepository = SettingsRepository,
            onBack = { route = Route.Home },
            onRefresh = ::refresh
        )
    }
}
