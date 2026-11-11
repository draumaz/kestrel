package xyz.draumaz.kestrel.feature.inbox

import androidx.compose.runtime.Immutable
import xyz.draumaz.kestrel.core.AppJson
import xyz.draumaz.kestrel.core.arr
import xyz.draumaz.kestrel.core.get
import xyz.draumaz.kestrel.core.long
import xyz.draumaz.kestrel.core.storage.Prefs
import xyz.draumaz.kestrel.core.str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One notification Kestrel handled: what came in and what it did with it. */
@Immutable
data class NotificationEvent(
    val at: Long,
    val account: String,
    /** [SOURCE_INBOX]: Kestrel's own inbox check; [SOURCE_REDDIT]: one from the Reddit app. */
    val source: String,
    val title: String,
    val text: String,
    val outcome: Outcome,
    /** Where it opens: a reply / mention (subreddit, post, comment) or a message thread. */
    val subreddit: String? = null,
    val postId: String? = null,
    val commentId: String? = null,
    val messageFullname: String? = null,
    /** How a copied Reddit notification's destination was found ("your post, by its title"). */
    val how: String? = null,
    /** The extra fields Reddit's notification carried (names, and any link), to find an exact one. */
    val redditData: String? = null,
) {
    enum class Outcome(val label: String, val ok: Boolean) {
        SHOWN("Notified", true),
        NOT_SHOWN("Not shown: Kestrel's notifications are off or blocked", false),
        REPLACED("Reddit's replaced by Kestrel's", true),
        COPIED("Reddit's copied into Kestrel", true),
        KEPT("Reddit's kept: Kestrel can't show notifications", false),
    }

    companion object {
        const val SOURCE_INBOX = "inbox"
        const val SOURCE_REDDIT = "reddit"
    }
}

/**
 * The last notifications Kestrel showed, replaced or couldn't show (Settings →
 * Notification activity), plus the last inbox check: so a missed one can be
 * found and opened, and it's clear why. On this device only, all accounts
 * (each entry names its account).
 */
class NotificationLog(private val prefs: Prefs) {
    private val _events = MutableStateFlow(load())
    val events: StateFlow<List<NotificationEvent>> = _events

    /** The last inbox check: when, and what came of it. */
    private val _lastCheck = MutableStateFlow(
        prefs.getString(CHECK_KEY)?.let { raw ->
            raw.substringBefore('|').toLongOrNull()?.let { it to raw.substringAfter('|') }
        },
    )
    val lastCheck: StateFlow<Pair<Long, String>?> = _lastCheck

    @Synchronized
    fun add(e: NotificationEvent) {
        val next = (listOf(e) + _events.value).take(MAX)
        _events.value = next
        prefs.setString(KEY, JsonArray(next.map { it.toJson() }).toString())
    }

    fun checked(result: String) {
        val now = System.currentTimeMillis()
        _lastCheck.value = now to result
        prefs.setString(CHECK_KEY, "$now|$result")
    }

    fun clear() {
        _events.value = emptyList()
        prefs.remove(KEY)
    }

    private fun NotificationEvent.toJson() = buildJsonObject {
        put("at", at); put("acct", account); put("src", source); put("title", title); put("text", text)
        put("out", outcome.name)
        subreddit?.let { put("sub", it) }; postId?.let { put("post", it) }
        commentId?.let { put("comment", it) }; messageFullname?.let { put("msg", it) }
        how?.let { put("how", it) }; redditData?.let { put("data", it) }
    }

    private fun load(): List<NotificationEvent> = runCatching {
        AppJson.parseToJsonElement(prefs.getString(KEY) ?: return emptyList()).arr().orEmpty().mapNotNull { j ->
            NotificationEvent(
                at = j["at"].long() ?: return@mapNotNull null,
                account = j["acct"].str().orEmpty(),
                source = j["src"].str() ?: NotificationEvent.SOURCE_INBOX,
                title = j["title"].str().orEmpty(),
                text = j["text"].str().orEmpty(),
                outcome = runCatching { NotificationEvent.Outcome.valueOf(j["out"].str()!!) }.getOrNull() ?: return@mapNotNull null,
                subreddit = j["sub"].str(),
                postId = j["post"].str(),
                commentId = j["comment"].str(),
                messageFullname = j["msg"].str(),
                how = j["how"].str(),
                redditData = j["data"].str(),
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY = "notification_log"
        private const val CHECK_KEY = "notification_last_check"
        private const val MAX = 50
    }
}
