package com.pelikan.glyphhub.accuracy

enum class ToolReadinessState {
    Ready,
    Calibrating,
    NeedsPermission,
    NeedsCalibration,
    Unstable,
    Unsupported,
    Degraded,
    Error
}

data class AccuracyProfile(
    val state: ToolReadinessState,
    val title: String,
    val message: String,
    val sensorQuality: SensorQuality = SensorQuality.Unknown,
    val calibrationState: CalibrationState = CalibrationState.NotRequired,
    val matrixLabel: String = defaultMatrixLabel(state)
) {
    val isReady: Boolean = state == ToolReadinessState.Ready

    companion object {
        fun ready(title: String, message: String): AccuracyProfile =
            AccuracyProfile(
                state = ToolReadinessState.Ready,
                title = title,
                message = message,
                sensorQuality = SensorQuality.Stable,
                calibrationState = CalibrationState.Calibrated,
                matrixLabel = ""
            )

        private fun defaultMatrixLabel(state: ToolReadinessState): String = when (state) {
            ToolReadinessState.Ready -> ""
            ToolReadinessState.Calibrating -> "WAIT"
            ToolReadinessState.NeedsPermission -> "MIC"
            ToolReadinessState.NeedsCalibration -> "CAL"
            ToolReadinessState.Unstable -> "WAIT"
            ToolReadinessState.Unsupported -> "NO"
            ToolReadinessState.Degraded -> "LOW"
            ToolReadinessState.Error -> "ERR"
        }
    }
}