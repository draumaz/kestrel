package xyz.draumaz.kestrel.settings

import xyz.draumaz.kestrel.settings.TopBarMenuItem.AUTOPLAY
import xyz.draumaz.kestrel.settings.TopBarMenuItem.BROWSE_MEDIA
import xyz.draumaz.kestrel.settings.TopBarMenuItem.LAYOUT
import xyz.draumaz.kestrel.settings.TopBarMenuItem.NEW_POST
import xyz.draumaz.kestrel.settings.TopBarMenuItem.PROFILE
import xyz.draumaz.kestrel.settings.TopBarMenuItem.SEARCH
import xyz.draumaz.kestrel.settings.TopBarMenuItem.SETTINGS
import org.junit.Assert.assertEquals
import org.junit.Test

/** Top bar menu (#108) and Dim read posts → Off (#106). */
class TopBarMenuAndDimTest {
    private fun shown(vararg items: TopBarMenuItem) = items.map { TopBarMenuEntry(it, true) }

    @Test
    fun `defaults are the panel as it was`() {
        assertEquals(shown(SEARCH, NEW_POST, SETTINGS, BROWSE_MEDIA, LAYOUT, AUTOPLAY), Settings().topBarMenu(TopBarMode.EXPANDABLE))
        assertEquals(shown(PROFILE, SETTINGS, BROWSE_MEDIA, LAYOUT, AUTOPLAY), Settings().topBarMenu(TopBarMode.FULL))
    }

    @Test
    fun `stored order and hidden items round-trip`() {
        val stored = listOf("layout", "-search", "settings", "newPost", "-browseMedia", "autoplay")
        val parsed = TopBarMenuItem.parse(stored, TopBarMode.EXPANDABLE)
        assertEquals(
            listOf(
                TopBarMenuEntry(LAYOUT, true), TopBarMenuEntry(SEARCH, false), TopBarMenuEntry(SETTINGS, true),
                TopBarMenuEntry(NEW_POST, true), TopBarMenuEntry(BROWSE_MEDIA, false), TopBarMenuEntry(AUTOPLAY, true),
            ),
            parsed,
        )
        assertEquals(stored, TopBarMenuItem.toPrefs(parsed))
    }

    @Test
    fun `unknown and other-mode ids are dropped, missing ones appended in default order`() {
        // "future" isn't known; Search isn't offered in the Full bar's panel; Profile and
        // Browse media are missing, so they come back, shown, in their default order.
        val parsed = TopBarMenuItem.parse(listOf("autoplay", "future", "-future", "search", "layout", "layout", "settings"), TopBarMode.FULL)
        assertEquals(shown(AUTOPLAY, LAYOUT, SETTINGS, PROFILE, BROWSE_MEDIA), parsed)
    }

    @Test
    fun `settings can't be hidden`() {
        assertEquals(TopBarMenuEntry(SETTINGS, true), TopBarMenuItem.parse(listOf("-settings"), TopBarMode.EXPANDABLE).first())
    }

    @Test
    fun `item ids never change`() {
        assertEquals(
            listOf("search", "profile", "newPost", "settings", "browseMedia", "layout", "autoplay"),
            TopBarMenuItem.entries.map { it.id },
        )
    }

    @Test
    fun `dim mode keeps dimReadTextOnly's meaning`() {
        assertEquals(ReadDim.CARD, Settings().readDim)
        assertEquals(ReadDim.TEXT, Settings(dimReadTextOnly = true).readDim)
        assertEquals(ReadDim.OFF, Settings(dimRead = false).readDim)
        assertEquals(ReadDim.OFF, Settings(dimRead = false, dimReadTextOnly = true).readDim)
    }
}
