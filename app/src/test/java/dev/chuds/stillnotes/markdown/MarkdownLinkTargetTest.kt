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
        assertFalse(isBrowserUrl("javascript:alert(1)"))
        assertFalse(isBrowserUrl("file:///etc/passwd"))
        assertFalse(isBrowserUrl("/relative/path"))
        assertFalse(isBrowserUrl(""))
    }

    @Test
    fun browserUrlsAllowSpacesAndNonAsciiInThePath() {
        assertTrue(isBrowserUrl("https://example.com/a b"))
        assertTrue(isBrowserUrl("https://en.wikipedia.org/wiki/Foo Bar"))
        assertTrue(isBrowserUrl("https://example.com/café"))
        assertTrue(isBrowserUrl("HTTPS://EXAMPLE.COM/UPPER"))
    }
}
