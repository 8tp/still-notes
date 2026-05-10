package dev.chuds.stillnotes.data

/**
 * A single note. Body lives on disk at filesDir/notes/<id>.md; everything else
 * is metadata persisted in the JSON index for fast list rendering.
 */
data class Note(
    val id: String,
    val title: String,
    val preview: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val tags: List<String>,
)

/**
 * Derive a clean title from the note body. Honors a leading "# heading" if present;
 * otherwise falls back to the first non-blank line, trimmed of common markdown noise.
 */
fun deriveTitle(body: String): String {
    val firstLine = body.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: ""
    return firstLine
        .removePrefix("######").removePrefix("#####").removePrefix("####")
        .removePrefix("###").removePrefix("##").removePrefix("#")
        .removePrefix(">")
        .trim()
        .take(120)
}

/**
 * Short body excerpt for the list view. Skips the title line and any leading blank lines,
 * collapses whitespace, and strips a few markdown markers so previews read like text.
 */
fun derivePreview(body: String): String {
    val lines = body.lineSequence().toList()
    val titleLineIndex = lines.indexOfFirst { it.trim().isNotEmpty() }
    val tail = if (titleLineIndex >= 0) lines.drop(titleLineIndex + 1) else lines
    val joined = tail.asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return joined
        .replace(Regex("^#+\\s*"), "")
        .replace(Regex("`+"), "")
        .replace(Regex("[*_]{1,3}"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(160)
}

/**
 * Tags are inline `#tag` markers within the body. We require a non-`#`, non-letter character
 * before the `#` so that headings (`# title`) and run-on text (`xyz#abc`) don't accidentally
 * register as tags. Tag bodies are letters, digits, dashes, and underscores.
 */
private val TAG_REGEX = Regex("(?:^|[^A-Za-z0-9_#])#([A-Za-z][A-Za-z0-9_-]{0,40})\\b")

fun extractTags(body: String): List<String> {
    val seen = LinkedHashSet<String>()
    TAG_REGEX.findAll(body).forEach { match ->
        // Skip "# heading" pattern: a hash at line start followed by a space.
        val tag = match.groupValues[1]
        seen += tag.lowercase()
    }
    return seen.toList()
}
