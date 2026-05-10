package dev.chuds.stillnotes.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helpers for the SAF flows wired up in StillNotesApp. Keeping them here means the
 * Compose layer doesn't need to know about ContentResolver mechanics.
 */

suspend fun writeBodyToUri(
    context: Context,
    uri: Uri,
    body: String,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        } ?: return@runCatching false
        true
    }.getOrElse {
        toastOnMain(context, "export failed")
        false
    }
}

suspend fun writeZipToUri(
    context: Context,
    uri: Uri,
    repository: NotesRepository,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            repository.exportZip(stream)
        } ?: return@runCatching false
        true
    }.getOrElse {
        toastOnMain(context, "bulk export failed")
        false
    }
}

suspend fun importFromUris(
    context: Context,
    uris: List<Uri>,
    repository: NotesRepository,
): Int = withContext(Dispatchers.IO) {
    var imported = 0
    uris.forEach { uri ->
        val text = readTextFromUri(context.contentResolver, uri) ?: return@forEach
        repository.importBody(text)
        imported++
    }
    imported
}

private fun readTextFromUri(resolver: ContentResolver, uri: Uri): String? = runCatching {
    resolver.openInputStream(uri)?.use { stream ->
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}.getOrNull()

private fun toastOnMain(context: Context, message: String) {
    android.os.Handler(context.mainLooper).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Build a share intent for a note body. Caller wraps with Intent.createChooser.
 */
fun buildShareIntent(title: String, body: String): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, body)
    putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "untitled" })
}
