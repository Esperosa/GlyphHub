package com.pelikan.glyphhub.glyph

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.PowerManager
import android.os.SystemClock
import com.pelikan.glyphhub.R
import com.pelikan.glyphhub.sensors.SensorController
import com.pelikan.glyphhub.schedule.ScheduleException
import com.pelikan.glyphhub.schedule.SchedulePolicy
import com.pelikan.glyphhub.settings.DefaultDisplayMode
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.settings.ToySettings
import com.pelikan.glyphhub.systemstatus.GlyphStatusEvent
import com.pelikan.glyphhub.systemstatus.GlyphStatusEventRouter
import com.pelikan.glyphhub.systemstatus.GlyphStatusEventType
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToySensorEvent
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.toys.ToyRuntimeContext
import com.pelikan.glyphhub.widget.GlyphHubWidgetRenderer
import com.pelikan.glyphhub.glyph.assets.GlyphIconLibrary as SharedGlyphIconLibrary

class GlyphHubToyService : Service() {
    private lateinit var controller: GlyphMatrixController
    private lateinit var renderScheduler: GlyphRenderScheduler
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var sensors: SensorController
    private val statusRouter = GlyphStatusEventRouter()
    private var currentModule: GlyphToyModule? = null
    private var currentModuleIsActiveToy = false
    private var currentPhase = ServicePhase.Inactive
    private var lastTickAt = 0L
    private var loopRunning = false
    private var renderSessionId = 0L
    private var renderLoopRunnable: Runnable? = null
    private var transitionRunnable: Runnable? = null
    private var autoReturnRunnable: Runnable? = null
    private var chargingMonitorRunnable: Runnable? = null
    private var lastObservedCharging = false
    private var chargingOverrideStartedAt = 0L
    private var chargingWakeLock: PowerManager.WakeLock? = null
    private val serviceMessenger by lazy {
        Messenger(
            Handler(Looper.getMainLooper()) { message ->
                handleGlyphToyMessage(message)
                true
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundRenderer()
        thread = HandlerThread("GlyphHubRender")
        thread.start()
        handler = Handler(thread.looper)
        controller = GlyphMatrixController.create(this)
        controller.connect()
        renderScheduler = GlyphRenderScheduler(
            controller = controller,
            appendLog = { SettingsRepository.appendLog(this, it) },
            debugMode = { SettingsRepository.appSettings(this).debugMode }
        )
        sensors = SensorController(applicationContext) { module, sessionId, event ->
            handler.post {
                if (sessionId != renderSessionId || module !== currentModule || !currentModuleIsActiveToy) return@post
                runCatching { module.onSensorEvent(event) }
                    .onFailure {
                        SettingsRepository.appendLog(this, "sensor dispatch failed toy=${module.id} ${it.message}")
                    }
            }
        }
        SettingsRepository.appendLog(this, "service created sdkMode=${if (controller.isRealSdk) "real" else "fake"}")
        startChargingMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val toyId = intent?.getStringExtra(EXTRA_TOY_ID)
        when (action) {
            ACTION_ACTIVATE -> toyId?.let { selectedToyId ->
                handler.post { activateToy(selectedToyId) }
            }
            ACTION_DEACTIVATE -> handler.post {
                deactivateToy(toyId)
            }
            ACTION_CLEAR -> handler.post {
                clearRuntimeAndMatrix("explicit_clear")
            }
            ACTION_TEST_ACTIVATION -> handler.post {
                previewTransition(SettingsRepository.appSettings(this).activationAnimation)
            }
            ACTION_TEST_DEACTIVATION -> handler.post {
                previewTransition(SettingsRepository.appSettings(this).deactivationAnimation)
            }
            ACTION_STATUS_EVENT -> parseStatusEvent(intent)?.let { event ->
                handler.post { handleStatusEvent(event) }
            }
            else -> handler.post { syncFromSettings() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        SettingsRepository.appendLog(this, "system toy bound")
        if (::handler.isInitialized) handler.post { syncFromSettings() }
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        SettingsRepository.appendLog(this, "system toy unbound")
        if (::handler.isInitialized) {
            handler.post {
                val sessionId = beginRenderSession("system_unbind")
                shutdownCurrentModule()
                SettingsRepository.deactivateCurrentToy(this)
                updatePhase(ServicePhase.Inactive, "system_unbind")
                renderBlackFrame(sessionId, "system_unbind")
                GlyphHubWidgetRenderer.updateAll(this)
            }
        }
        return false
    }

    override fun onDestroy() {
        cancelScheduledWork()
        stopChargingMonitor()
        releaseChargingWakeLock()
        sensors.stop()
        runCatching { currentModule?.onDeactivate() }
        controller.close()
        thread.quitSafely()
        super.onDestroy()
    }

    private fun startForegroundRenderer(includeMicrophone: Boolean = false) {
        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_glyphhub_service)
            .setContentTitle("GlyphHub")
            .setContentText("Rendering Glyph Matrix toy")
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                if (includeMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            startForeground(
                NOTIFICATION_ID,
                notification,
                serviceType
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "GlyphHub renderer",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun syncFromSettings() {
        val settings = SettingsRepository.appSettings(this)
        val activeToyId = settings.activeToyId
        val power = batteryPowerState()
        if (power.isCharging && activeToyId == null) {
            ensureChargingOverrideLoop("sync", power)
            GlyphHubWidgetRenderer.updateAll(this)
            return
        }
        if (activeToyId != null) {
            if (currentModuleIsActiveToy && currentModule?.id == activeToyId) {
                refreshActiveToy(activeToyId)
            } else {
                activateToy(activeToyId, updateRepository = false, playActivationTransition = false)
            }
        } else {
            showDefaultDisplay("sync")
        }
        GlyphHubWidgetRenderer.updateAll(this)
    }

    private fun activateToy(
        toyId: String,
        updateRepository: Boolean = true,
        playActivationTransition: Boolean = true
    ) {
        val module = ToyRegistry.byId(toyId) ?: return
        val sessionId = beginRenderSession("activate:$toyId")
        shutdownCurrentModule()
        startForegroundRenderer(includeMicrophone = module.requiresMicrophoneForeground())
        val toySettings = SettingsRepository.getToySettings(this, module.id, module.settingsSchema)
        module.updateSettings(toySettings)
        if (updateRepository) SettingsRepository.activateToy(this, toyId)
        val runtimeContext = runtimeContext()
        val activated = runCatching { module.onActivate(runtimeContext) }
        if (activated.isFailure) {
            SettingsRepository.deactivateCurrentToy(this)
            SettingsRepository.appendLog(this, "activate failed toy=$toyId ${activated.exceptionOrNull()?.message}")
            currentModule = null
            currentModuleIsActiveToy = false
            showDefaultDisplay("activate_failed:$toyId")
            GlyphHubWidgetRenderer.updateAll(this)
            return
        }
        val override = toySettings.text("activationAnimationOverride", "")
        currentModule = module
        currentModuleIsActiveToy = true
        val shouldPlayActivationTransition = playActivationTransition && !module.requiresImmediateActivation()
        updatePhase(
            if (shouldPlayActivationTransition) ServicePhase.Transition else ServicePhase.ActiveToy,
            "activate:$toyId"
        )
        fun completeActivation() {
            if (sessionId != renderSessionId || currentModule?.id != toyId || !currentModuleIsActiveToy) {
                return
            }
            updatePhase(ServicePhase.ActiveToy, "active:$toyId")
            syncSensors(module)
            scheduleAutoReturn(sessionId, toyId)
            startLoop(sessionId)
            SettingsRepository.appendLog(this, "activated toy=$toyId")
            GlyphHubWidgetRenderer.updateAll(this)
        }
        if (shouldPlayActivationTransition) {
            playTransition(
                sessionId,
                override.ifBlank { SettingsRepository.appSettings(this).activationAnimation }
            ) { completeActivation() }
        } else {
            completeActivation()
        }
    }

    private fun deactivateToy(toyId: String?) {
        val module = currentModule?.takeIf { currentModuleIsActiveToy }
        val requestedToyId = toyId ?: module?.id ?: "unknown"
        val sessionId = beginRenderSession("deactivate:$requestedToyId")
        sensors.stop()
        if (module == null) {
            clearCurrentModuleReference()
            SettingsRepository.deactivateCurrentToy(this)
            SettingsRepository.appendLog(this, "deactivate skipped no active toy requested=$requestedToyId")
            showDefaultDisplay("after_deactivate:$requestedToyId")
            GlyphHubWidgetRenderer.updateAll(this)
            return
        }

        module.updateSettings(SettingsRepository.getToySettings(this, module.id, module.settingsSchema))
        val override = module.getSettings().text("deactivationAnimationOverride", "")
        updatePhase(ServicePhase.Transition, "deactivate:${module.id}")
        fun finishDeactivation() {
            if (sessionId != renderSessionId) {
                return
            }
            runCatching { module.onDeactivate() }
                .onFailure {
                    SettingsRepository.appendLog(this, "deactivate failed toy=${module.id} ${it.message}")
                }
            clearCurrentModuleReference()
            SettingsRepository.deactivateCurrentToy(this)
            SettingsRepository.appendLog(this, "deactivated toy=${module.id}")
            showDefaultDisplay("after_deactivate:${module.id}")
            GlyphHubWidgetRenderer.updateAll(this)
        }
        playTransition(
            sessionId,
            override.ifBlank { SettingsRepository.appSettings(this).deactivationAnimation }
        ) { finishDeactivation() }
    }

    private fun showDefaultDisplay(reason: String) {
        val sessionId = beginRenderSession("default:$reason")
        shutdownCurrentModule()
        val settings = SettingsRepository.appSettings(this)
        if (!settings.aodEnabled || settings.defaultDisplayMode == DefaultDisplayMode.Off) {
            currentModule = null
            updatePhase(ServicePhase.Inactive, "default_off")
            renderBlackFrame(sessionId, "default_off")
            SettingsRepository.appendLog(this, "default display cleared mode=${settings.defaultDisplayMode.id} aod=${settings.aodEnabled}")
            return
        }
        val module = defaultDisplayModule(settings.defaultDisplayMode)
            ?: ToyRegistry.byId(settings.defaultToyId)?.takeIf { it.supportsAod }

        if (module == null) {
            currentModule = null
            updatePhase(ServicePhase.Inactive, "default_unavailable")
            renderBlackFrame(sessionId, "default_unavailable")
            SettingsRepository.appendLog(this, "default display unavailable mode=${settings.defaultDisplayMode.id}")
            return
        }
        module.updateSettings(defaultDisplaySettings(module, settings.defaultDisplayMode))
        val activated = runCatching { module.onActivate(runtimeContext()) }
        if (activated.isFailure) {
            currentModule = null
            updatePhase(ServicePhase.Inactive, "default_activate_failed")
            renderBlackFrame(sessionId, "default_activate_failed")
            SettingsRepository.appendLog(this, "default display activation failed module=${module.id} ${activated.exceptionOrNull()?.message}")
            return
        }
        currentModule = module
        currentModuleIsActiveToy = false
        updatePhase(ServicePhase.DefaultDisplay, "default:${module.id}")
        SettingsRepository.appendLog(this, "default display module=${module.id} mode=${settings.defaultDisplayMode.id}")
        startLoop(sessionId)
    }

    private fun refreshActiveToy(toyId: String) {
        val module = currentModule?.takeIf { it.id == toyId && currentModuleIsActiveToy } ?: return
        module.updateSettings(SettingsRepository.getToySettings(this, module.id, module.settingsSchema))
        syncSensors(module)
        scheduleAutoReturn(renderSessionId, toyId)
        if (currentPhase != ServicePhase.Transition && !loopRunning) startLoop(renderSessionId)
    }

    private fun syncSensors(module: GlyphToyModule) {
        if (currentModuleIsActiveToy && SettingsRepository.appSettings(this).sensorsEnabled && module.supportsSensors) {
            sensors.startForToy(module, renderSessionId)
        } else {
            sensors.stop()
        }
    }

    private fun defaultDisplayModule(mode: DefaultDisplayMode): GlyphToyModule? {
        val settings = SettingsRepository.appSettings(this)
        return when (mode) {
            DefaultDisplayMode.Off -> null
            DefaultDisplayMode.Clock -> ToyRegistry.byId("clock")
            DefaultDisplayMode.Battery -> ToyRegistry.byId("battery")
            DefaultDisplayMode.Date -> ToyRegistry.byId("idle_default")
            DefaultDisplayMode.CustomGlyph -> ToyRegistry.byId("idle_default")
            DefaultDisplayMode.LastActive -> ToyRegistry.byId(settings.lastActiveToyId)
            DefaultDisplayMode.SelectedToy -> ToyRegistry.byId(settings.selectedToyId)
        }?.takeIf { it.supportsAod }
    }

    private fun defaultDisplaySettings(module: GlyphToyModule, mode: DefaultDisplayMode): ToySettings {
        val stored = SettingsRepository.getToySettings(this, module.id, module.settingsSchema)
        return if (module.id == "idle_default" && mode in setOf(DefaultDisplayMode.Date, DefaultDisplayMode.CustomGlyph)) {
            stored.withValue("selectedDefaultMode", mode.id)
        } else {
            stored
        }
    }

    private fun startLoop(sessionId: Long) {
        loopRunning = true
        lastTickAt = 0L
        renderLoopRunnable?.let(handler::removeCallbacks)
        val runnable = object : Runnable {
            override fun run() {
                if (sessionId != renderSessionId || !loopRunning) return
                val module = currentModule ?: return
                val now = SystemClock.elapsedRealtime()
                val delta = if (lastTickAt == 0L) 0L else now - lastTickAt
                lastTickAt = now
                val chargingOverride = chargingOverrideFrame(now)
                val statusFrame = if (chargingOverride == null) {
                    statusRouter.activeFrame(SettingsRepository.appSettings(this@GlyphHubToyService).globalBrightness, now)
                } else {
                    null
                }
                val frame = chargingOverride ?: statusFrame ?: runCatching { module.onTick(delta) }
                        .onFailure {
                            SettingsRepository.appendLog(this@GlyphHubToyService, "tick failed toy=${module.id} ${it.message}")
                        }
                        .getOrNull()
                if (frame != null) {
                    renderFrame(
                        sessionId = sessionId,
                        frame = frame,
                        source = if (chargingOverride != null) "charging_override:battery" else "loop:${module.id}",
                        now = now
                    )
                }
                renderLoopRunnable = this
                val nextDelayMs = when {
                    chargingOverride != null -> CHARGING_OVERRIDE_RENDER_INTERVAL_MS
                    module.id == "tuner" -> TUNER_RENDER_INTERVAL_MS
                    module.id == "level" -> LEVEL_RENDER_INTERVAL_MS
                    else -> RENDER_INTERVAL_MS
                }
                handler.postDelayed(this, nextDelayMs)
            }
        }
        renderLoopRunnable = runnable
        handler.post(runnable)
    }

    private fun stopLoop() {
        loopRunning = false
        if (::handler.isInitialized) {
            renderLoopRunnable?.let(handler::removeCallbacks)
            renderLoopRunnable = null
        }
    }

    private fun previewTransition(transitionId: String) {
        val sessionId = beginRenderSession("preview_transition:$transitionId")
        shutdownCurrentModule()
        updatePhase(ServicePhase.Transition, "preview_transition:$transitionId")
        playTransition(sessionId, transitionId) {
            if (sessionId != renderSessionId) return@playTransition
            updatePhase(ServicePhase.Inactive, "preview_transition_complete:$transitionId")
            renderBlackFrame(sessionId, "preview_transition_complete:$transitionId")
        }
    }

    private fun playTransition(sessionId: Long, transitionId: String, onComplete: () -> Unit) {
        cancelTransition()
        val settings = SettingsRepository.appSettings(this)
        val animation = GlyphTransitionEngine.animation(transitionId, settings.globalBrightness)
        if (animation.frames.isEmpty()) {
            onComplete()
            return
        }
        scheduleTransitionFrame(sessionId, animation, 0, onComplete)
    }

    private fun handleStatusEvent(event: GlyphStatusEvent) {
        if (event.type.isBatteryPowerEvent()) {
            handleBatteryPowerStatus(event.type)
            return
        }
        val settings = SettingsRepository.appSettings(this)
        if (!SchedulePolicy.matrixAllowed(settings, exception = exceptionFor(event.type))) {
            SettingsRepository.appendLog(this, "status blocked by schedule type=${event.type.id}")
            return
        }
        if (currentModuleIsActiveToy && event.type.priority < ACTIVE_TOY_INTERRUPT_PRIORITY) {
            SettingsRepository.appendLog(this, "status skipped active_owner type=${event.type.id} active=${currentModule?.id}")
            return
        }
        if (!statusRouter.submit(event)) {
            SettingsRepository.appendLog(this, "status skipped lower_priority type=${event.type.id}")
            return
        }
        SettingsRepository.appendLog(this, "status event type=${event.type.id} value=${event.value} label=${event.label}")
        if (!loopRunning) {
            val sessionId = if (currentModule == null) beginRenderSession("status:${event.type.id}") else renderSessionId
            if (currentModule == null) {
                currentModule = ToyRegistry.byId("idle_default")?.also { module ->
                    module.updateSettings(SettingsRepository.getToySettings(this, module.id, module.settingsSchema))
                    runCatching { module.onActivate(runtimeContext()) }
                }
                currentModuleIsActiveToy = false
                updatePhase(ServicePhase.DefaultDisplay, "status:${event.type.id}")
            }
            startLoop(sessionId)
        }
    }

    private fun exceptionFor(type: GlyphStatusEventType): ScheduleException =
        when (type) {
            GlyphStatusEventType.ChargingFull -> ScheduleException.ChargingFull
            GlyphStatusEventType.PaymentManual,
            GlyphStatusEventType.PaymentSuccessManual,
            GlyphStatusEventType.NfcEvent -> ScheduleException.Payment
            GlyphStatusEventType.Beacon -> ScheduleException.Beacon
            GlyphStatusEventType.Wake -> ScheduleException.Alarm
            else -> ScheduleException.None
        }

    private fun GlyphStatusEventType.isBatteryPowerEvent(): Boolean =
        this == GlyphStatusEventType.ChargingConnected ||
            this == GlyphStatusEventType.ChargingDisconnected ||
            this == GlyphStatusEventType.ChargingFull

    private fun handleBatteryPowerStatus(type: GlyphStatusEventType) {
        val power = batteryPowerState()
        if (power.isCharging) {
            holdChargingWakeLock(type.id)
            ensureChargingOverrideLoop(type.id, power)
        } else {
            chargingOverrideStartedAt = 0L
            SettingsRepository.appendLog(this, "charging override stopped type=${type.id}")
            if (currentPhase == ServicePhase.ChargingOverride && !currentModuleIsActiveToy) {
                syncFromSettings()
            }
        }
    }

    private fun ensureChargingOverrideLoop(reason: String, power: BatteryPowerState = batteryPowerState()) {
        if (!power.isCharging) return
        if (currentModuleIsActiveToy) {
            chargingOverrideStartedAt = 0L
            SettingsRepository.appendLog(this, "charging override skipped active toy=${currentModule?.id} level=${power.level}")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val alreadyOverriding = chargingOverrideStartedAt != 0L
        if (!alreadyOverriding) chargingOverrideStartedAt = now
        if (currentModule != null && loopRunning) {
            if (!alreadyOverriding) {
                renderChargingOverrideFrameNow(renderSessionId, reason, power, now)
                startLoop(renderSessionId)
                SettingsRepository.appendLog(this, "charging override started over=${currentModule?.id} level=${power.level} reason=$reason")
            } else {
                SettingsRepository.appendLog(this, "charging override active over=${currentModule?.id} level=${power.level} reason=$reason")
            }
            return
        }
        val module = ToyRegistry.byId("battery") ?: return
        val sessionId = beginRenderSession("charging_override:$reason")
        shutdownCurrentModule()
        module.updateSettings(SettingsRepository.getToySettings(this, module.id, module.settingsSchema))
        runCatching { module.onActivate(runtimeContext()) }
            .onFailure { SettingsRepository.appendLog(this, "charging override activate failed ${it.message}") }
        currentModule = module
        currentModuleIsActiveToy = false
        updatePhase(ServicePhase.ChargingOverride, "charging_override:$reason")
        renderChargingOverrideFrameNow(sessionId, reason, power, now)
        startLoop(sessionId)
        GlyphHubWidgetRenderer.updateAll(this)
    }

    private fun chargingOverrideFrame(now: Long): GlyphFrame? {
        if (currentModuleIsActiveToy) return null
        val power = batteryPowerState()
        if (!power.isCharging) return null
        if (chargingOverrideStartedAt == 0L) chargingOverrideStartedAt = now
        return chargingBatteryFrame(power, now)
    }

    private fun renderChargingOverrideFrameNow(
        sessionId: Long,
        reason: String,
        power: BatteryPowerState,
        now: Long = SystemClock.elapsedRealtime()
    ) {
        if (sessionId != renderSessionId) return
        renderFrame(
            sessionId = sessionId,
            frame = chargingBatteryFrame(power, now),
            source = "charging_override:battery:$reason",
            now = now
        )
    }

    private fun chargingBatteryFrame(power: BatteryPowerState, now: Long): GlyphFrame =
        SharedGlyphIconLibrary.battery(
            level = power.level,
            brightness = 100,
            charging = true,
            elapsedMs = (now - chargingOverrideStartedAt).coerceAtLeast(0L),
            lowWarning = false,
            animateCharging = true
        )

    private fun batteryPowerState(): BatteryPowerState {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val rawLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
            val level = if (rawLevel >= 0) (rawLevel * 100 / scale).coerceIn(0, 100) else 75
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
            return BatteryPowerState(level = level, isCharging = charging)
        }
        val manager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return BatteryPowerState(level = 75, isCharging = false)
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it >= 0 } ?: 75
        return BatteryPowerState(level = level, isCharging = manager.isCharging)
    }

    private fun holdChargingWakeLock(reason: String) {
        val manager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val lock = chargingWakeLock ?: manager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:charging_override"
        ).also {
            it.setReferenceCounted(false)
            chargingWakeLock = it
        }
        runCatching {
            if (lock.isHeld) lock.release()
            lock.acquire(CHARGING_OVERRIDE_WAKELOCK_MS)
            SettingsRepository.appendLog(this, "charging wakelock acquired reason=$reason")
        }.onFailure {
            SettingsRepository.appendLog(this, "charging wakelock skipped reason=$reason ${it.message}")
        }
    }

    private fun releaseChargingWakeLock() {
        chargingWakeLock?.let { lock ->
            runCatching {
                if (lock.isHeld) lock.release()
            }
        }
        chargingWakeLock = null
    }

    private fun startChargingMonitor() {
        if (chargingMonitorRunnable != null) return
        val runnable = object : Runnable {
            override fun run() {
                val power = batteryPowerState()
                if (power.isCharging) {
                    if (currentModuleIsActiveToy) {
                        chargingOverrideStartedAt = 0L
                    } else if (!lastObservedCharging || chargingOverrideStartedAt == 0L) {
                        holdChargingWakeLock("monitor")
                        ensureChargingOverrideLoop("monitor", power)
                    }
                } else if (lastObservedCharging) {
                    chargingOverrideStartedAt = 0L
                    SettingsRepository.appendLog(this@GlyphHubToyService, "charging override stopped type=monitor")
                    if (currentPhase == ServicePhase.ChargingOverride && !currentModuleIsActiveToy) {
                        syncFromSettings()
                    }
                } else {
                    chargingOverrideStartedAt = 0L
                }
                lastObservedCharging = power.isCharging
                chargingMonitorRunnable = this
                handler.postDelayed(this, CHARGING_MONITOR_INTERVAL_MS)
            }
        }
        chargingMonitorRunnable = runnable
        handler.post(runnable)
    }

    private fun stopChargingMonitor() {
        if (::handler.isInitialized) {
            chargingMonitorRunnable?.let(handler::removeCallbacks)
        }
        chargingMonitorRunnable = null
    }

    private fun parseStatusEvent(intent: Intent): GlyphStatusEvent? {
        val type = GlyphStatusEventType.fromId(intent.getStringExtra(EXTRA_STATUS_TYPE).orEmpty()) ?: return null
        return GlyphStatusEvent(
            type = type,
            value = intent.getIntExtra(EXTRA_STATUS_VALUE, 0),
            label = intent.getStringExtra(EXTRA_STATUS_LABEL).orEmpty(),
            ttlMs = intent.getLongExtra(EXTRA_STATUS_TTL, type.defaultTtlMs)
        )
    }

    private fun runtimeContext(): ToyRuntimeContext {
        val settings = SettingsRepository.appSettings(this)
        return ToyRuntimeContext(
            androidContext = applicationContext,
            settingsRepository = SettingsRepository,
            matrixWidth = controller.matrixWidth,
            matrixHeight = controller.matrixHeight,
            brightness = settings.globalBrightness
        )
    }

    private fun handleGlyphToyMessage(message: Message) {
        if (message.what != glyphToyMessageWhat()) return
        val event = message.data?.getString(glyphToyDataKey()).orEmpty()
        SettingsRepository.appendLog(this, "system toy event=$event")
        when (event) {
            glyphToyString("EVENT_AOD", "aod") -> handler.post { syncFromSettings() }
            glyphToyString("EVENT_CHANGE", "change") -> handler.post { syncFromSettings() }
            glyphToyString("EVENT_ACTION_DOWN", "action_down") -> handler.post { dispatchGlyphToyInteraction(event) }
        }
    }

    private fun dispatchGlyphToyInteraction(event: String) {
        val module = currentModule
        if (module == null || !currentModuleIsActiveToy) {
            SettingsRepository.appendLog(this, "glyph interaction ignored event=$event active=${module?.id}")
            return
        }
        runCatching { module.onSensorEvent(ToySensorEvent.BackTap) }
            .onFailure {
                SettingsRepository.appendLog(this, "glyph interaction failed toy=${module.id} ${it.message}")
            }
    }

    private fun glyphToyMessageWhat(): Int =
        runCatching { Class.forName(GLYPH_TOY_CLASS).getField("MSG_GLYPH_TOY").getInt(null) }
            .getOrDefault(1)

    private fun glyphToyDataKey(): String =
        glyphToyString("MSG_GLYPH_TOY_DATA", "data")

    private fun glyphToyString(fieldName: String, fallback: String): String =
        runCatching { Class.forName(GLYPH_TOY_CLASS).getField(fieldName).get(null) as? String }
            .getOrNull()
            ?: fallback

    private fun beginRenderSession(reason: String): Long {
        cancelScheduledWork()
        renderSessionId += 1
        renderScheduler.resetTracking()
        lastTickAt = 0L
        SettingsRepository.appendLog(this, "render session=$renderSessionId reason=$reason")
        return renderSessionId
    }

    private fun cancelScheduledWork() {
        stopLoop()
        cancelTransition()
        cancelAutoReturn()
    }

    private fun cancelTransition() {
        if (::handler.isInitialized) {
            transitionRunnable?.let(handler::removeCallbacks)
            transitionRunnable = null
        }
    }

    private fun cancelAutoReturn() {
        if (::handler.isInitialized) {
            autoReturnRunnable?.let(handler::removeCallbacks)
            autoReturnRunnable = null
        }
    }

    private fun scheduleTransitionFrame(
        sessionId: Long,
        animation: GlyphAnimation,
        frameIndex: Int,
        onComplete: () -> Unit
    ) {
        if (sessionId != renderSessionId) return
        val timedFrame = animation.frames.getOrNull(frameIndex)
        if (timedFrame == null) {
            transitionRunnable = null
            onComplete()
            return
        }
        renderFrame(
            sessionId = sessionId,
            frame = timedFrame.frame,
            source = "transition:${animation.id}:$frameIndex",
            transitionId = animation.id
        )
        val next = Runnable {
            scheduleTransitionFrame(sessionId, animation, frameIndex + 1, onComplete)
        }
        transitionRunnable = next
        handler.postDelayed(next, timedFrame.durationMs)
    }

    private fun renderFrame(
        sessionId: Long,
        frame: GlyphFrame,
        source: String,
        now: Long = SystemClock.elapsedRealtime(),
        transitionId: String? = null,
        intentionalFullMatrixFlash: Boolean = false
    ) {
        if (sessionId != renderSessionId) return
        renderScheduler.submit(
            sessionId = sessionId,
            source = source,
            frame = frame,
            transitionId = transitionId,
            now = now,
            keepAliveMs = STATIC_FRAME_KEEPALIVE_MS,
            intentionalFullMatrixFlash = intentionalFullMatrixFlash
        )
    }

    private fun renderBlackFrame(sessionId: Long, source: String) {
        if (sessionId != renderSessionId) return
        renderScheduler.submit(
            sessionId = sessionId,
            source = source,
            frame = GlyphFrame.empty(controller.matrixWidth, controller.matrixHeight, 0),
            now = SystemClock.elapsedRealtime(),
            keepAliveMs = 0L,
            allowAllOff = true
        )
        lastTickAt = 0L
    }

    private fun shutdownCurrentModule() {
        sensors.stop()
        val module = currentModule
        if (module != null) {
            deactivateModule(module)
        }
        if (module?.requiresMicrophoneForeground() == true) startForegroundRenderer(includeMicrophone = false)
        clearCurrentModuleReference()
    }

    private fun GlyphToyModule.requiresMicrophoneForeground(): Boolean =
        id == "tuner" || id == "sound_meter"

    private fun GlyphToyModule.requiresImmediateActivation(): Boolean =
        id == "tuner" || id == "sound_meter"

    private fun clearRuntimeAndMatrix(reason: String) {
        val sessionId = beginRenderSession(reason)
        shutdownCurrentModule()
        SettingsRepository.deactivateCurrentToy(this)
        updatePhase(ServicePhase.Inactive, reason)
        renderBlackFrame(sessionId, reason)
        GlyphHubWidgetRenderer.updateAll(this)
    }

    private fun scheduleAutoReturn(sessionId: Long, toyId: String) {
        cancelAutoReturn()
        val settings = SettingsRepository.appSettings(this)
        if (!settings.autoReturnToDefault) return
        val delayMs = settings.autoReturnDelaySeconds.coerceAtLeast(0).toLong() * 1000L
        val runnable = Runnable {
            if (sessionId != renderSessionId || currentModule?.id != toyId || !currentModuleIsActiveToy) return@Runnable
            SettingsRepository.appendLog(this, "auto return toy=$toyId delayMs=$delayMs")
            deactivateToy(toyId)
        }
        autoReturnRunnable = runnable
        if (delayMs <= 0L) {
            handler.post(runnable)
        } else {
            handler.postDelayed(runnable, delayMs)
        }
    }

    private fun deactivateModule(module: GlyphToyModule?) {
        module ?: return
        runCatching { module.onDeactivate() }
            .onFailure {
                SettingsRepository.appendLog(this, "module deactivate failed toy=${module.id} ${it.message}")
            }
    }

    private fun clearCurrentModuleReference() {
        currentModule = null
        currentModuleIsActiveToy = false
    }

    private fun updatePhase(phase: ServicePhase, reason: String) {
        if (currentPhase != phase) {
            SettingsRepository.appendLog(this, "phase ${currentPhase.id} -> ${phase.id} reason=$reason")
        }
        currentPhase = phase
    }

    private enum class ServicePhase(val id: String) {
        Inactive("inactive"),
        Transition("transition"),
        ActiveToy("active_toy"),
        DefaultDisplay("default_display"),
        ChargingOverride("charging_override")
    }

    private data class BatteryPowerState(
        val level: Int,
        val isCharging: Boolean
    )

    companion object {
        private const val GLYPH_TOY_CLASS = "com.nothing.ketchum.GlyphToy"
        private const val ACTION_SYNC = "com.pelikan.glyphhub.glyph.SYNC"
        private const val ACTION_ACTIVATE = "com.pelikan.glyphhub.glyph.ACTIVATE"
        private const val ACTION_DEACTIVATE = "com.pelikan.glyphhub.glyph.DEACTIVATE"
        private const val ACTION_CLEAR = "com.pelikan.glyphhub.glyph.CLEAR"
        private const val ACTION_TEST_ACTIVATION = "com.pelikan.glyphhub.glyph.TEST_ACTIVATION"
        private const val ACTION_TEST_DEACTIVATION = "com.pelikan.glyphhub.glyph.TEST_DEACTIVATION"
        private const val ACTION_STATUS_EVENT = "com.pelikan.glyphhub.glyph.STATUS_EVENT"
        private const val EXTRA_TOY_ID = "toyId"
        private const val EXTRA_STATUS_TYPE = "statusType"
        private const val EXTRA_STATUS_VALUE = "statusValue"
        private const val EXTRA_STATUS_LABEL = "statusLabel"
        private const val EXTRA_STATUS_TTL = "statusTtl"
        private const val NOTIFICATION_CHANNEL_ID = "glyphhub_renderer"
        private const val NOTIFICATION_ID = 25111
        private const val RENDER_INTERVAL_MS = 120L
        private const val TUNER_RENDER_INTERVAL_MS = 33L
        private const val LEVEL_RENDER_INTERVAL_MS = 33L
        private const val CHARGING_OVERRIDE_RENDER_INTERVAL_MS = 33L
        private const val CHARGING_MONITOR_INTERVAL_MS = 180L
        private const val CHARGING_OVERRIDE_WAKELOCK_MS = 8_000L
        private const val STATIC_FRAME_KEEPALIVE_MS = 1000L
        private const val ACTIVE_TOY_INTERRUPT_PRIORITY = 40

        fun requestSync(context: Context) = start(context, ACTION_SYNC)
        fun requestActivate(context: Context, toyId: String) = start(context, ACTION_ACTIVATE, toyId)
        fun requestDeactivate(context: Context, toyId: String? = null) = start(context, ACTION_DEACTIVATE, toyId)
        fun requestClear(context: Context) = start(context, ACTION_CLEAR)
        fun requestTestActivation(context: Context) = start(context, ACTION_TEST_ACTIVATION)
        fun requestTestDeactivation(context: Context) = start(context, ACTION_TEST_DEACTIVATION)
        fun requestStatusEvent(context: Context, event: GlyphStatusEvent) {
            val intent = Intent(context, GlyphHubToyService::class.java)
                .setAction(ACTION_STATUS_EVENT)
                .putExtra(EXTRA_STATUS_TYPE, event.type.id)
                .putExtra(EXTRA_STATUS_VALUE, event.value)
                .putExtra(EXTRA_STATUS_LABEL, event.label)
                .putExtra(EXTRA_STATUS_TTL, event.ttlMs)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { SettingsRepository.appendLog(context, "service start failed action=$ACTION_STATUS_EVENT ${it.message}") }
        }

        private fun start(context: Context, action: String, toyId: String? = null) {
            val intent = Intent(context, GlyphHubToyService::class.java).setAction(action)
            if (toyId != null) intent.putExtra(EXTRA_TOY_ID, toyId)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
                .onFailure { SettingsRepository.appendLog(context, "service start failed action=$action ${it.message}") }
        }
    }
}
