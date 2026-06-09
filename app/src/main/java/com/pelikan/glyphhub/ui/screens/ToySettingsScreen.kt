package com.pelikan.glyphhub.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pelikan.glyphhub.glyph.GlyphTransitionEngine
import com.pelikan.glyphhub.schoolonline.SkolaOnlineRepository
import com.pelikan.glyphhub.schoolonline.SkolaOnlineServer
import com.pelikan.glyphhub.schoolonline.SkolaOnlineSyncResult
import com.pelikan.glyphhub.settings.SettingsRepository
import com.pelikan.glyphhub.settings.ToySettingDefinition
import com.pelikan.glyphhub.settings.ToySettingType
import com.pelikan.glyphhub.glyph.GlyphFrame
import com.pelikan.glyphhub.toys.GlyphIconLibrary
import com.pelikan.glyphhub.toys.GlyphToyModule
import com.pelikan.glyphhub.toys.SchoolMatrixText
import com.pelikan.glyphhub.ui.components.EditablePixelGrid
import com.pelikan.glyphhub.ui.components.MatrixPreview
import com.pelikan.glyphhub.ui.components.NothingButton
import com.pelikan.glyphhub.ui.components.NothingSwitch
import com.pelikan.glyphhub.ui.components.SettingsRow
import com.pelikan.glyphhub.ui.theme.GlyphBlack
import com.pelikan.glyphhub.ui.theme.GlyphLine
import com.pelikan.glyphhub.ui.theme.GlyphMuted
import com.pelikan.glyphhub.ui.theme.GlyphPanel
import com.pelikan.glyphhub.ui.theme.GlyphPanelSoft
import com.pelikan.glyphhub.ui.theme.GlyphRed
import com.pelikan.glyphhub.ui.theme.GlyphWhite
import java.time.DayOfWeek

@Composable
fun ToySettingsScreen(
    module: GlyphToyModule,
    settingsRepository: SettingsRepository,
    isActive: Boolean,
    quickOnly: Boolean,
    debugMode: Boolean,
    onBack: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current
    var permissionRefresh by remember(module.id) { mutableIntStateOf(0) }
    var settings by remember(module.id) {
        mutableStateOf(settingsRepository.getToySettings(context, module.id, module.settingsSchema))
    }
    module.updateSettings(settings)
    val quickEntries = module.settingsSchema.quickEntries(debugMode)
    val normalEntries = module.settingsSchema.normalEntries(debugMode)
    val advancedEntries = module.settingsSchema.advancedEntries(debugMode)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("BACK", onClick = onBack)
            Text(
                if (quickOnly) "${module.name} Quick Settings" else module.name,
                style = MaterialTheme.typography.headlineMedium
            )
        }
        MatrixPreview(frame = module.previewFrame(), active = isActive, modifier = Modifier.size(104.dp))
        PermissionSetupPanel(module.id, permissionRefresh) { permissionRefresh += 1 }
        if (module.id == "pixel_art" && !quickOnly) {
            PixelArtEditor(
                encodedRows = settings.values["customGlyphRows"].orEmpty(),
                fallbackRows = GlyphIconLibrary.rowsForEditor(
                    id = settings.values["selectedGlyphAsset"].orEmpty().ifBlank { "heart" },
                    customRows = settings.values["customGlyphRows"].orEmpty()
                ),
                onRowsChanged = { rows ->
                    settings = settings
                        .withValue("selectedGlyphAsset", "custom")
                        .withValue("customGlyphRows", rows)
                    settingsRepository.updateToySetting(context, module.id, "selectedGlyphAsset", "custom")
                    settingsRepository.updateToySetting(context, module.id, "customGlyphRows", rows)
                    onSettingsChanged()
                }
            )
        }
        if (module.id == "school_class_timer" && !quickOnly) {
            SchoolOnlinePanel()
            SchoolScheduleEditor(
                encodedSchedule = settings.values["schedule"].orEmpty(),
                onScheduleChanged = { schedule ->
                    settings = settings.withValue("schedule", schedule)
                    settingsRepository.updateToySetting(context, module.id, "schedule", schedule)
                    onSettingsChanged()
                }
            )
        }
        if (quickOnly) {
            SettingsSection(
                title = "Quick",
                entries = quickEntries,
                settings = settings,
                onValueChange = { definition, value ->
                    settings = settings.withValue(definition.key, value)
                    settingsRepository.updateToySetting(context, module.id, definition.key, value)
                    onSettingsChanged()
                }
            )
        } else {
            SettingsSection(
                title = "Quick",
                entries = quickEntries,
                settings = settings,
                onValueChange = { definition, value ->
                    settings = settings.withValue(definition.key, value)
                    settingsRepository.updateToySetting(context, module.id, definition.key, value)
                    onSettingsChanged()
                }
            )
            SettingsSection(
                title = "Settings",
                entries = normalEntries,
                settings = settings,
                onValueChange = { definition, value ->
                    settings = settings.withValue(definition.key, value)
                    settingsRepository.updateToySetting(context, module.id, definition.key, value)
                    onSettingsChanged()
                }
            )
            SettingsSection(
                title = "Advanced",
                entries = advancedEntries,
                settings = settings,
                onValueChange = { definition, value ->
                    settings = settings.withValue(definition.key, value)
                    settingsRepository.updateToySetting(context, module.id, definition.key, value)
                    onSettingsChanged()
                }
            )
        }
    }
}

@Composable
private fun SchoolOnlinePanel() {
    val context = LocalContext.current
    val repository = remember { SkolaOnlineRepository(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf(repository.accountStatus()) }
    var enabled by remember(status) { mutableStateOf(status.enabled) }
    var server by remember(status) { mutableStateOf(status.server) }
    var username by remember(status) { mutableStateOf(status.username) }
    var password by remember { mutableStateOf("") }
    var syncMessage by remember(status) {
        mutableStateOf(status.lastSyncStatus.ifBlank { "Not synced" })
    }
    var syncing by remember { mutableStateOf(false) }
    val cached = repository.cachedSchedule()

    fun saveAndMaybeSync(sync: Boolean) {
        repository.saveAccount(
            enabled = enabled,
            server = server,
            username = username,
            password = password.takeIf { it.isNotBlank() }
        )
        status = repository.accountStatus()
        if (!sync || !enabled) return
        syncing = true
        syncMessage = "Syncing"
        Thread({
            val result = repository.refresh()
            mainHandler.post {
                status = repository.accountStatus()
                syncMessage = when (result) {
                    SkolaOnlineSyncResult.Disabled -> "Disabled"
                    is SkolaOnlineSyncResult.Failure -> "Sync failed: ${result.reason}"
                    is SkolaOnlineSyncResult.Success -> "Synced ${result.snapshot.lessons.size} lessons"
                }
                password = ""
                syncing = false
            }
        }, "GlyphHubSchoolOnlineSettings").apply {
            isDaemon = true
            start()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlyphLine, MaterialTheme.shapes.small)
            .background(GlyphPanel)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Škola Online", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(syncMessage, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
            }
            NothingSwitch(enabled) { enabled = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkolaOnlineServer.entries.forEach { option ->
                NothingButton(
                    if (option == SkolaOnlineServer.Default) "ONLINE" else "PLZEŇ",
                    active = server == option,
                    onClick = { server = option }
                )
            }
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.take(80) },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(120) },
            label = { Text(if (status.hasPassword) "Password (saved)" else "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (cached != null) {
            Text(
                "Cache: ${cached.lessons.size} lessons / ${cached.dateFrom} - ${cached.dateTo}",
                color = GlyphMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("SAVE", onClick = { saveAndMaybeSync(sync = true) })
            NothingButton("SYNC", onClick = { saveAndMaybeSync(sync = true) }, active = syncing)
        }
    }
}

@Composable
private fun SchoolScheduleEditor(
    encodedSchedule: String,
    onScheduleChanged: (String) -> Unit
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var selectedLessonIndex by remember { mutableStateOf(0) }
    var schedule by remember(encodedSchedule) { mutableStateOf(parseSchoolSchedule(encodedSchedule)) }

    fun commit(next: Map<DayOfWeek, List<SchoolLessonUi>>) {
        schedule = next
        onScheduleChanged(encodeSchoolSchedule(next))
    }

    val lessons = schedule[selectedDay].orEmpty()
    val selectedLesson = lessons.getOrNull(selectedLessonIndex)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("School Timer", style = MaterialTheme.typography.titleMedium)
                Text("${schedule.values.sumOf { it.size }} lessons configured", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(selectedDay.czechLabel(), style = MaterialTheme.typography.headlineMedium, color = GlyphRed)
        }
        SchoolDaySelector(
            schedule = schedule,
            selectedDay = selectedDay,
            onSelect = { day ->
                selectedDay = day
                selectedLessonIndex = 0
            }
        )
        SchoolDayLessonList(
            day = selectedDay,
            lessons = lessons,
            selectedLessonIndex = selectedLessonIndex,
            onSelect = { selectedLessonIndex = it }
        )
        SchoolScheduleGrid(
            schedule = schedule,
            selectedDay = selectedDay,
            selectedLessonIndex = selectedLessonIndex,
            onSelect = { day, index ->
                selectedDay = day
                selectedLessonIndex = index.coerceAtLeast(0)
            }
        )
        if (selectedLesson != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlyphLine, MaterialTheme.shapes.small)
                    .background(GlyphPanel)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Lesson ${selectedLessonIndex + 1}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${selectedLesson.start}-${selectedLesson.end} / ${selectedLesson.weekMode.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlyphMuted
                        )
                    }
                    SchoolMatrixLabelPreview(selectedLesson.label)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedLesson.start,
                        onValueChange = { value ->
                            commit(schedule.withLesson(selectedDay, selectedLessonIndex, selectedLesson.copy(start = value.cleanTimeInput())))
                        },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectedLesson.end,
                        onValueChange = { value ->
                            commit(schedule.withLesson(selectedDay, selectedLessonIndex, selectedLesson.copy(end = value.cleanTimeInput())))
                        },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectedLesson.label,
                        onValueChange = { value ->
                            commit(schedule.withLesson(selectedDay, selectedLessonIndex, selectedLesson.copy(label = value.uppercase().filter(Char::isLetterOrDigit).take(3))))
                        },
                        label = { Text("Subj") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekModeUi.entries.forEach { mode ->
                        NothingButton(
                            mode.label,
                            active = selectedLesson.weekMode == mode,
                            onClick = {
                                commit(schedule.withLesson(selectedDay, selectedLessonIndex, selectedLesson.copy(weekMode = mode)))
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NothingButton("REMOVE", onClick = {
                        commit(schedule.withDayLessons(selectedDay, lessons.toMutableList().also { it.removeAt(selectedLessonIndex) }))
                        selectedLessonIndex = (selectedLessonIndex - 1).coerceAtLeast(0)
                    })
                    if (selectedLessonIndex > 0) {
                        NothingButton("UP", onClick = {
                            val nextLessons = lessons.toMutableList()
                            val moved = nextLessons.removeAt(selectedLessonIndex)
                            nextLessons.add(selectedLessonIndex - 1, moved)
                            commit(schedule.withDayLessons(selectedDay, nextLessons))
                            selectedLessonIndex -= 1
                        })
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlyphLine, MaterialTheme.shapes.small)
                    .background(GlyphPanel)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Free day", color = GlyphMuted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("ADD", onClick = {
                val nextLesson = nextLessonAfter(lessons)
                commit(schedule.withDayLessons(selectedDay, lessons + nextLesson))
                selectedLessonIndex = lessons.size
            })
            NothingButton("COPY MON", onClick = {
                commit(schedule.withDayLessons(selectedDay, schedule[DayOfWeek.MONDAY].orEmpty()))
                selectedLessonIndex = 0
            })
            NothingButton("CLEAR DAY", onClick = {
                commit(schedule.withDayLessons(selectedDay, emptyList()))
                selectedLessonIndex = 0
            })
        }
    }
}

@Composable
private fun SchoolDaySelector(
    schedule: Map<DayOfWeek, List<SchoolLessonUi>>,
    selectedDay: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleSchoolDays.forEach { day ->
            val active = day == selectedDay
            Column(
                modifier = Modifier
                    .width(74.dp)
                    .height(58.dp)
                    .border(1.dp, if (active) GlyphRed else GlyphLine, MaterialTheme.shapes.small)
                    .background(if (active) GlyphRed else GlyphPanel)
                    .clickable { onSelect(day) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(day.czechLabel(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${schedule[day].orEmpty().size} hod.", color = if (active) GlyphWhite else GlyphMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SchoolMatrixLabelPreview(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(SchoolMatrixText.displayLabel(label), color = GlyphRed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("MATRIX", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
        }
        MatrixPreview(
            frame = SchoolMatrixText.drawSubjectLabel(label, brightness = 100),
            active = true,
            modifier = Modifier.size(58.dp)
        )
    }
}

@Composable
private fun SchoolDayLessonList(
    day: DayOfWeek,
    lessons: List<SchoolLessonUi>,
    selectedLessonIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(day.longLabel(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        if (lessons.isEmpty()) {
            Text("No lessons", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        lessons.forEachIndexed { index, lesson ->
            val active = index == selectedLessonIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (active) GlyphRed else GlyphLine, MaterialTheme.shapes.small)
                    .background(if (active) GlyphPanelSoft else GlyphPanel)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(lesson.label, style = MaterialTheme.typography.titleMedium, color = if (active) GlyphRed else GlyphWhite)
                Column(modifier = Modifier.weight(1f)) {
                    Text("${lesson.start}-${lesson.end}", style = MaterialTheme.typography.bodySmall)
                    Text(lesson.weekMode.label, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text("#${index + 1}", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SchoolScheduleGrid(
    schedule: Map<DayOfWeek, List<SchoolLessonUi>>,
    selectedDay: DayOfWeek,
    selectedLessonIndex: Int,
    onSelect: (DayOfWeek, Int) -> Unit
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.horizontalScroll(scroll)) {
        Row {
            GridCell("", widthDp = 54, header = true)
            schoolPeriods.forEach { period ->
                GridCell("${period.index}\n${period.start}-${period.end}", widthDp = 112, header = true)
            }
        }
        visibleSchoolDays.forEach { day ->
            Row {
                GridCell(
                    day.czechLabel(),
                    widthDp = 54,
                    header = true,
                    active = selectedDay == day,
                    onClick = { onSelect(day, 0) }
                )
                schoolPeriods.forEach { period ->
                    val lessonIndex = schedule[day].orEmpty().indexOfFirst { it.overlaps(period.start, period.end) }
                    val lesson = schedule[day].orEmpty().getOrNull(lessonIndex)
                    val active = lessonIndex >= 0 && selectedDay == day && selectedLessonIndex == lessonIndex
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(72.dp)
                            .border(1.dp, if (active) GlyphRed else GlyphLine)
                            .background(if (active) GlyphRed else if (lesson != null) GlyphPanelSoft else GlyphPanel)
                            .clickable { onSelect(day, lessonIndex.coerceAtLeast(0)) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lesson != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    lesson.label,
                                    color = GlyphWhite,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    lesson.weekMode.label,
                                    color = if (active) GlyphWhite else GlyphMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    text: String,
    widthDp: Int,
    header: Boolean = false,
    active: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier
        .width(widthDp.dp)
        .height(if (header) 52.dp else 72.dp)
        .border(1.dp, GlyphLine)
        .background(if (active) GlyphRed else if (header) GlyphPanelSoft else GlyphPanel)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(4.dp)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun PermissionSetupPanel(moduleId: String, refreshToken: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val permission = when (moduleId) {
        "tuner",
        "sound_meter" -> Manifest.permission.RECORD_AUDIO
        "motion" -> Manifest.permission.ACTIVITY_RECOGNITION
        else -> null
    } ?: return
    val label = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Microphone"
        Manifest.permission.ACTIVITY_RECOGNITION -> "Activity recognition"
        else -> "Permission"
    }
    val description = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Required only for local microphone audio tools."
        Manifest.permission.ACTIVITY_RECOGNITION -> "Used for step detector/counter when the device exposes it."
        else -> "Required for this tool."
    }
    val granted = remember(permission, refreshToken) {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onChanged()
    }
    SettingsRow(label, if (granted) "Granted" else description) {
        NothingButton(
            text = if (granted) "GRANTED" else "ALLOW",
            enabled = !granted,
            onClick = { launcher.launch(permission) }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    entries: List<ToySettingDefinition>,
    settings: com.pelikan.glyphhub.settings.ToySettings,
    onValueChange: (ToySettingDefinition, String) -> Unit
) {
    if (entries.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    entries.forEach { definition ->
        SettingControl(
            definition = definition,
            value = settings.values[definition.key] ?: definition.defaultValue,
            onValueChange = { value -> onValueChange(definition, value) }
        )
    }
}

@Composable
private fun PixelArtEditor(
    encodedRows: String,
    fallbackRows: List<String>,
    onRowsChanged: (String) -> Unit
) {
    var rows by remember(encodedRows, fallbackRows) {
        mutableStateOf(normalizeRows(encodedRows, fallbackRows))
    }
    fun commit(nextRows: List<String>) {
        rows = GlyphIconLibrary.normalizeRows(nextRows)
        onRowsChanged(GlyphIconLibrary.encodeRows(rows))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom Matrix Icon", style = MaterialTheme.typography.bodyLarge)
        EditablePixelGrid(
            frame = GlyphFrame.fromBinaryRows(rows, brightness = 80),
            modifier = Modifier.fillMaxWidth(0.72f)
        ) { x, y ->
            rows = rows.mapIndexed { rowIndex, row ->
                if (rowIndex != y) row else row.mapIndexed { colIndex, char ->
                    if (colIndex == x) {
                        if (char == '1') '0' else '1'
                    } else {
                        char
                    }
                }.joinToString("")
            }
            onRowsChanged(GlyphIconLibrary.encodeRows(rows))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("CLEAR", onClick = {
                commit(List(GlyphFrame.MATRIX_SIZE) { "0".repeat(GlyphFrame.MATRIX_SIZE) })
            })
            NothingButton("FILL", onClick = {
                commit(List(GlyphFrame.MATRIX_SIZE) { y ->
                    buildString {
                        for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                            append(if (com.pelikan.glyphhub.glyph.GlyphMatrixLayout.isPhysicalLed(x, y)) '1' else '0')
                        }
                    }
                })
            })
            NothingButton("INVERT", onClick = {
                commit(rows.map { row ->
                    row.map { if (it == '1') '0' else '1' }.joinToString("")
                })
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NothingButton("MIRROR", onClick = {
                commit(rows.map { it.reversed() })
            })
            NothingButton("ROTATE", onClick = {
                commit(List(GlyphFrame.MATRIX_SIZE) { y ->
                    buildString {
                        for (x in 0 until GlyphFrame.MATRIX_SIZE) {
                            append(rows[GlyphFrame.MATRIX_SIZE - 1 - x][y])
                        }
                    }
                })
            })
            NothingButton("SHIFT", onClick = {
                commit(rows.map { row -> row.last() + row.dropLast(1) })
            })
        }
        Text("13x13 physical Glyph layout", color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun normalizeRows(encodedRows: String, fallbackRows: List<String>): List<String> {
    val parsed = encodedRows.split("/")
        .filter { it.length == GlyphFrame.MATRIX_SIZE }
        .take(GlyphFrame.MATRIX_SIZE)
    return if (parsed.size == GlyphFrame.MATRIX_SIZE) {
        parsed.map { row -> row.map { if (it == '1') '1' else '0' }.joinToString("") }
    } else {
        GlyphIconLibrary.normalizeRows(fallbackRows)
    }
}

private data class SchoolLessonUi(
    val start: String,
    val end: String,
    val label: String,
    val weekMode: WeekModeUi = WeekModeUi.All
)

private enum class WeekModeUi(val id: String, val label: String) {
    All("ALL", "ALL"),
    Even("EVEN", "EVEN"),
    Odd("ODD", "ODD");

    companion object {
        fun fromId(value: String?): WeekModeUi =
            entries.firstOrNull { it.id.equals(value.orEmpty(), ignoreCase = true) } ?: All
    }
}

private data class SchoolPeriodUi(
    val index: Int,
    val start: String,
    val end: String
)

private fun parseSchoolSchedule(encoded: String): Map<DayOfWeek, List<SchoolLessonUi>> {
    if (encoded.isBlank()) return defaultSchoolSchedule()
    return encoded.split(';')
        .mapNotNull { dayBlock ->
            val day = dayFromId(dayBlock.substringBefore('=').trim()) ?: return@mapNotNull null
            val lessons = dayBlock.substringAfter('=', "")
                .split(',', '|')
                .mapNotNull { lesson ->
                    val match = schoolLessonPattern.matchEntire(lesson.trim()) ?: return@mapNotNull null
                    val start = match.groupValues[1].cleanTimeInput()
                    val end = match.groupValues[2].cleanTimeInput()
                    val label = match.groupValues[3].uppercase().filter(Char::isLetterOrDigit).take(3).ifBlank { "CLS" }
                    val weekMode = WeekModeUi.fromId(match.groupValues.getOrNull(4))
                    if (start.length < 4 || end.length < 4) null else SchoolLessonUi(start, end, label, weekMode)
                }
            day to lessons
        }
        .toMap()
        .ifEmpty { defaultSchoolSchedule() }
}

private fun encodeSchoolSchedule(schedule: Map<DayOfWeek, List<SchoolLessonUi>>): String =
    DayOfWeek.entries.joinToString(";") { day ->
        val lessons = schedule[day].orEmpty().joinToString(",") { lesson ->
            "${lesson.start}-${lesson.end}:${lesson.label.ifBlank { "CLS" }}:${lesson.weekMode.id}"
        }
        "${day.shortLabel()}=$lessons"
    }

private fun defaultSchoolSchedule(): Map<DayOfWeek, List<SchoolLessonUi>> =
    buildMap {
        put(DayOfWeek.MONDAY, listOf(SchoolLessonUi("12:35", "13:20", "HV")))
        put(
            DayOfWeek.TUESDAY,
            listOf(
                SchoolLessonUi("11:40", "12:25", "IKT", WeekModeUi.Even),
                SchoolLessonUi("12:35", "13:20", "IKT", WeekModeUi.Even),
                SchoolLessonUi("13:30", "15:05", "PDT", WeekModeUi.Even)
            )
        )
        put(DayOfWeek.WEDNESDAY, listOf(SchoolLessonUi("11:40", "12:25", "F")))
        put(
            DayOfWeek.THURSDAY,
            listOf(
                SchoolLessonUi("10:45", "11:30", "F"),
                SchoolLessonUi("13:30", "14:15", "HV"),
                SchoolLessonUi("14:20", "15:05", "HV")
            )
        )
        put(DayOfWeek.FRIDAY, emptyList())
        put(DayOfWeek.SATURDAY, emptyList())
        put(DayOfWeek.SUNDAY, emptyList())
    }

private fun Map<DayOfWeek, List<SchoolLessonUi>>.withLesson(
    day: DayOfWeek,
    index: Int,
    lesson: SchoolLessonUi
): Map<DayOfWeek, List<SchoolLessonUi>> =
    withDayLessons(day, this[day].orEmpty().mapIndexed { itemIndex, item ->
        if (itemIndex == index) lesson else item
    })

private fun Map<DayOfWeek, List<SchoolLessonUi>>.withDayLessons(
    day: DayOfWeek,
    lessons: List<SchoolLessonUi>
): Map<DayOfWeek, List<SchoolLessonUi>> =
    this + (day to lessons.sortedBy { it.start })

private fun nextLessonAfter(lessons: List<SchoolLessonUi>): SchoolLessonUi {
    val previous = lessons.maxByOrNull { it.end } ?: return SchoolLessonUi("08:00", "08:45", "M")
    val start = addMinutes(previous.end, 10)
    val end = addMinutes(start, 45)
    return SchoolLessonUi(start, end, "CLS")
}

private fun addMinutes(value: String, minutes: Int): String {
    val parts = value.split(':')
    val rawHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val rawMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val total = (rawHour * 60 + rawMinute + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun String.cleanTimeInput(): String {
    val filtered = filter { it.isDigit() || it == ':' || it == '.' }.replace('.', ':').take(5)
    if (filtered.length == 4 && ':' !in filtered) return filtered.substring(0, 2) + ":" + filtered.substring(2)
    return filtered
}

private fun DayOfWeek.shortLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "MON"
        DayOfWeek.TUESDAY -> "TUE"
        DayOfWeek.WEDNESDAY -> "WED"
        DayOfWeek.THURSDAY -> "THU"
        DayOfWeek.FRIDAY -> "FRI"
        DayOfWeek.SATURDAY -> "SAT"
        DayOfWeek.SUNDAY -> "SUN"
    }

private fun DayOfWeek.longLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }

private fun dayFromId(value: String): DayOfWeek? =
    DayOfWeek.entries.firstOrNull { it.shortLabel().equals(value, ignoreCase = true) }

private fun SchoolLessonUi.overlaps(start: String, end: String): Boolean =
    timeToMinutes(this.start) < timeToMinutes(end) && timeToMinutes(this.end) > timeToMinutes(start)

private fun timeToMinutes(value: String): Int {
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (hour * 60 + minute).coerceIn(0, 23 * 60 + 59)
}

private fun DayOfWeek.czechLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "Po"
        DayOfWeek.TUESDAY -> "Ut"
        DayOfWeek.WEDNESDAY -> "St"
        DayOfWeek.THURSDAY -> "Ct"
        DayOfWeek.FRIDAY -> "Pa"
        DayOfWeek.SATURDAY -> "So"
        DayOfWeek.SUNDAY -> "Ne"
    }

private val visibleSchoolDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

private val schoolPeriods = listOf(
    SchoolPeriodUi(0, "07:00", "07:45"),
    SchoolPeriodUi(1, "07:50", "08:35"),
    SchoolPeriodUi(2, "08:45", "09:30"),
    SchoolPeriodUi(3, "09:50", "10:35"),
    SchoolPeriodUi(4, "10:45", "11:30"),
    SchoolPeriodUi(5, "11:40", "12:25"),
    SchoolPeriodUi(6, "12:35", "13:20"),
    SchoolPeriodUi(7, "13:30", "14:15"),
    SchoolPeriodUi(8, "14:20", "15:05")
)

private val schoolLessonPattern =
    Regex("""(\d{1,2}[:.]\d{2})-(\d{1,2}[:.]\d{2}):([^:;,|]+)(?::(ALL|EVEN|ODD))?""", RegexOption.IGNORE_CASE)

@Composable
private fun SettingControl(
    definition: ToySettingDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (definition.type) {
        ToySettingType.Boolean -> SettingsRow(definition.title, definition.description) {
            NothingSwitch(value.toBooleanStrictOrNull() ?: false) { onValueChange(it.toString()) }
        }
        ToySettingType.Int -> Column(modifier = Modifier.fillMaxWidth()) {
            Text("${definition.title}: $value", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = value.toFloatOrNull() ?: definition.defaultValue.toFloat(),
                onValueChange = { onValueChange(it.toInt().toString()) },
                valueRange = (definition.min ?: 0).toFloat()..(definition.max ?: 100).toFloat(),
                steps = ((definition.max ?: 100) - (definition.min ?: 0) - 1).coerceAtLeast(0)
            )
            Text(definition.description, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
        }
        ToySettingType.Text -> if (definition.key.isTransitionOverrideKey()) {
            TransitionOverrideControl(definition, value, onValueChange)
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(definition.title) },
                supportingText = { Text(definition.description) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        ToySettingType.Choice -> ChoiceControl(definition, value, onValueChange)
    }
}

@Composable
private fun TransitionOverrideControl(
    definition: ToySettingDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (value.isBlank()) "Global default" else value
    Column {
        Text(definition.title, style = MaterialTheme.typography.bodyLarge)
        NothingButton(selectedLabel, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Global default") },
                onClick = {
                    expanded = false
                    onValueChange("")
                }
            )
            GlyphTransitionEngine.transitionIds.forEach { transitionId ->
                DropdownMenuItem(
                    text = { Text(transitionId) },
                    onClick = {
                        expanded = false
                        onValueChange(transitionId)
                    }
                )
            }
        }
        Text(definition.description, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun String.isTransitionOverrideKey(): Boolean =
    this == "activationAnimationOverride" || this == "deactivationAnimationOverride"

@Composable
private fun ChoiceControl(
    definition: ToySettingDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(definition.title, style = MaterialTheme.typography.bodyLarge)
        NothingButton(
            text = definition.options.firstOrNull { it.id == value }?.label ?: value,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            definition.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onValueChange(option.id)
                    }
                )
            }
        }
        Text(definition.description, color = GlyphMuted, style = MaterialTheme.typography.bodySmall)
    }
}
