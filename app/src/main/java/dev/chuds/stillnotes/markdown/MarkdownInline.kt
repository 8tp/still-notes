package dev.chuds.stillnotes.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Convert markdown inline formatting into an [AnnotatedString].
 *
 * Supported tokens:
 *   `code`          — monospace span on a faint background
 *   **bold**        — FontWeight.Medium (we keep weight conservative for OLED legibility)
 *   *italic*        — FontStyle.Italic
 *   _italic_        — same
 *   [text](url)     — text rendered with a hairline underline; URL stored as a string annotation
 *
 * Single-pass, character-by-character: predictable, no recursion, no third-party parser.
 */
fun parseInline(
    source: String,
    monoFont: FontFamily?,
    codeColor: Color,
    codeBackground: Color,
    linkColor: Color,
    inlineCodeFontSize: TextUnit? = null,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = source.length
    while (i < n) {
        val c = source[i]

        // Inline code: `foo bar` or ``foo`bar`` — match the indexer in Note.kt.
        if (c == '`') {
            val runLength = countBacktickRun(source, i)
            val closeStart = findClosingBacktickRun(source, i + runLength, runLength)
            if (closeStart != -1) {
                val text = source.substring(i + runLength, closeStart)
                pushStyle(
                    SpanStyle(
                        fontFamily = monoFont ?: FontFamily.Monospace,
                        background = codeBackground,
                        color = codeColor,
                        fontSize = inlineCodeFontSize ?: TextUnit.Unspecified,
                    )
                )
                append(text)
                pop()
                i = closeStart + runLength
                continue
            }
        }

        // Bold: **text**
        if (c == '*' && i + 1 < n && source[i + 1] == '*') {
            val end = findBoldEnd(source, i + 2)
            if (end != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Medium))
                append(parseInline(source.substring(i + 2, end), monoFont, codeColor, codeBackground, linkColor, inlineCodeFontSize))
                pop()
                i = end + 2
                continue
            }
        }

        // Italic: *text* or _text_
        if ((c == '*' || c == '_')) {
            val end = findItalicEnd(source, i + 1, c)
            if (end != -1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(parseInline(source.substring(i + 1, end), monoFont, codeColor, codeBackground, linkColor, inlineCodeFontSize))
                pop()
                i = end + 1
                continue
            }
        }

        // Link: [label](url)
        if (c == '[') {
            val labelEnd = source.indexOf(']', i + 1)
            if (labelEnd != -1 && labelEnd + 1 < n && source[labelEnd + 1] == '(') {
                val urlEnd = findLinkDestinationEnd(source, labelEnd + 2)
                if (urlEnd != -1) {
                    val label = source.substring(i + 1, labelEnd)
                    val url = source.substring(labelEnd + 2, urlEnd)
                    pushStringAnnotation(tag = "URL", annotation = url)
                    pushStyle(SpanStyle(color = linkColor))
                    append(label)
                    pop()
                    pop()
                    i = urlEnd + 1
                    continue
                }
            }
        }

        append(c)
        i++
    }
}

private fun findLinkDestinationEnd(source: String, from: Int): Int {
    var depth = 1
    var i = from
    while (i < source.length) {
        when (source[i]) {
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

private fun findBoldEnd(source: String, from: Int): Int {
    var i = from
    while (i < source.length - 1) {
        if (source[i] == '*' && source[i + 1] == '*') return i
        i++
    }
    return -1
}

private fun findItalicEnd(source: String, from: Int, marker: Char): Int {
    var i = from
    while (i < source.length) {
        val ch = source[i]
        if (ch == marker) {
            // Bold marker (**) wins — don't claim its first asterisk as a closer.
            if (marker == '*' && i + 1 < source.length && source[i + 1] == '*') {
                i += 2
                continue
            }
            return i
        }
        i++
    }
    return -1
}

private fun countBacktickRun(source: String, start: Int): Int {
    var end = start
    while (end < source.length && source[end] == '`') {
        end++
    }
    return end - start
}

private fun findClosingBacktickRun(source: String, start: Int, runLength: Int): Int {
    var i = start
    while (i < source.length) {
        if (source[i] != '`') {
            i++
            continue
        }
        val candidateStart = i
        val candidateLength = countBacktickRun(source, candidateStart)
        if (candidateLength == runLength) {
            return candidateStart
        }
        i = candidateStart + candidateLength
    }
    return -1
}
