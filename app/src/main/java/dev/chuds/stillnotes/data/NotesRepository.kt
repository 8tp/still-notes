package dev.chuds.stillnotes.data

import android.content.Context
import java.io.File
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * File-backed notes store.
 *
 * Layout under filesDir:
 *   notes/<id>.md     — note body (plain UTF-8 markdown)
 *   index.json        — ordered list of metadata for fast list rendering
 */
class NotesRepository(context: Context) {

    private val notesDir: File = File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
    private val indexFile: File = File(context.filesDir, "index.json")
    private val ioMutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val loaded = readIndex() ?: rebuildIndexFromDisk()
            _notes.value = loaded.sortedDescending()
        }
    }

    suspend fun read(id: String): String = withContext(Dispatchers.IO) {
        val file = noteFile(id)
        if (file.exists()) file.readText() else ""
    }

    suspend fun create(initialBody: String = ""): Note = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val note = Note(
            id = id,
            title = deriveTitle(initialBody),
            preview = derivePreview(initialBody),
            createdAt = now,
            updatedAt = now,
            pinned = false,
            tags = extractTags(initialBody),
        )
        ioMutex.withLock {
            noteFile(id).writeText(initialBody)
            val updated = listOf(note) + _notes.value
            writeIndex(updated)
            _notes.value = updated.sortedDescending()
        }
        note
    }

    suspend fun save(id: String, body: String): Note = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        ioMutex.withLock {
            noteFile(id).writeText(body)
            val existing = _notes.value.firstOrNull { it.id == id }
            val createdAt = existing?.createdAt ?: now
            val pinned = existing?.pinned ?: false
            val refreshed = Note(
                id = id,
                title = deriveTitle(body),
                preview = derivePreview(body),
                createdAt = createdAt,
                updatedAt = now,
                pinned = pinned,
                tags = extractTags(body),
            )
            val next = _notes.value.filterNot { it.id == id } + refreshed
            writeIndex(next)
            _notes.value = next.sortedDescending()
            refreshed
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            noteFile(id).delete()
            val next = _notes.value.filterNot { it.id == id }
            writeIndex(next)
            _notes.value = next.sortedDescending()
        }
    }

    suspend fun togglePinned(id: String) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val existing = _notes.value.firstOrNull { it.id == id } ?: return@withLock
            val updated = existing.copy(pinned = !existing.pinned)
            val next = _notes.value.map { if (it.id == id) updated else it }
            writeIndex(next)
            _notes.value = next.sortedDescending()
        }
    }

    /**
     * Search across the index. Title matches rank above body matches; case-insensitive.
     * Returns notes in rank order (best match first), preserving the pinned bonus.
     */
    suspend fun search(query: String): List<Note> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext _notes.value
        _notes.value
            .map { note ->
                val metaScore = note.matchScore(q)
                val bodyScore = if (metaScore > 0) 0 else {
                    if (noteFile(note.id).takeIf { it.exists() }
                            ?.readText()?.lowercase()?.contains(q) == true) 1 else 0
                }
                note to (metaScore + bodyScore)
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Note, Int>> { it.first.pinned }
                    .thenByDescending { it.second }
                    .thenByDescending { it.first.updatedAt }
            )
            .map { it.first }
    }

    /**
     * Body bytes for export. Returns the raw markdown unchanged.
     */
    suspend fun bodyBytes(id: String): ByteArray = withContext(Dispatchers.IO) {
        val file = noteFile(id)
        if (file.exists()) file.readBytes() else ByteArray(0)
    }

    /**
     * Bulk export — write every note as <safe-title>.md inside a zip stream.
     * Caller supplies the [output] (typically a SAF-provided OutputStream).
     */
    suspend fun exportZip(output: OutputStream) = withContext(Dispatchers.IO) {
        ZipOutputStream(output).use { zip ->
            val seen = HashSet<String>()
            _notes.value.forEach { note ->
                val baseName = safeFilename(note.title.ifBlank { "untitled-${note.id.take(8)}" })
                var name = "$baseName.md"
                var counter = 1
                while (!seen.add(name)) {
                    name = "$baseName-${counter++}.md"
                }
                zip.putNextEntry(ZipEntry(name))
                zip.write(noteFile(note.id).takeIf { it.exists() }?.readBytes() ?: ByteArray(0))
                zip.closeEntry()
            }
        }
    }

    /**
     * Import a markdown body as a new note. Returns the new note id.
     * Caller is responsible for reading bytes from a SAF Uri and passing the text here.
     */
    suspend fun importBody(body: String): String = create(body).id

    private fun noteFile(id: String): File = File(notesDir, "$id.md")

    private fun readIndex(): List<Note>? {
        if (!indexFile.exists()) return null
        return runCatching {
            val text = indexFile.readText()
            if (text.isBlank()) return null
            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Note(
                    id = obj.getString("id"),
                    title = obj.optString("title"),
                    preview = obj.optString("preview"),
                    createdAt = obj.optLong("createdAt"),
                    updatedAt = obj.optLong("updatedAt"),
                    pinned = obj.optBoolean("pinned", false),
                    tags = obj.optJSONArray("tags")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                )
            }
        }.getOrNull()
    }

    private fun rebuildIndexFromDisk(): List<Note> {
        val notes = (notesDir.listFiles { f -> f.extension == "md" } ?: emptyArray()).map { file ->
            val body = runCatching { file.readText() }.getOrDefault("")
            val timestamp = file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
            Note(
                id = file.nameWithoutExtension,
                title = deriveTitle(body),
                preview = derivePreview(body),
                createdAt = timestamp,
                updatedAt = timestamp,
                pinned = false,
                tags = extractTags(body),
            )
        }
        writeIndex(notes)
        return notes
    }

    private fun writeIndex(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
                .put("id", note.id)
                .put("title", note.title)
                .put("preview", note.preview)
                .put("createdAt", note.createdAt)
                .put("updatedAt", note.updatedAt)
                .put("pinned", note.pinned)
                .put("tags", JSONArray(note.tags))
            array.put(obj)
        }
        indexFile.writeText(array.toString())
    }

    private fun List<Note>.sortedDescending(): List<Note> =
        sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })

    private fun Note.matchScore(q: String): Int {
        var score = 0
        val title = title.lowercase()
        val preview = preview.lowercase()
        if (title.contains(q)) score += 5
        if (preview.contains(q)) score += 2
        if (tags.any { it.contains(q) }) score += 3
        return score
    }

    private fun safeFilename(input: String): String {
        val cleaned = input.replace(Regex("[^A-Za-z0-9._\\- ]"), "").trim()
        return (if (cleaned.isEmpty()) "note" else cleaned).take(80).replace(' ', '-')
    }
}
