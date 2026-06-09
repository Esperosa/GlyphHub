package com.pelikan.glyphhub.glyph.assets

import android.content.Context

class GlyphAssetRepository(context: Context) {
    private val loader = GlyphAssetLoader(context.applicationContext)
    private val manifest: GlyphAssetManifest? by lazy { loader.loadManifest() }

    fun manifest(): GlyphAssetManifest? = manifest

    fun listAssetPaths(): List<String> =
        manifest?.assets.orEmpty().map { "glyphs/$it" } +
            manifest?.transitions.orEmpty().map { "glyphs/$it" }

    fun loadAll(): List<GlyphAnimationAsset> =
        listAssetPaths().mapNotNull { loader.loadAsset(it) }

    fun loadById(id: String): GlyphAnimationAsset? =
        loadAll().firstOrNull { it.asset.id == id }
}
