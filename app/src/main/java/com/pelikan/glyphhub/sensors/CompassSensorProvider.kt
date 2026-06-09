package com.pelikan.glyphhub.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import kotlin.math.roundToInt

class CompassSensorProvider {
    private val rotationMatrix = FloatArray(9)
    private val inclinationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var accelerometerValues: FloatArray? = null
    private var magneticValues: FloatArray? = null

    fun headingFromSensor(sensorType: Int, values: FloatArray): Float? {
        if (values.isEmpty()) return null
        return when (sensorType) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
                headingFromMatrix(rotationMatrix)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = values.copyOf()
                headingFromAccelerometerAndMagneticField()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticValues = values.copyOf()
                headingFromAccelerometerAndMagneticField()
            }
            else -> null
        }
    }

    private fun headingFromAccelerometerAndMagneticField(): Float? {
        val accelerometer = accelerometerValues ?: return null
        val magnetic = magneticValues ?: return null
        val success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometer, magnetic)
        if (!success) return null
        return headingFromMatrix(rotationMatrix)
    }

    private fun headingFromMatrix(matrix: FloatArray): Float {
        SensorManager.getOrientation(matrix, orientation)
        val degrees = Math.toDegrees(orientation[0].toDouble()).roundToInt().toFloat()
        return ((degrees % 360f) + 360f) % 360f
    }
}
