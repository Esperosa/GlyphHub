package com.pelikan.glyphhub.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToySensorEvent

class SensorController(
    private val context: Context,
    private val dispatchEvent: (GlyphToyModule, Long, ToySensorEvent) -> Unit = { toy, _, event ->
        toy.onSensorEvent(event)
    }
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val shakeDetector = ShakeDetector()
    private val backTapDetector = BackTapDetector(enabled = false)
    private val compass = CompassSensorProvider()
    private var activeToy: GlyphToyModule? = null
    private var activeSessionId: Long = 0L
    private val seenSensorTypes = mutableSetOf<Int>()

    fun startForToy(module: GlyphToyModule, sessionId: Long) {
        stop()
        if (!module.supportsSensors) return
        activeToy = module
        activeSessionId = sessionId
        seenSensorTypes.clear()
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val light = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val stepDetector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val stepCounter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        SettingsRepository.appendLog(
            context,
            "sensor availability toy=${module.id} accel=${accelerometer != null} magnetic=${magneticField != null} rotation=${rotationVector != null} light=${light != null} stepDetector=${stepDetector != null} stepCounter=${stepCounter != null}"
        )
        accelerometer?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magneticField?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationVector?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        light?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        stepDetector?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCounter?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        SettingsRepository.appendLog(context, "sensors started for ${module.id}")
    }

    fun stop() {
        manager.unregisterListener(this)
        activeToy = null
        activeSessionId = 0L
        seenSensorTypes.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val toy = activeToy ?: return
        val timestampMs = System.currentTimeMillis()
        if (seenSensorTypes.add(event.sensor.type)) {
            SettingsRepository.appendLog(context, "sensor event first toy=${toy.id} type=${event.sensor.type}")
        }
        dispatch(ToySensorEvent.Raw(event.sensor.type, event.values.copyOf(), timestampMs))
        compass.headingFromSensor(event.sensor.type, event.values)?.let {
            dispatch(ToySensorEvent.Heading(it))
        }
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                shakeDetector.onAccelerometer(event.values, timestampMs)?.let {
                    dispatch(ToySensorEvent.Shake(it))
                }
                if (backTapDetector.onAccelerometer(event.values, timestampMs)) {
                    dispatch(ToySensorEvent.BackTap)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val toy = activeToy ?: return
        val sensorType = sensor?.type ?: return
        dispatchEvent(toy, activeSessionId, ToySensorEvent.SensorAccuracy(sensorType, accuracy))
    }

    private fun dispatch(event: ToySensorEvent) {
        val toy = activeToy ?: return
        dispatchEvent(toy, activeSessionId, event)
    }
}
