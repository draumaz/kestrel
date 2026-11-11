package xyz.draumaz.kestrel.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.draumaz.kestrel.app
import xyz.draumaz.kestrel.feature.home.icon
import xyz.draumaz.kestrel.settings.SideBarItem
import xyz.draumaz.kestrel.ui.Overlays
import xyz.draumaz.kestrel.ui.switchThumb

/**
 * Settings › Side bar entries (#109): which entries show under the side bar's
 * tabs, and in what order. Each change applies at once. Settings can't be
 * switched off: the side bar may be the only way back to it.
 */
internal fun showSideBarItems() {
    Overlays.launch { dismiss ->
        OverlaySheet<Unit>(done = { dismiss() }) { _ ->
            val s by app.settings.state.collectAsStateWithLifecycle()
            val items = s.sideBarItems
            SectionHeader("Side bar entries", "Shown under the tabs, in this order")
            items.forEachIndexed { i, entry ->
                key(entry.item) {
                    val locked = entry.item == SideBarItem.SETTINGS
                    SettingTile(
                        entry.item.label,
                        subtitle = when {
                            locked -> "Always shown"
                            entry.item == SideBarItem.RECENTLY_VISITED && !s.showRecentlyVisited ->
                                "Hidden everywhere while Data & storage › Recently visited is off"
                            else -> null
                        },
                        icon = entry.item.icon,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { app.settings.setSideBarItems(items.toMutableList().apply { add(i - 1, removeAt(i)) }) },
                                    enabled = i > 0,
                                ) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                                IconButton(
                                    onClick = { app.settings.setSideBarItems(items.toMutableList().apply { add(i + 1, removeAt(i)) }) },
                                    enabled = i < items.lastIndex,
                                ) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                                Switch(
                                    checked = entry.shown,
                                    onCheckedChange = { v -> app.settings.setSideBarItems(items.toMutableList().apply { set(i, entry.copy(shown = v)) }) },
                                    enabled = !locked,
                                    thumbContent = switchThumb(entry.shown),
                                )
                            }
                        },
                    )
                }
            }
            TextButton(onClick = { app.settings.setSideBarItems(null) }, Modifier.padding(start = 12.dp)) { Text("Reset to default") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
