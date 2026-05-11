package dev.chuds.stillnotes.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.chuds.stillnotes.data.extractTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownInlineTest {
    @Test
    fun parsesBoldItalicInlineCodeAndLinkAnnotations() {
        val codeColor = Color(0xFFECECEC)
        val codeBackground = Color(0xFF202020)
        val linkColor = Color(0xFF80CBC4)
        val parsed = parseInline(
            source = "A **bold** and *italic* plus _em_ with `#code` and [site](https://example.com)",
            monoFont = null,
            codeColor = codeColor,
            codeBackground = codeBackground,
            linkColor = linkColor,
        )

        assertEquals("A bold and italic plus em with #code and site", parsed.text)
        assertTrue(parsed.hasSpan("bold") { it.fontWeight == FontWeight.Medium })
        assertTrue(parsed.hasSpan("italic") { it.fontStyle == FontStyle.Italic })
        assertTrue(parsed.hasSpan("#code") { it.color == codeColor && it.background == codeBackground })
        assertTrue(parsed.hasSpan("site") { it.color == linkColor })

        val linkStart = parsed.text.indexOf("site")
        val link = parsed.getStringAnnotations(
            tag = "URL",
            start = linkStart,
            end = linkStart + "site".length,
        ).single()
        assertEquals("https://example.com", link.item)
    }

    @Test
    fun extractTagsIgnoresHashMarkersInsideInlineCodeSpans() {
        val tags = extractTags("Keep #Public, ignore `#private #also_private`, then keep #later-tag.")

        assertEquals(listOf("public", "later-tag"), tags)
    }

    @Test
    fun extractTagsIgnoresHashMarkersInsideMultiBacktickInlineCodeSpans() {
        val tags = extractTags("Keep #public, ignore ``#private`` and ``one ` tick #also_private``, then #later.")

        assertEquals(listOf("public", "later"), tags)
    }

    @Test
    fun extractTagsIgnoresHashMarkersInsideMultilineInlineCodeSpans() {
        val tags = extractTags(
            """
            Keep #public before `#private
            #also_private` and #later.
            """.trimIndent(),
        )

        assertEquals(listOf("public", "later"), tags)
    }

    @Test
    fun extractTagsIgnoresHashMarkersInsideFencedCodeBlocks() {
        val tags = extractTags(
            """
            Keep #public.
            ```
            #private
            val x = "#also_private"
            ```
            Then keep #later.
            """.trimIndent(),
        )

        assertEquals(listOf("public", "later"), tags)
    }

    @Test
    fun extractTagsKeepsLiteralBackticksInsideFencedCodeBlocksMasked() {
        val tags = extractTags(
            """
            Keep #public.
            ```kotlin
            val literalFence = "``` #private"
            // #also_private
            ```
            Then keep #later.
            """.trimIndent(),
        )

        assertEquals(listOf("public", "later"), tags)
    }

    @Test
    fun extractTagsIgnoresHashMarkersInsideMarkdownLinkDestinations() {
        val tags = extractTags("[site](https://example.com/#private) #public")

        assertEquals(listOf("public"), tags)
    }

    @Test
    fun extractTagsKeepsLinkTextTagsWhileMaskingNestedLinkDestinations() {
        val tags = extractTags("[#visible](https://example.com/a_(b)#hidden) #later")

        assertEquals(listOf("visible", "later"), tags)
    }

    private fun AnnotatedString.hasSpan(
        text: String,
        predicate: (androidx.compose.ui.text.SpanStyle) -> Boolean,
    ): Boolean {
        val start = this.text.indexOf(text)
        val end = start + text.length
        return start >= 0 && spanStyles.any { span ->
            span.start == start && span.end == end && predicate(span.item)
        }
    }
}
