package com.pelikan.glyphhub.toys

import android.hardware.Sensor
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.settings.ToySettingsSchema
import kotlin.math.abs

class MazeToyModule : BaseGlyphToyModule() {
    override val id = "maze"
    override val name = "Maze Toy"
    override val shortName = "MAZE"
    override val description = "Tilt through a compact maze and reach the goal."
    override val iconAsset = "glyphs/idle/default_idle.json"
    override val supportsAod = true
    override val supportsSensors = true
    override val settingsSchema = ToySettingsSchema(
        listOf(
            ToySettingDefinition("tiltThreshold", "Tilt threshold", "Required tilt before the cursor moves.", ToySettingType.Int, "35", 10, 85),
            ToySettingDefinition("moveCooldownMs", "Step ms", "Minimum delay between tilt steps.", ToySettingType.Int, "160", 60, 500),
            ToySettingDefinition("showTrail", "Trail", "Show visited path.", ToySettingType.Boolean, "false"),
            ToySettingDefinition("brightness", "Brightness", "Toy brightness.", ToySettingType.Int, "80", 0, 100),
            activationAnimationOverrideDefinition(),
            deactivationAnimationOverrideDefinition()
        )
    )

    private var playerX = START_X
    private var playerY = START_Y
    private var completed = false
    private var lastMoveAt = 0L
    private val trail = linkedSetOf(START_X to START_Y)

    override fun onActivate(context: ToyRuntimeContext) {
        reset()
    }

    override fun onSensorEvent(event: ToySensorEvent) {
        when (event) {
            is ToySensorEvent.Raw -> handleTilt(event)
            is ToySensorEvent.Shake -> reset()
            ToySensorEvent.BackTap -> reset()
            else -> Unit
        }
    }

    override fun onTick(deltaMs: Long): GlyphFrame {
        val brightness = getSettings().int("brightness", 80)
        val maze = frameFromRows(MAZE_ROWS, brightness)
        val goal = frameFromPoints(
            setOf(
                GOAL_X to GOAL_Y,
                (GOAL_X - 1) to GOAL_Y,
                GOAL_X to (GOAL_Y - 1),
                (GOAL_X - 1) to (GOAL_Y - 1)
            ),
            brightness
        )
        val path = if (getSettings().bool("showTrail", false)) frameFromPoints(trail, brightness) else GlyphFrame.empty13(brightness)
        val player = frameFromPoints(
            setOf(
                playerX to playerY,
                (playerX - 1) to playerY,
                (playerX + 1) to playerY,
                playerX to (playerY - 1),
                playerX to (playerY + 1)
            ),
            brightness
        )
        val output = maze.overlay(path).overlay(goal).overlay(player)
        return output
    }

    private fun handleTilt(event: ToySensorEvent.Raw) {
        if (completed || event.sensorType != Sensor.TYPE_ACCELEROMETER || event.values.size < 2) return
        val threshold = getSettings().int("tiltThreshold", 35).coerceIn(10, 85) / 100f
        val cooldownMs = getSettings().int("moveCooldownMs", 160).toLong().coerceIn(60L, 500L)
        if (event.timestamp - lastMoveAt < cooldownMs) return
        val x = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
        val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
        val move = when {
            abs(x) > abs(y) && abs(x) >= threshold -> if (x > 0f) -1 to 0 else 1 to 0
            abs(y) >= threshold -> 0 to if (y > 0f) 1 else -1
            else -> null
        } ?: return
        lastMoveAt = event.timestamp
        attemptMove(move.first, move.second)
    }

    private fun attemptMove(dx: Int, dy: Int) {
        val targetX = playerX + dx
        val targetY = playerY + dy
        if (!isOpen(targetX, targetY)) return
        playerX = targetX
        playerY = targetY
        trail += playerX to playerY
        if (playerX == GOAL_X && playerY == GOAL_Y) {
            completed = true
        }
    }

    private fun reset() {
        playerX = START_X
        playerY = START_Y
        completed = false
        lastMoveAt = 0L
        trail.clear()
        trail += START_X to START_Y
    }

    private fun isOpen(x: Int, y: Int): Boolean {
        if (x !in 0 until GlyphFrame.MATRIX_SIZE || y !in 0 until GlyphFrame.MATRIX_SIZE) return false
        return MAZE_ROWS[y][x] == '0'
    }

    private companion object {
        const val START_X = 1
        const val START_Y = 1
        const val GOAL_X = 11
        const val GOAL_Y = 11

        val MAZE_ROWS = listOf(
            "1111111111111",
            "1000000000001",
            "1111111111101",
            "1000000000101",
            "1011111110101",
            "1010000010101",
            "1010111010101",
            "1010100010101",
            "1010101110101",
            "1010101000101",
            "1010101111101",
            "1000100000001",
            "1111111111111"
        )
    }
}
