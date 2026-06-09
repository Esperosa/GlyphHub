package com.pelikan.glyphhub.accuracy

import android.hardware.SensorManager

object ToolReadinessChecker {
    fun compass(hasHeadingCapability: Boolean, hasHeadingSample: Boolean, sensorAccuracy: Int?): AccuracyProfile {
        if (!hasHeadingCapability) {
            return AccuracyProfile(
                state = ToolReadinessState.Unsupported,
                title = "Compass unavailable",
                message = "This device does not expose a reliable heading source.",
                sensorQuality = SensorQuality.Unsupported,
                calibrationState = CalibrationState.NotRequired
            )
        }
        if (!hasHeadingSample) {
            return AccuracyProfile(
                state = ToolReadinessState.Calibrating,
                title = "Compass warming up",
                message = "Waiting for stable heading samples.",
                sensorQuality = SensorQuality.Unknown,
                calibrationState = CalibrationState.Calibrating
            )
        }
        return when (sensorAccuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> AccuracyProfile(
                state = ToolReadinessState.NeedsCalibration,
                title = "Compass calibration needed",
                message = "Move the phone in a figure-eight until heading reliability improves.",
                sensorQuality = SensorQuality.Unstable,
                calibrationState = CalibrationState.Uncalibrated
            )
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> AccuracyProfile(
                state = ToolReadinessState.Degraded,
                title = "Compass degraded",
                message = "Heading is available, but low sensor accuracy limits trust.",
                sensorQuality = SensorQuality.Noisy,
                calibrationState = CalibrationState.Calibrating
            )
            else -> AccuracyProfile.ready(
                title = "Compass ready",
                message = "Heading source is active and considered usable."
            )
        }
    }

    fun level(hasAccelerometer: Boolean, sampleCount: Int, noise: Float): AccuracyProfile {
        if (!hasAccelerometer) {
            return AccuracyProfile(
                state = ToolReadinessState.Unsupported,
                title = "Level unavailable",
                message = "Accelerometer support is missing on this device.",
                sensorQuality = SensorQuality.Unsupported,
                calibrationState = CalibrationState.NotRequired
            )
        }
        if (sampleCount < 4) {
            return AccuracyProfile(
                state = ToolReadinessState.Calibrating,
                title = "Level warming up",
                message = "Collecting enough samples for a stable zero point.",
                calibrationState = CalibrationState.Calibrating
            )
        }
        if (noise > 0.12f) {
            return AccuracyProfile(
                state = ToolReadinessState.Unstable,
                title = "Level unstable",
                message = "Sensor noise is high. Hold the phone steady on the target surface.",
                sensorQuality = SensorQuality.Unstable,
                calibrationState = CalibrationState.Calibrating
            )
        }
        return AccuracyProfile.ready(
            title = "Level ready",
            message = "Tilt samples are stable enough for guidance."
        )
    }

    fun lux(hasLightSensor: Boolean, hasReading: Boolean): AccuracyProfile {
        if (!hasLightSensor) {
            return AccuracyProfile(
                state = ToolReadinessState.Unsupported,
                title = "Lux sensor unavailable",
                message = "This device does not expose an ambient light sensor.",
                sensorQuality = SensorQuality.Unsupported,
                calibrationState = CalibrationState.NotRequired
            )
        }
        if (!hasReading) {
            return AccuracyProfile(
                state = ToolReadinessState.Calibrating,
                title = "Lux warming up",
                message = "Waiting for the first ambient light sample.",
                calibrationState = CalibrationState.Calibrating
            )
        }
        return AccuracyProfile.ready(
            title = "Lux ready",
            message = "Ambient light readings are available."
        )
    }

    fun weather(city: String, networkAvailable: Boolean, providerConfigured: Boolean): AccuracyProfile {
        if (city.isBlank()) {
            return AccuracyProfile(
                state = ToolReadinessState.Error,
                title = "Weather setup required",
                message = "Choose a city before Weather can show real data.",
                matrixLabel = "SET"
            )
        }
        if (!networkAvailable) {
            return AccuracyProfile(
                state = ToolReadinessState.Degraded,
                title = "Weather offline",
                message = "Network access is unavailable, so only cached or placeholder data can be shown.",
                sensorQuality = SensorQuality.Noisy,
                matrixLabel = "OFF"
            )
        }
        if (!providerConfigured) {
            return AccuracyProfile(
                state = ToolReadinessState.Error,
                title = "Weather provider missing",
                message = "Open-Meteo is the expected provider, but this build path reports it unavailable.",
                matrixLabel = "WIP"
            )
        }
        return AccuracyProfile.ready(
            title = "Weather ready",
            message = "Weather provider and connectivity are available."
        )
    }
}
