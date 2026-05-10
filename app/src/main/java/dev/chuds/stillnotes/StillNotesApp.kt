package dev.chuds.stillnotes

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chuds.stillnotes.data.FontPreset
import dev.chuds.stillnotes.data.NotesRepository
import dev.chuds.stillnotes.data.NotesSettings
import dev.chuds.stillnotes.data.PreferencesRepository
import dev.chuds.stillnotes.data.buildShareIntent
import dev.chuds.stillnotes.data.deriveTitle
import dev.chuds.stillnotes.data.importFromUris
import dev.chuds.stillnotes.data.writeBodyToUri
import dev.chuds.stillnotes.data.writeZipToUri
import dev.chuds.stillnotes.ui.list.NotesListScreen
import dev.chuds.stillnotes.ui.list.NotesListViewModel
import dev.chuds.stillnotes.ui.note.NoteScreen
import dev.chuds.stillnotes.ui.note.NoteViewModel
import dev.chuds.stillnotes.ui.settings.SettingsScreen
import dev.chuds.stillnotes.ui.theme.LocalStillTypography
import dev.chuds.stillnotes.ui.theme.stillTypographyFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Top-level composable. Hand-rolled router: list / note / settings. Owns the SAF
 * launchers for export, bulk export, and import.
 */
@Composable
fun StillNotesApp(incomingSharedText: String? = null) {
    val context = LocalContext.current.applicationContext
    val notesRepository = remember(context) { NotesRepository(context) }
    val preferencesRepository = remember(context) { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val activityContext = LocalContext.current

    val settingsState = remember(preferencesRepository) {
        preferencesRepository.settings.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = NotesSettings(),
        )
    }
    val settings by settingsState.collectAsStateWithLifecycle()

    var route by remember { mutableStateOf<Route>(Route.List) }

    // SAF state — which note id is the export targeting; what bytes are pending.
    var exportTarget by remember { mutableStateOf<ExportTarget?>(null) }

    val exportNoteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        val target = exportTarget
        exportTarget = null
        if (uri != null && target is ExportTarget.Single) {
            scope.launch {
                val body = notesRepository.read(target.id)
                if (writeBodyToUri(activityContext, uri, body)) {
                    Toast.makeText(activityContext, "exported", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val target = exportTarget
        exportTarget = null
        if (uri != null && target is ExportTarget.All) {
            scope.launch {
                if (writeZipToUri(activityContext, uri, notesRepository)) {
                    Toast.makeText(activityContext, "exported all notes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val n = importFromUris(activityContext, uris, notesRepository)
                Toast.makeText(
                    activityContext,
                    "imported $n " + if (n == 1) "note" else "notes",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun startExport(id: String) {
        scope.launch {
            val note = notesRepository.notes.value.firstOrNull { it.id == id }
            val baseName = (note?.title ?: deriveTitle(notesRepository.read(id)))
                .ifBlank { "untitled" }
            val safe = baseName.replace(Regex("[^A-Za-z0-9._\\- ]"), "").trim().replace(' ', '-')
            exportTarget = ExportTarget.Single(id)
            exportNoteLauncher.launch(if (safe.isEmpty()) "note.md" else "$safe.md")
        }
    }

    fun startExportAll() {
        exportTarget = ExportTarget.All
        exportZipLauncher.launch("still-notes-${System.currentTimeMillis()}.zip")
    }

    fun startImport() {
        importLauncher.launch(arrayOf("text/markdown", "text/plain", "text/*", "application/octet-stream"))
    }

    fun shareNoteById(id: String) {
        scope.launch {
            val note = notesRepository.notes.value.firstOrNull { it.id == id }
            val body = notesRepository.read(id)
            val intent = buildShareIntent(note?.title ?: "untitled", body)
            runCatching {
                activityContext.startActivity(Intent.createChooser(intent, "share note"))
            }
        }
    }

    // Honor an incoming ACTION_SEND text/plain payload by creating a note from it once.
    LaunchedEffect(incomingSharedText) {
        val text = incomingSharedText?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val note = notesRepository.create(text)
        route = Route.Note(note.id, startInEdit = true)
    }

    BackHandler(enabled = route !is Route.List) {
        route = Route.List
    }

    val typography = remember(settings.fontPreset) { stillTypographyFor(settings.fontPreset) }

    CompositionLocalProvider(LocalStillTypography provides typography) {
        when (val current = route) {
            Route.List -> {
                val listViewModel: NotesListViewModel = viewModel(
                    factory = NotesListViewModel.factory(notesRepository),
                )
                NotesListScreen(
                    viewModel = listViewModel,
                    onOpenNote = { id ->
                        route = Route.Note(id, startInEdit = !settings.previewByDefault)
                    },
                    onOpenSettings = { route = Route.Settings },
                    onCreateNote = { id -> route = Route.Note(id, startInEdit = true) },
                    onExportNote = ::startExport,
                    onShareNote = ::shareNoteById,
                )
            }

            is Route.Note -> {
                val noteViewModel: NoteViewModel = viewModel(
                    key = "note:${current.id}",
                    factory = NoteViewModel.factory(notesRepository, current.id),
                )
                NoteScreen(
                    viewModel = noteViewModel,
                    startInEdit = current.startInEdit,
                    onBack = { route = Route.List },
                    onShare = { shareNoteById(current.id) },
                    onExport = { startExport(current.id) },
                )
            }

            Route.Settings -> {
                val notesCount by notesRepository.notes.collectAsState(initial = emptyList())
                SettingsScreen(
                    settings = settings,
                    notesCount = notesCount.size,
                    onCycleFontPreset = {
                        scope.launch {
                            val next = when (settings.fontPreset) {
                                FontPreset.System -> FontPreset.Editorial
                                FontPreset.Editorial -> FontPreset.Terminal
                                FontPreset.Terminal -> FontPreset.Grotesk
                                FontPreset.Grotesk -> FontPreset.System
                            }
                            preferencesRepository.setFontPreset(next)
                        }
                    },
                    onTogglePreviewByDefault = {
                        scope.launch {
                            preferencesRepository.setPreviewByDefault(!settings.previewByDefault)
                        }
                    },
                    onExportAll = ::startExportAll,
                    onImport = ::startImport,
                    onBack = { route = Route.List },
                )
            }
        }
    }
}

private sealed interface Route {
    data object List : Route
    data class Note(val id: String, val startInEdit: Boolean) : Route
    data object Settings : Route
}

private sealed interface ExportTarget {
    data class Single(val id: String) : ExportTarget
    data object All : ExportTarget
}

/**
 * Pulls the shared text out of an incoming ACTION_SEND intent if present.
 */
fun ComponentActivity.consumeSharedTextIfAny(): String? {
    val intent = intent ?: return null
    if (intent.action != Intent.ACTION_SEND) return null
    val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
    // Mark consumed so a configuration change doesn't re-fire it.
    intent.action = null
    intent.removeExtra(Intent.EXTRA_TEXT)
    return text
}

