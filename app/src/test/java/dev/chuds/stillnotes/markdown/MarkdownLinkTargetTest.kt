package dev.chuds.stillnotes.markdown

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLinkTargetTest {
    @Test
    fun browserUrlsAreLimitedToHttpAndHttps() {
        assertTrue(isBrowserUrl("https://example.com/a#b"))
        assertTrue(isBrowserUrl("http://example.com"))

        assertFalse(isBrowserUrl("tel:+15551234567"))
        assertFalse(isBrowserUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isBrowserUrl("content://com.example/item"))
        assertFalse(isBrowserUrl("/relative/path"))
    }
}
