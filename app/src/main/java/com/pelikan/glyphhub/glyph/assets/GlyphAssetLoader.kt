package com.pelikan.glyphhub.glyph.assets

import android.content.Context
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.validation.GlyphFrameNormalizer
import org.json.JSONArray
import org.json.JSONObject

class GlyphAssetLoader(private val context: Context) {
    fun loadManifest(path: String = "glyphs/manifest.json"): GlyphAssetManifest? =
        runCatching {
            val root = JSONObject(readAsset(path))
            GlyphAssetManifest(
                matrix = root.optInt("matrix", GlyphFrame.MATRIX_SIZE),
                device = root.optString("device", "Glyph.DEVICE_25111p"),
                assets = root.optJSONArray("assets").toStringList(),
                transitions = root.optJSONArray("transitions").toStringList()
            )
        }.getOrNull()

    fun loadAsset(path: String): GlyphAnimationAsset? =
        runCatching {
            parseAnimationAsset(JSONObject(readAsset(path)), path)
        }.getOrNull()

    fun parseAnimationAsset(root: JSONObject, path: String = ""): GlyphAnimationAsset {
        val matrix = root.optInt("matrix", GlyphFrame.MATRIX_SIZE)
        require(matrix == GlyphFrame.MATRIX_SIZE) { "Only 13x13 Glyph assets are supported." }
        val type = GlyphAssetType.fromId(root.optString("type", if (root.optBoolean("loop", false)) "animation" else "icon"))
        val respectCircularMask = root.optBoolean("respectCircularMask", true)
        val normalizeBrightness = root.optBoolean("normalizeBrightness", true)
        val frames = root.getJSONArray("frames")
        val parsedFrames = buildList {
            for (index in 0 until frames.length()) {
                val frameJson = frames.getJSONObject(index)
                add(
                    GlyphAssetFrame(
                        frame = parseFrame(frameJson, respectCircularMask && normalizeBrightness),
                        durationMs = frameJson.optLong("durationMs", defaultDuration(root))
                    )
                )
            }
        }
        val id = root.optString("id", path.substringAfterLast('/').substringBeforeLast('.'))
        val asset = GlyphAsset(
            id = id,
            name = root.optString("name", id),
            matrix = matrix,
            type = type,
            respectCircularMask = respectCircularMask,
            normalizeBrightness = normalizeBrightness,
            frames = parsedFrames,
            metadata = parseMetadata(root.optJSONObject("metadata"))
        )
        return GlyphAnimationAsset(
            asset = asset,
            loop = root.optBoolean("loop", false),
            fps = root.optInt("fps", 12).coerceAtLeast(1)
        )
    }

    private fun parseFrame(frameJson: JSONObject, normalize: Boolean): GlyphFrame {
        val rows = frameJson.getJSONArray("pixels").toStringList()
        val width = rows.firstOrNull()?.length ?: GlyphFrame.MATRIX_SIZE
        val pixels = rows.flatMap { row ->
            row.map { char ->
                when {
                    char == '0' || char == '.' -> 0
                    char == '1' -> 100
                    char in '2'..'9' -> ((char - '0') * 100 / 9).coerceIn(1, 100)
                    char in 'A'..'Z' -> (10 + (char - 'A') * 3).coerceIn(1, 100)
                    else -> 0
                }
            }
        }
        val frame = GlyphFrame(width, rows.size, frameJson.optInt("brightness", 80), pixels)
        return if (normalize) GlyphFrameNormalizer.normalize(frame) else frame
    }

    private fun parseMetadata(json: JSONObject?): GlyphAssetMetadata {
        if (json == null) return GlyphAssetMetadata()
        return GlyphAssetMetadata(
            category = json.optString("category", ""),
            intendedUse = json.optJSONArray("intendedUse").toStringList(),
            source = json.optString("source", "internal"),
            sourceTool = json.optString("sourceTool", "GlyphHub"),
            validated = json.optBoolean("validated", false),
            allowAllOff = json.optBoolean("allowAllOff", false),
            allowFullMatrixFlash = json.optBoolean("allowFullMatrixFlash", false),
            allowOffMaskPixels = json.optBoolean("allowOffMaskPixels", false),
            particleEffect = json.optBoolean("particleEffect", false),
            directional = json.optBoolean("directional", false),
            notes = json.optString("notes", "")
        )
    }

    private fun defaultDuration(root: JSONObject): Long {
        val fps = root.optInt("fps", 12).coerceAtLeast(1)
        return (1000L / fps).coerceAtLeast(1L)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) add(getString(index))
        }
    }
}
