package dev.chuds.stillnotes.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownBlocksTest {
    @Test
    fun parsesHeadingLevelsOneThroughThree() {
        val blocks = parseBlocks(
            """
            # One
            ## Two
            ### Three
            """.trimIndent()
        )

        assertEquals(
            listOf(
                MdBlock.Heading(level = 1, text = "One"),
                MdBlock.Heading(level = 2, text = "Two"),
                MdBlock.Heading(level = 3, text = "Three"),
            ),
            blocks,
        )
    }

    @Test
    fun parsesListsBlockquoteFencedCodeAndHorizontalRule() {
        val blocks = parseBlocks(
            """
            - one
            * two

            1. first
            2) second

            > quoted
            > continued

            ```kotlin
            val answer = 42
            println(answer)
            ```

            ---
            """.trimIndent()
        )

        assertEquals(
            listOf(
                MdBlock.BulletList(listOf("one", "two")),
                MdBlock.OrderedList(listOf("first", "second")),
                MdBlock.BlockQuote("quoted\ncontinued"),
                MdBlock.CodeBlock(
                    language = "kotlin",
                    code = "val answer = 42\nprintln(answer)",
                ),
                MdBlock.Rule,
            ),
            blocks,
        )
    }
}
