package com.pelikan.glyphhub.web

import android.content.Context
import com.pelikan.glyphhub.glyph.GlyphHubToyService
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidationOptions
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidator
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.toys.GlyphIconLibrary
import com.pelikan.glyphhub.toys.ToyRegistry
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap

object GlyphWebPortalManager {
    private var server: GlyphWebPortalServer? = null

    fun start(context: Context, port: Int = 8765, token: String): Boolean {
        if (server != null) return true
        val created = runCatching {
            GlyphWebPortalServer(context.applicationContext, port, token).also {
                it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
        }.getOrNull() ?: return false
        server = created
        SettingsRepository.appendLog(context, "web portal started port=$port")
        return true
    }

    fun stop(context: Context) {
        server?.stop()
        server = null
        SettingsRepository.appendLog(context, "web portal stopped")
    }

    fun running(): Boolean = server != null
}

class GlyphWebPortalServer(
    private val context: Context,
    port: Int,
    private val token: String
) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        if (!authorized(session)) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Missing or invalid token.")
        }
        return when {
            session.uri == "/" -> html()
            session.uri == "/status" -> json(statusJson())
            session.uri == "/toys" -> json(toysJson())
            session.uri == "/activate" -> {
                val toyId = session.parameters["toy"]?.firstOrNull()
                if (toyId != null && ToyRegistry.byId(toyId) != null) {
                    SettingsRepository.activateToy(context, toyId)
                    GlyphHubToyService.requestActivate(context, toyId)
                    json(JSONObject().put("ok", true).put("activated", toyId))
                } else {
                    json(JSONObject().put("ok", false).put("error", "unknown_toy"), Response.Status.BAD_REQUEST)
                }
            }
            session.uri == "/deactivate" -> {
                SettingsRepository.deactivateCurrentToy(context)
                GlyphHubToyService.requestDeactivate(context)
                json(JSONObject().put("ok", true))
            }
            session.uri == "/asset/upload" -> uploadAsset(session)
            session.uri == "/asset/export" -> exportAsset()
            session.uri == "/matrix/test-frame" -> testFrame(session)
            session.uri == "/logs" -> json(JSONObject().put("logs", JSONArray(SettingsRepository.debugLogs(context))))
            session.uri == "/settings/export" -> json(settingsExportJson())
            session.uri == "/settings/import" -> importSettings(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun authorized(session: IHTTPSession): Boolean {
        if (token.isBlank()) return false
        val queryToken = session.parameters["token"]?.firstOrNull()
        val headerToken = session.headers["x-glyphhub-token"]
        return queryToken == token || headerToken == token
    }

    private fun statusJson(): JSONObject {
        val settings = SettingsRepository.appSettings(context)
        return JSONObject()
            .put("selectedToyId", settings.selectedToyId)
            .put("activeToyId", settings.activeToyId)
            .put("webPortal", true)
    }

    private fun toysJson(): JSONObject {
        val toys = JSONArray()
        ToyRegistry.entries.forEach { entry ->
            toys.put(
                JSONObject()
                    .put("id", entry.module.id)
                    .put("name", entry.module.name)
                    .put("category", entry.category.name)
                    .put("visibility", entry.visibility.name)
            )
        }
        return JSONObject().put("toys", toys)
    }

    private fun html(): Response =
        newFixedLengthResponse(
            Response.Status.OK,
            "text/html",
            """
            <!doctype html>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>GlyphHub Portal</title>
            <style>
            body{font-family:system-ui;background:#050505;color:#f4f4f4;margin:24px}
            button{background:#e60012;color:white;border:0;padding:10px 12px;margin:4px;border-radius:6px}
            .grid{display:grid;grid-template-columns:repeat(13,18px);gap:3px;margin-top:16px}
            .cell{width:18px;height:18px;border-radius:50%;background:#222;border:1px solid #444}
            .on{background:#fff}
            </style>
            <h1>GlyphHub</h1>
            <p>Local development portal. Asset upload/test routes are intentionally limited to safe JSON/settings operations.</p>
            <button onclick="fetch('/status?token=${token}').then(r=>r.json()).then(j=>out.textContent=JSON.stringify(j,null,2))">Status</button>
            <button onclick="fetch('/toys?token=${token}').then(r=>r.json()).then(j=>out.textContent=JSON.stringify(j,null,2))">Toys</button>
            <button onclick="testFrame()">Test Frame</button>
            <pre id="out"></pre>
            <div id="grid" class="grid"></div>
            <script>
            const g=document.getElementById('grid');
            for(let i=0;i<169;i++){const c=document.createElement('div');c.className='cell';c.onclick=()=>c.classList.toggle('on');g.appendChild(c);}
            function rows(){let cs=[...document.querySelectorAll('.cell')];let out=[];for(let y=0;y<13;y++){out.push(cs.slice(y*13,y*13+13).map(c=>c.classList.contains('on')?'1':'0').join(''));}return out;}
            function testFrame(){fetch('/matrix/test-frame?token=${token}',{method:'POST',body:JSON.stringify({pixels:rows()})}).then(r=>r.json()).then(j=>out.textContent=JSON.stringify(j,null,2));}
            </script>
            """.trimIndent()
        )

    private fun json(json: JSONObject, status: Response.Status = Response.Status.OK): Response =
        newFixedLengthResponse(status, "application/json", json.toString())

    private fun uploadAsset(session: IHTTPSession): Response {
        val body = requestBody(session) ?: return json(JSONObject().put("ok", false).put("error", "missing_body"), Response.Status.BAD_REQUEST)
        val rows = rowsFromJson(body) ?: return json(JSONObject().put("ok", false).put("error", "unsupported_asset_json"), Response.Status.BAD_REQUEST)
        val result = validateRows(rows)
        if (!result.valid) {
            return json(
                JSONObject()
                    .put("ok", false)
                    .put("issues", JSONArray(result.issues.map { it.code })),
                Response.Status.BAD_REQUEST
            )
        }
        saveCustomRows(rows)
        return json(JSONObject().put("ok", true).put("rows", JSONArray(rows)).put("warnings", JSONArray(result.warnings.map { it.code })))
    }

    private fun exportAsset(): Response {
        val module = ToyRegistry.byId("pixel_art") ?: return json(JSONObject().put("ok", false), Response.Status.NOT_FOUND)
        val settings = SettingsRepository.getToySettings(context, module.id, module.settingsSchema)
        val rows = GlyphIconLibrary.rowsForEditor(settings.values["selectedGlyphAsset"].orEmpty().ifBlank { "heart" }, settings.values["customGlyphRows"].orEmpty())
        return json(
            JSONObject()
                .put("id", "portal_export")
                .put("matrix", 13)
                .put("type", "icon")
                .put("pixels", JSONArray(rows))
        )
    }

    private fun testFrame(session: IHTTPSession): Response {
        val body = requestBody(session) ?: return json(JSONObject().put("ok", false).put("error", "missing_body"), Response.Status.BAD_REQUEST)
        val rows = rowsFromJson(body) ?: return json(JSONObject().put("ok", false).put("error", "unsupported_frame_json"), Response.Status.BAD_REQUEST)
        val result = validateRows(rows)
        if (!result.valid) {
            return json(JSONObject().put("ok", false).put("issues", JSONArray(result.issues.map { it.code })), Response.Status.BAD_REQUEST)
        }
        saveCustomRows(rows)
        SettingsRepository.activateToy(context, "pixel_art")
        GlyphHubToyService.requestActivate(context, "pixel_art")
        return json(JSONObject().put("ok", true).put("warnings", JSONArray(result.warnings.map { it.code })))
    }

    private fun importSettings(session: IHTTPSession): Response {
        val body = requestBody(session) ?: return json(JSONObject().put("ok", false).put("error", "missing_body"), Response.Status.BAD_REQUEST)
        val input = runCatching { JSONObject(body) }.getOrNull()
            ?: return json(JSONObject().put("ok", false).put("error", "invalid_json"), Response.Status.BAD_REQUEST)
        SettingsRepository.updateAppSettings(context) { settings ->
            settings.copy(
                aodEnabled = input.optBoolean("aodEnabled", settings.aodEnabled),
                sensorsEnabled = input.optBoolean("sensorsEnabled", settings.sensorsEnabled),
                scheduleEnabled = input.optBoolean("scheduleEnabled", settings.scheduleEnabled),
                nightStart = input.optString("nightStart", settings.nightStart),
                nightEnd = input.optString("nightEnd", settings.nightEnd),
                showExperimentalToys = input.optBoolean("showExperimentalToys", settings.showExperimentalToys)
            )
        }
        return json(JSONObject().put("ok", true))
    }

    private fun settingsExportJson(): JSONObject {
        val settings = SettingsRepository.appSettings(context)
        return JSONObject()
            .put("aodEnabled", settings.aodEnabled)
            .put("sensorsEnabled", settings.sensorsEnabled)
            .put("scheduleEnabled", settings.scheduleEnabled)
            .put("nightStart", settings.nightStart)
            .put("nightEnd", settings.nightEnd)
            .put("showExperimentalToys", settings.showExperimentalToys)
    }

    private fun requestBody(session: IHTTPSession): String? {
        val files = HashMap<String, String>()
        return runCatching {
            session.parseBody(files)
            files["postData"] ?: session.parameters["json"]?.firstOrNull()
        }.getOrNull()
    }

    private fun rowsFromJson(body: String): List<String>? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val pixels = json.optJSONArray("pixels")
            ?: json.optJSONArray("frames")?.optJSONObject(0)?.optJSONArray("pixels")
            ?: return null
        val rows = List(pixels.length()) { index -> pixels.optString(index, "") }
        return GlyphIconLibrary.normalizeRows(rows).takeIf { it.size == GlyphFrame.MATRIX_SIZE }
    }

    private fun validateRows(rows: List<String>) =
        GlyphFrameValidator.validate(
            GlyphFrame.fromBinaryRows(rows, brightness = 80),
            GlyphFrameValidationOptions(allowAllOff = false, textRendererSource = false)
        )

    private fun saveCustomRows(rows: List<String>) {
        val encoded = GlyphIconLibrary.encodeRows(rows)
        SettingsRepository.updateToySetting(context, "pixel_art", "selectedGlyphAsset", "custom")
        SettingsRepository.updateToySetting(context, "pixel_art", "customGlyphRows", encoded)
    }
}
