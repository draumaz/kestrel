package xyz.draumaz.kestrel.feature.media

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedFileNameTest {
    @Test fun usesThePostTitle() {
        assertEquals("My cat does a backflip", savedFileName("My cat does a backflip", 1L))
    }

    @Test fun dropsCharactersFileSystemsRefuse() {
        assertEquals("AC DC live no", savedFileName("AC/DC live:\nno?", 1L))
        assertEquals("a b c d", savedFileName("a\\b*c|d", 1L))
    }

    @Test fun cutsLongTitlesAndTrailingDots() {
        assertEquals(80, savedFileName("x".repeat(200), 1L).length)
        assertEquals("Wait for it", savedFileName("Wait for it...", 1L))
    }

    @Test fun fallsBackWithoutATitle() {
        assertEquals("kestrel_42", savedFileName(null, 42L))
        assertEquals("kestrel_42", savedFileName("  ?? ", 42L))
    }
}
