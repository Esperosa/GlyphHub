package com.pelikan.glyphhub.glyph

import android.content.Context
import android.util.Log
import com.pelikan.glyphhub.settings.SettingsRepository
import java.lang.reflect.Proxy

class RealGlyphMatrixController(
    private val context: Context
) : GlyphMatrixController {
    override var matrixWidth: Int = GlyphMatrixController.DEFAULT_MATRIX_SIZE
        private set
    override var matrixHeight: Int = GlyphMatrixController.DEFAULT_MATRIX_SIZE
        private set
    override val isRealSdk: Boolean = true
    override var lastFrame: GlyphFrame? = null
        private set

    private var matrixManager: Any? = null
    private var callbackProxy: Any? = null
    @Volatile private var serviceConnected = false
    @Volatile private var registered = false
    @Volatile private var pendingFrame: GlyphFrame? = null
    private val frameLock = Any()

    override fun connect() {
        matrixManager = loadMatrixManager()
        detectMatrixSize()
        initMatrixManager()
        Log.d(
            SettingsRepository.LOG_TAG,
            "Real Glyph SDK bridge initialized device=${GlyphMatrixController.DEVICE_IDENTIFIER} sdkTarget=${sdkDeviceTarget()} matrix=${matrixWidth}x$matrixHeight"
        )
    }

    override fun renderFrame(frame: GlyphFrame) {
        synchronized(frameLock) {
            lastFrame = frame
            if (!serviceConnected) {
                pendingFrame = frame
                return
            }
            if (!registered && !registerDevice()) {
                pendingFrame = frame
                return
            }
            renderSdkFrame(frame).recoverCatching {
                runCatching { setMatrixTimeout(enabled = false) }
                renderAppSdkFrame(frame).getOrThrow()
            }.onFailure {
                pendingFrame = frame
                Log.d(SettingsRepository.LOG_TAG, "Real SDK frame queued: ${it.message}")
            }
        }
    }

    override fun close() {
        synchronized(frameLock) {
            runCatching { matrixManager?.javaClass?.getMethod("closeAppMatrix")?.invoke(matrixManager) }
            runCatching { matrixManager?.javaClass?.getMethod("turnOff")?.invoke(matrixManager) }
            runCatching { matrixManager?.javaClass?.getMethod("unInit")?.invoke(matrixManager) }
            matrixManager = null
            callbackProxy = null
            serviceConnected = false
            registered = false
            pendingFrame = null
            lastFrame = null
        }
    }

    private fun loadMatrixManager(): Any? {
        val managerClass = runCatching { Class.forName(GLYPH_MATRIX_MANAGER_CLASS) }.getOrNull() ?: return null
        return runCatching {
            managerClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        }.getOrNull()
    }

    private fun initMatrixManager() {
        val manager = matrixManager ?: return
        val callbackClass = runCatching { Class.forName(GLYPH_MATRIX_CALLBACK_CLASS) }.getOrNull() ?: return
        callbackProxy = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, _ ->
            when (method.name) {
                "onServiceConnected" -> {
                    serviceConnected = true
                    val success = registerDevice()
                    Log.d(SettingsRepository.LOG_TAG, "Real Glyph SDK service connected registered=$success")
                    flushPendingFrame()
                }
                "onServiceDisconnected" -> {
                    serviceConnected = false
                    registered = false
                    Log.d(SettingsRepository.LOG_TAG, "Real Glyph SDK service disconnected")
                }
            }
            null
        }
        runCatching {
            manager.javaClass.getMethod("init", callbackClass).invoke(manager, callbackProxy)
        }.onFailure {
            Log.d(SettingsRepository.LOG_TAG, "Real Glyph SDK init failed: ${it.message}")
        }
    }

    private fun detectMatrixSize() {
        val detected = runCatching {
            Class.forName(COMMON_CLASS)
                .getMethod("getDeviceMatrixLength")
                .invoke(null) as? Int
        }.getOrNull()?.takeIf { it > 0 }
            ?: sdkMatrixLength()

        if (detected != null && detected > 0) {
            matrixWidth = detected
            matrixHeight = detected
        }
    }

    private fun registerDevice(): Boolean {
        synchronized(frameLock) {
            val manager = matrixManager ?: return false
            return runCatching {
                val registeredNow = manager.javaClass.getMethod("register", String::class.java)
                    .invoke(manager, sdkDeviceTarget()) as? Boolean
                registered = registeredNow == true
                if (registered) setMatrixTimeout(enabled = false)
                registered
            }.getOrElse {
                registered = false
                false
            }
        }
    }

    private fun renderSdkFrame(frame: GlyphFrame): Result<Unit> = runCatching {
        val manager = matrixManager ?: error("GlyphMatrixManager is unavailable.")
        val sdkFrame = buildSdkMatrixFrame(frame)
        val sdkFrameClass = Class.forName(GLYPH_MATRIX_FRAME_CLASS)
        manager.javaClass.getMethod("setAppMatrixFrame", sdkFrameClass).invoke(manager, sdkFrame)
    }

    private fun renderAppSdkFrame(frame: GlyphFrame): Result<Unit> = runCatching {
        val manager = matrixManager ?: error("GlyphMatrixManager is unavailable.")
        manager.javaClass.getMethod("setAppMatrixFrame", IntArray::class.java).invoke(manager, toMatrixPayload(frame))
    }

    private fun setMatrixTimeout(enabled: Boolean) {
        val manager = matrixManager ?: return
        runCatching {
            manager.javaClass.getMethod("setGlyphMatrixTimeout", Boolean::class.javaPrimitiveType)
                .invoke(manager, enabled)
        }.onFailure {
            Log.d(SettingsRepository.LOG_TAG, "Real SDK timeout mode skipped: ${it.message}")
        }
    }

    private fun buildSdkMatrixFrame(frame: GlyphFrame): Any {
        val builderClass = Class.forName(GLYPH_MATRIX_FRAME_BUILDER_CLASS)
        val builder = builderClass.getConstructor().newInstance()
        val payload = toMatrixPayload(frame)
        builderClass.getMethod("addTop", IntArray::class.java).invoke(builder, payload)
        return builderClass.getMethod("build", Context::class.java).invoke(builder, context)
    }

    private fun toMatrixPayload(frame: GlyphFrame): IntArray {
        val output = IntArray(matrixWidth * matrixHeight)
        val offsetX = (matrixWidth - frame.width) / 2
        val offsetY = (matrixHeight - frame.height) / 2
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val targetX = x + offsetX
                val targetY = y + offsetY
                if (targetX in 0 until matrixWidth && targetY in 0 until matrixHeight) {
                    val physicalLed = matrixWidth != GlyphMatrixLayout.SIZE ||
                        matrixHeight != GlyphMatrixLayout.SIZE ||
                        GlyphMatrixLayout.isPhysicalLed(targetX, targetY)
                    val intensity = if (physicalLed) frame.intensityAt(x, y) else 0
                    output[targetY * matrixWidth + targetX] =
                        mapRelativeIntensityToSdk(frame.brightness, intensity)
                }
            }
        }
        return output
    }

    private fun mapRelativeIntensityToSdk(frameIntensity: Int, pixelIntensity: Int): Int {
        val frameLevel = frameIntensity.coerceIn(0, 100)
        val pixelLevel = pixelIntensity.coerceIn(0, 100)
        return frameLevel * pixelLevel * SDK_MAX_INTENSITY / 10_000
    }

    private fun flushPendingFrame() {
        synchronized(frameLock) {
            val frame = pendingFrame ?: return
            pendingFrame = null
            renderSdkFrame(frame).onFailure {
                pendingFrame = frame
                Log.d(SettingsRepository.LOG_TAG, "Real SDK pending frame render failed: ${it.message}")
            }
        }
    }

    private fun sdkDeviceTarget(): String =
        runCatching {
            Class.forName(GLYPH_CLASS).getField("DEVICE_25111p").get(null) as? String
        }.getOrNull() ?: GlyphMatrixController.DEVICE_IDENTIFIER

    private fun sdkMatrixLength(): Int? =
        runCatching {
            Class.forName(GLYPH_CLASS).getField("DEVICE_25111p_MATRIX_LENGTH").getInt(null)
        }.getOrNull()?.takeIf { it > 0 }

    companion object {
        private const val COMMON_CLASS = "com.nothing.ketchum.Common"
        private const val GLYPH_CLASS = "com.nothing.ketchum.Glyph"
        private const val GLYPH_MATRIX_MANAGER_CLASS = "com.nothing.ketchum.GlyphMatrixManager"
        private const val GLYPH_MATRIX_CALLBACK_CLASS = "com.nothing.ketchum.GlyphMatrixManager\$Callback"
        private const val GLYPH_MATRIX_FRAME_CLASS = "com.nothing.ketchum.GlyphMatrixFrame"
        private const val GLYPH_MATRIX_FRAME_BUILDER_CLASS = "com.nothing.ketchum.GlyphMatrixFrame\$Builder"
        private const val SDK_MAX_INTENSITY = 255

        fun isSdkAvailable(): Boolean =
            runCatching { Class.forName(GLYPH_MATRIX_MANAGER_CLASS) }.isSuccess &&
                runCatching { Class.forName(GLYPH_MATRIX_FRAME_BUILDER_CLASS) }.isSuccess
    }
}
