package dev.chuds.stillnotes.markdown

/**
 * Block-level model for the in-house markdown renderer.
 *
 * Deliberately small: headings, paragraphs, lists, blockquotes, code blocks, and rules.
 * Inline formatting (bold, italic, inline code, links) is parsed on demand by [parseInline].
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class BulletList(val items: List<String>) : MdBlock
    data class OrderedList(val items: List<String>) : MdBlock
    data class BlockQuote(val text: String) : MdBlock
    data class CodeBlock(val language: String?, val code: String) : MdBlock
    data object Rule : MdBlock
}

private val FENCE = Regex("^```\\s*([A-Za-z0-9_+-]*)\\s*$")
private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^\\s*[-*+]\\s+(.*)$")
private val ORDERED = Regex("^\\s*\\d+[.)]\\s+(.*)$")
private val QUOTE = Regex("^\\s*>\\s?(.*)$")
private val RULE = Regex("^\\s*(-{3,}|\\*{3,}|_{3,})\\s*$")

/**
 * Parse markdown source into a flat list of blocks. Tolerant: anything that doesn't match
 * a structural rule becomes a paragraph. Adjacent paragraph lines join with a space.
 */
fun parseBlocks(source: String): List<MdBlock> {
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val out = mutableListOf<MdBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        val fenceMatch = FENCE.matchEntire(line.trimEnd())
        if (fenceMatch != null) {
            val lang = fenceMatch.groupValues[1].ifEmpty { null }
            val buf = StringBuilder()
            i++
            while (i < lines.size && FENCE.matchEntire(lines[i].trimEnd()) == null) {
                if (buf.isNotEmpty()) buf.append('\n')
                buf.append(lines[i])
                i++
            }
            if (i < lines.size) i++ // consume closing fence
            out += MdBlock.CodeBlock(language = lang, code = buf.toString())
            continue
        }

        if (line.isBlank()) {
            i++
            continue
        }

        if (RULE.matches(line.trim())) {
            out += MdBlock.Rule
            i++
            continue
        }

        val headingMatch = HEADING.matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            out += MdBlock.Heading(level = level, text = headingMatch.groupValues[2].trim())
            i++
            continue
        }

        if (BULLET.matches(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val m = BULLET.matchEntire(lines[i]) ?: break
                items += m.groupValues[1].trim()
                i++
            }
            out += MdBlock.BulletList(items)
            continue
        }

        if (ORDERED.matches(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val m = ORDERED.matchEntire(lines[i]) ?: break
                items += m.groupValues[1].trim()
                i++
            }
            out += MdBlock.OrderedList(items)
            continue
        }

        if (QUOTE.matches(line)) {
            val buf = StringBuilder()
            while (i < lines.size) {
                val m = QUOTE.matchEntire(lines[i]) ?: break
                if (buf.isNotEmpty()) buf.append('\n')
                buf.append(m.groupValues[1])
                i++
            }
            out += MdBlock.BlockQuote(buf.toString().trimEnd())
            continue
        }

        // Paragraph — gather adjacent non-special lines.
        val buf = StringBuilder(line.trim())
        i++
        while (i < lines.size) {
            val next = lines[i]
            if (next.isBlank()) break
            if (HEADING.matches(next)) break
            if (BULLET.matches(next)) break
            if (ORDERED.matches(next)) break
            if (QUOTE.matches(next)) break
            if (RULE.matches(next.trim())) break
            if (FENCE.matchEntire(next.trimEnd()) != null) break
            buf.append(' ').append(next.trim())
            i++
        }
        out += MdBlock.Paragraph(buf.toString().trim())
    }

    return out
}
