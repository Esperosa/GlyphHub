package com.pelikan.glyphhub.toys

sealed class ToySensorEvent {
    data class Shake(val force: Float) : ToySensorEvent()
    data class Heading(val degrees: Float) : ToySensorEvent()
    data class SensorAccuracy(val sensorType: Int, val accuracy: Int) : ToySensorEvent()
    data object BackTap : ToySensorEvent()
    data class Raw(val sensorType: Int, val values: FloatArray, val timestamp: Long) : ToySensorEvent()
}
