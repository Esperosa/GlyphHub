package com.pelikan.glyphhub.glyph.animation

import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.validation.GlyphFrameNormalizer
import org.json.JSONArray
import org.json.JSONObject

object GlyphMatrixEditorImporter : GlyphAnimationImporter {
    override val id: String = "glyph_matrix_editor"
    override val displayName: String = "Glyph Matrix Editor JSON"

    override fun import(bytes: ByteArray, options: GlyphImportOptions): GlyphImportResult =
        runCatching {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val frames = when {
                root.has("frames") -> parseFrames(root.getJSONArray("frames"), options)
                root.has("pixels") -> listOf(parsePixelRows(root.getJSONArray("pixels"), options))
                root.optInt("gridSize", 0) == GlyphFrame.MATRIX_SIZE && root.has("brightness") ->
                    listOf(parseBrightnessArray(root.getJSONArray("brightness"), options))
                else -> return GlyphImportResult.Unsupported("Unsupported Glyph Matrix Editor JSON shape.")
            }
            GlyphImportResult.Success(
                GlyphFrameSequence(
                    id = root.optString("id", options.assetId),
                    name = root.optString("name", options.assetName),
                    loop = root.optBoolean("loop", options.loop),
                    fps = root.optInt("fps", options.fps),
                    frames = frames
                ),
                notes = listOf("Imported JSON through clean-room 13x13 parser.")
            )
        }.getOrElse { GlyphImportResult.Failed("Glyph Matrix Editor import failed.", it) }

    private fun parseFrames(framesJson: JSONArray, options: GlyphImportOptions): List<GlyphSequenceFrame> =
        buildList {
            for (index in 0 until framesJson.length()) {
                val frameJson = framesJson.getJSONObject(index)
                val frame = when {
                    frameJson.has("pixels") -> parsePixelRows(frameJson.getJSONArray("pixels"), options)
                    frameJson.has("brightness") -> parseBrightnessArray(frameJson.getJSONArray("brightness"), options)
                    else -> null
                } ?: continue
                add(GlyphSequenceFrame(frame.frame, frame.durationMs(frameJson, options)))
            }
        }

    private fun GlyphSequenceFrame.durationMs(frameJson: JSONObject, options: GlyphImportOptions): Long =
        frameJson.optLong("durationMs", (1000L / options.fps.coerceAtLeast(1)).coerceAtLeast(1L))

    private fun parsePixelRows(rowsJson: JSONArray, options: GlyphImportOptions): GlyphSequenceFrame {
        val rows = buildList {
            for (row in 0 until rowsJson.length()) add(rowsJson.getString(row))
        }
        val pixels = rows.flatMap { row ->
            row.map { char ->
                when {
                    char == '0' || char == '.' -> 0
                    char in '1'..'9' -> ((char - '0') * 100 / 9).coerceIn(1, 100)
                    char in 'A'..'Z' -> (10 + (char - 'A') * 3).coerceIn(1, 100)
                    else -> 0
                }
            }
        }
        val frame = GlyphFrame(rows.firstOrNull()?.length ?: 0, rows.size, options.brightness, pixels)
        return GlyphSequenceFrame(GlyphFrameNormalizer.normalize(frame, options.respectCircularMask), 1000L)
    }

    private fun parseBrightnessArray(brightnessJson: JSONArray, options: GlyphImportOptions): GlyphSequenceFrame {
        val pixels = buildList {
            for (index in 0 until brightnessJson.length()) {
                val raw = brightnessJson.optInt(index, 0)
                add(if (raw <= 0) 0 else ((raw.coerceAtMost(4095) / 4095.0) * 100.0).toInt().coerceIn(1, 100))
            }
        }
        val frame = GlyphFrame(GlyphFrame.MATRIX_SIZE, GlyphFrame.MATRIX_SIZE, options.brightness, pixels)
        return GlyphSequenceFrame(GlyphFrameNormalizer.normalize(frame, options.respectCircularMask), 1000L)
    }
}
