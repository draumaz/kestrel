package xyz.draumaz.kestrel.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import xyz.draumaz.kestrel.ui.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.draumaz.kestrel.BuildConfig
import xyz.draumaz.kestrel.app
import xyz.draumaz.kestrel.feature.feed.BloomTopBar
import xyz.draumaz.kestrel.nav.Route
import xyz.draumaz.kestrel.ui.Overlays

/** One release's highlights, as shown in What's new. */
class Release(val version: String, val date: String, val highlights: List<String>)

/**
 * What's new, newest first. Add the new release at the top with every release
 * (WhatsNewTest checks the newest entry is this build's version).
 */
val CHANGELOG = listOf(
    Release(
        "1.0.0", "Nov 11, 2026",
        listOf(
            "First release of Kestrel for Reddit",
        ),
    ),
)

private const val SEEN_PREF = "whatsNewSeen"

private fun parts(v: String) = v.substringBefore(' ').split('.').map { it.toIntOrNull() ?: 0 }

/** Whether release [a] (its first version, for a range) is newer than version [b]. */
internal fun newer(a: String, b: String): Boolean {
    val x = parts(a)
    val y = parts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
        if (d != 0) return d > 0
    }
    return false
}

/**
 * Once after an update: what changed since the version you last saw here
 * (or this release's, the first time). Never on a fresh install.
 */
@OptIn(ExperimentalMaterial3Api::class)
suspend fun maybeShowWhatsNew() {
    val current = BuildConfig.VERSION_NAME
    val seen = app.prefs.getString(SEEN_PREF)
    app.prefs.setString(SEEN_PREF, current)
    if (seen == current) return
    val releases = if (seen == null) {
        val info = runCatching { app.context.packageManager.getPackageInfo(app.context.packageName, 0) }.getOrNull()
        if (info == null || info.firstInstallTime == info.lastUpdateTime) return // a fresh install: nothing is "new"
        CHANGELOG.take(1)
    } else {
        CHANGELOG.filter { newer(it.version.substringAfterLast(' '), seen) }.take(3)
    }
    if (releases.isEmpty()) return
    val all = Overlays.show<Boolean> { done ->
        ModalBottomSheet(onDismissRequest = { done(false) }) {
            Column(
                Modifier.navigationBarsPadding().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            ) {
                Text("What's new in Kestrel $current", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                for (r in releases) ReleaseNotes(r, showVersion = releases.size > 1)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { done(true) }) { Text("All changes") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { done(false) }) { Text("Got it") }
                }
            }
        }
    }
    if (all == true) app.navigatorOrNull?.push(Route.WhatsNew)
}

@Composable
private fun ReleaseNotes(r: Release, showVersion: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 12.dp)) {
        if (showVersion) {
            Row {
                Text(r.version, fontWeight = FontWeight.Bold, color = cs.primary)
                Spacer(Modifier.width(8.dp))
                Text(r.date, fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
        }
        for (h in r.highlights) {
            Row(Modifier.padding(bottom = 6.dp)) {
                Text("•", color = cs.primary)
                Spacer(Modifier.width(10.dp))
                Text(h)
            }
        }
    }
}

/** Settings → About → What's new: every release's highlights. */
@Composable
fun WhatsNewScreen() {
    val cs = MaterialTheme.colorScheme
    Scaffold(topBar = { BloomTopBar("What's new") }, containerColor = cs.surface) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(CHANGELOG, key = { it.version }) { r ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerLow) {
                    Column(Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 6.dp)) { ReleaseNotes(r) }
                }
            }
        }
    }
}
