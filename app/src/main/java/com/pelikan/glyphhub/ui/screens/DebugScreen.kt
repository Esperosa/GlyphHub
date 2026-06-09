package com.pelikan.glyphhub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.glyph.GlyphHubToyService
import com.pelikan.glyphhub.glyph.GlyphMatrixController
import com.pelikan.glyphhub.glyph.assets.GlyphAssetRepository
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidationOptions
import com.pelikan.glyphhub.glyph.validation.GlyphFrameValidator
import com.pelikan.glyphhub.settings.AppSettings
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.ToyRegistry
import com.pelikan.glyphhub.ui.components.MatrixPreview
import com.pelikan.glyphhub.ui.components.NothingButton
import com.pelikan.glyphhub.ui.components.PixelGrid
import com.pelikan.glyphhub.ui.theme.GlyphBlack
import com.pelikan.glyphhub.ui.theme.GlyphMuted

@Composable
fun DebugScreen(
    appSettings: AppSettings,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val controller = GlyphMatrixController.create(context)
    val module = ToyRegistry.byId(appSettings.activeToyId) ?: ToyRegistry.byId(appSettings.selectedToyId) ?: ToyRegistry.modules.first()
    module.updateSettings(settingsRepository.getToySettings(context, module.id, module.settingsSchema))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("BACK", onClick = onBack)
            Text("Debug", style = MaterialTheme.typography.headlineMedium)
        }
        Text("selectedToyId=${appSettings.selectedToyId}")
        Text("activeToyId=${appSettings.activeToyId ?: "null"}")
        Text("matrix=${controller.matrixWidth}x${controller.matrixHeight}")
        Text("sdkMode=${if (controller.isRealSdk) "real" else "fake"}")
        MatrixPreview(frame = module.previewFrame(), modifier = Modifier.size(104.dp), active = appSettings.activeToyId != null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("TEST IN", onClick = { GlyphHubToyService.requestTestActivation(context); onRefresh() })
            NothingButton("TEST OUT", onClick = { GlyphHubToyService.requestTestDeactivation(context); onRefresh() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("DICE", onClick = { SettingsRepository.activateToy(context, "dice"); GlyphHubToyService.requestActivate(context, "dice"); onRefresh() })
            NothingButton("COIN", onClick = { SettingsRepository.activateToy(context, "coin"); GlyphHubToyService.requestActivate(context, "coin"); onRefresh() })
            NothingButton("CLOCK", onClick = { SettingsRepository.activateToy(context, "clock"); GlyphHubToyService.requestActivate(context, "clock"); onRefresh() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("CLEAR", onClick = { SettingsRepository.deactivateCurrentToy(context); GlyphHubToyService.requestClear(context); onRefresh() })
            NothingButton("WIDGET", onClick = { com.pelikan.glyphhub.widget.GlyphHubWidgetRenderer.updateAll(context); onRefresh() })
        }
        AssetQualityLab(
            appSettings = appSettings,
            settingsRepository = settingsRepository,
            onTest = { toyId ->
                SettingsRepository.activateToy(context, toyId)
                GlyphHubToyService.requestActivate(context, toyId)
                onRefresh()
            }
        )
        Text("LOGS", style = MaterialTheme.typography.titleMedium)
        settingsRepository.debugLogs(context).takeLast(20).forEach {
            Text(it, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AssetQualityLab(
    appSettings: AppSettings,
    settingsRepository: SettingsRepository,
    onTest: (String) -> Unit
) {
    val context = LocalContext.current
    val canonicalAssets = remember(context) { GlyphAssetRepository(context).loadAll() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Asset Quality / Animation Lab", style = MaterialTheme.typography.titleMedium)
        if (canonicalAssets.isNotEmpty()) {
            Text("Canonical Assets", style = MaterialTheme.typography.bodyLarge)
            canonicalAssets.forEach { animationAsset ->
                val asset = animationAsset.asset
                val firstFrame = asset.frames.firstOrNull()?.frame ?: return@forEach
                AssetQualityFrameRow(
                    label = "${asset.name} (${asset.id})",
                    frame = firstFrame,
                    active = false,
                    directional = asset.metadata.directional,
                    onTest = null
                )
            }
        }
        Text("Toy Outputs", style = MaterialTheme.typography.bodyLarge)
        ToyRegistry.modules
            .filter { module -> appSettings.showExperimentalToys || ToyRegistry.visibleModules(false).any { it.id == module.id } }
            .forEach { module ->
                module.updateSettings(settingsRepository.getToySettings(context, module.id, module.settingsSchema))
                AssetQualityRow(
                    module = module,
                    frame = module.previewFrame(),
                    active = appSettings.activeToyId == module.id,
                    onTest = onTest
                )
            }
    }
}

@Composable
private fun AssetQualityRow(
    module: GlyphToyModule,
    frame: GlyphFrame,
    active: Boolean,
    onTest: (String) -> Unit
) {
    val validation = GlyphFrameValidator.validate(
        frame,
        GlyphFrameValidationOptions(
            allowAllOff = false,
            directional = module.id in setOf("compass", "level", "maze")
        )
    )
    AssetQualityFrameRow(
        label = "${module.name} (${module.id})",
        frame = validation.sanitizedFrame,
        active = active,
        directional = module.id in setOf("compass", "level", "maze"),
        onTest = { onTest(module.id) }
    )
}

@Composable
private fun AssetQualityFrameRow(
    label: String,
    frame: GlyphFrame,
    active: Boolean,
    directional: Boolean,
    onTest: (() -> Unit)?
) {
    val validation = GlyphFrameValidator.validate(
        frame,
        GlyphFrameValidationOptions(
            allowAllOff = false,
            directional = directional
        )
    )
    val metrics = validation.metrics
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PixelGrid(
            frame = validation.sanitizedFrame,
            modifier = Modifier.size(72.dp),
            active = active,
            showOffPixels = true
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "lit=${metrics.physicalLitPixels}/${metrics.physicalLedCount} fill=${metrics.fillRatioPercent}% " +
                    "bbox=${metrics.boundingBox?.let { "${it.width}x${it.height}" } ?: "-"}",
                color = GlyphMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "center=${metrics.centerOfMassX?.format1() ?: "-"},${metrics.centerOfMassY?.format1() ?: "-"} " +
                    "intensity=${metrics.minIntensity}-${metrics.maxIntensity} unique=${metrics.uniqueIntensityCount}",
                color = GlyphMuted,
                style = MaterialTheme.typography.bodySmall
            )
            val issueText = validation.issues.joinToString { it.code }.ifBlank { "ok" }
            Text("validation=$issueText", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (onTest != null) {
            NothingButton("TEST", onClick = onTest)
        }
    }
}

private fun Double.format1(): String =
    ((this * 10.0).toInt() / 10.0).toString()
