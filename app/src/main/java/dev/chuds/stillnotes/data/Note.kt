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
private val CODE_FENCE = Regex("^```\\s*([A-Za-z0-9_+-]*)\\s*$")

fun extractTags(body: String): List<String> {
    val seen = LinkedHashSet<String>()
    TAG_REGEX.findAll(body.withoutCodeSpans().withoutMarkdownLinkDestinations()).forEach { match ->
        // Skip "# heading" pattern: a hash at line start followed by a space.
        val tag = match.groupValues[1]
        seen += tag.lowercase()
    }
    return seen.toList()
}

private fun String.withoutCodeSpans(): String {
    val out = StringBuilder(length)
    val plain = StringBuilder()
    var lineStart = 0
    var inFence = false

    fun flushPlain() {
        if (plain.isEmpty()) return
        out.append(plain.toString().withoutInlineCodeSpans())
        plain.clear()
    }

    while (lineStart <= length) {
        val newline = indexOf('\n', startIndex = lineStart)
        val lineEnd = if (newline == -1) length else newline
        val line = substring(lineStart, lineEnd)
        val isFence = CODE_FENCE.matchEntire(line.trimEnd()) != null
        val wasInFence = inFence
        val appendedToPlain: Boolean

        when {
            isFence -> {
                flushPlain()
                repeat(line.length) { out.append(' ') }
                inFence = !inFence
                appendedToPlain = false
            }
            wasInFence -> {
                repeat(line.length) { out.append(' ') }
                appendedToPlain = false
            }
            line.isBlank() -> {
                flushPlain()
                out.append(line)
                appendedToPlain = false
            }
            else -> {
                plain.append(line)
                appendedToPlain = true
            }
        }

        if (newline == -1) break
        if (appendedToPlain) plain.append('\n') else out.append('\n')
        lineStart = newline + 1
    }

    flushPlain()
    return out.toString()
}

private fun String.withoutMarkdownLinkDestinations(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        if (this[i] == ']' && i + 1 < length && this[i + 1] == '(' && hasMarkdownLinkLabelStart(i)) {
            val destinationStart = i + 2
            val destinationEnd = findMarkdownLinkDestinationEnd(destinationStart)
            if (destinationEnd != -1) {
                out.append("](")
                for (j in destinationStart until destinationEnd) {
                    val c = this[j]
                    out.append(if (c == '\n' || c == '\r') c else ' ')
                }
                out.append(')')
                i = destinationEnd + 1
                continue
            }
        }
        out.append(this[i])
        i++
    }
    return out.toString()
}

private fun String.hasMarkdownLinkLabelStart(labelEnd: Int): Boolean {
    var i = labelEnd - 1
    while (i >= 0) {
        when (this[i]) {
            '[' -> return true
            ']', '\n', '\r' -> return false
        }
        i--
    }
    return false
}

private fun String.findMarkdownLinkDestinationEnd(start: Int): Int {
    var depth = 1
    var i = start
    while (i < length) {
        when (this[i]) {
            '\\' -> i += 2
            '\n', '\r' -> return -1
            '(' -> {
                depth++
                i++
            }
            ')' -> {
                depth--
                if (depth == 0) return i
                i++
            }
            else -> i++
        }
    }
    return -1
}

private fun String.withoutInlineCodeSpans(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        if (this[i] == '`') {
            val runLength = countBacktickRun(i)
            val closeStart = findClosingBacktickRun(i + runLength, runLength)
            if (closeStart != -1) {
                val closeEnd = closeStart + runLength
                repeat(closeEnd - i) { out.append(' ') }
                i = closeEnd
                continue
            }
        }
        out.append(this[i])
        i++
    }
    return out.toString()
}

private fun String.countBacktickRun(start: Int): Int {
    var end = start
    while (end < length && this[end] == '`') {
        end++
    }
    return end - start
}

private fun String.findClosingBacktickRun(start: Int, runLength: Int): Int {
    var i = start
    while (i < length) {
        if (this[i] != '`') {
            i++
            continue
        }

        val candidateStart = i
        val candidateLength = countBacktickRun(candidateStart)
        if (candidateLength == runLength) {
            return candidateStart
        }
        i = candidateStart + candidateLength
    }
    return -1
}
