package xyz.draumaz.kestrel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.draumaz.kestrel.core.redditUrlInText
import xyz.draumaz.kestrel.feature.post.SharedSummarySheet
import xyz.draumaz.kestrel.ui.theme.KestrelTheme

/**
 * "Summarize with Kestrel" in the share sheet: the summary sheet over the app the
 * post was shared from, without opening Kestrel (a see-through window).
 */
class SummarizeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val link = redditUrlInText(intent.getStringExtra(Intent.EXTRA_TEXT))
        setContent {
            val settings by app.settings.state.collectAsStateWithLifecycle()
            KestrelTheme(settings) {
                SharedSummarySheet(
                    link,
                    onOpen = { post ->
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com${post.permalink}"), this, KestrelActivity::class.java),
                        )
                        finish()
                    },
                    done = ::finish,
                )
            }
        }
    }
}
