package com.pelikan.glyphhub.glyph

import android.content.Context
import org.json.JSONObject
import kotlin.math.roundToInt

class GlyphAssetLoader(private val context: Context) {
    fun loadAnimation(assetPath: String): GlyphAnimation? =
        runCatching {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            when {
                root.has("frames") -> loadGlyphHubAnimation(root)
                root.optInt("gridSize", 0) == GlyphFrame.MATRIX_SIZE && root.has("brightness") ->
                    loadToyphBrightnessGlyph(root, assetPath)
                else -> null
            }
        }.getOrNull()

    private fun loadGlyphHubAnimation(root: JSONObject): GlyphAnimation {
        val framesJson = root.getJSONArray("frames")
        val frames = buildList {
            for (index in 0 until framesJson.length()) {
                val frameJson = framesJson.getJSONObject(index)
                val rowsJson = frameJson.getJSONArray("pixels")
                val rows = buildList {
                    for (row in 0 until rowsJson.length()) add(rowsJson.getString(row))
                }
                add(
                    GlyphAnimationFrame(
                        frame = GlyphMatrixLayout.mask(
                            GlyphFrame.fromBinaryRows(
                                rows,
                                frameJson.optInt("brightness", 80)
                            )
                        ),
                        durationMs = frameJson.optLong("durationMs", 120L)
                    )
                )
            }
        }
        val id = root.getString("id")
        return GlyphAnimation(
            id = id,
            name = root.optString("name", id),
            loop = root.optBoolean("loop", false),
            frames = frames
        )
    }

    private fun loadToyphBrightnessGlyph(root: JSONObject, assetPath: String): GlyphAnimation? {
        val brightnessJson = root.getJSONArray("brightness")
        val size = root.optInt("gridSize", GlyphFrame.MATRIX_SIZE)
        if (size != GlyphFrame.MATRIX_SIZE || brightnessJson.length() != size * size) return null
        val pixels = buildList {
            for (index in 0 until brightnessJson.length()) {
                val raw = brightnessJson.optInt(index, 0)
                val intensity = if (raw <= 0) {
                    0
                } else {
                    ((raw.coerceAtMost(4095) / 4095.0) * 100.0).roundToInt().coerceIn(1, 100)
                }
                add(intensity)
            }
        }
        val id = root.optString("id", assetPath.substringAfterLast('/').substringBeforeLast('.'))
        return GlyphAnimation(
            id = id,
            name = root.optString("name", id),
            loop = false,
            frames = listOf(
                GlyphAnimationFrame(
                    frame = GlyphMatrixLayout.mask(GlyphFrame(size, size, brightness = 100, pixels = pixels)),
                    durationMs = root.optLong("displayDurationMs", 1000L)
                )
            )
        )
    }
}
