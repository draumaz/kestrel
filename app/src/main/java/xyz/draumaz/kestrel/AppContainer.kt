package xyz.draumaz.kestrel

import xyz.draumaz.kestrel.state.RecentSubreddits
import xyz.draumaz.kestrel.feature.inbox.RedditNotifications
import android.content.Context
import xyz.draumaz.kestrel.auth.AuthRepository
import xyz.draumaz.kestrel.auth.SessionManager
import xyz.draumaz.kestrel.auth.SessionState
import xyz.draumaz.kestrel.core.net.RateLimit
import xyz.draumaz.kestrel.core.net.RedditClient
import xyz.draumaz.kestrel.core.net.ResponseCache
import xyz.draumaz.kestrel.core.storage.Prefs
import xyz.draumaz.kestrel.core.storage.SecureStore
import xyz.draumaz.kestrel.data.RedditRepository
import xyz.draumaz.kestrel.feature.feed.FeedModule
import xyz.draumaz.kestrel.feature.foryou.ForYouModule
import xyz.draumaz.kestrel.feature.inbox.InboxModule
import xyz.draumaz.kestrel.feature.media.MediaModule
import xyz.draumaz.kestrel.feature.post.PostModule
import xyz.draumaz.kestrel.nav.AppNavigator
import xyz.draumaz.kestrel.settings.SettingsStore
import xyz.draumaz.kestrel.state.ContentFiltersStore
import xyz.draumaz.kestrel.state.Drafts
import xyz.draumaz.kestrel.state.ExpiringIds
import xyz.draumaz.kestrel.state.SummaryStore
import xyz.draumaz.kestrel.state.HistoryStore
import xyz.draumaz.kestrel.state.OfflineStore
import xyz.draumaz.kestrel.state.CommentVotes
import xyz.draumaz.kestrel.state.PostOverrides
import xyz.draumaz.kestrel.state.ThreadVisits
import xyz.draumaz.kestrel.state.UserScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The app's singletons (manual DI). Reach it with the global [app]. */
class AppContainer(val context: Context) {
    /** Lives as long as the process (background work, stores). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * This process was started to restore the app after Android killed it in
     * the background (not a fresh launch): feeds show their saved page if it's
     * still fresh rather than loading anew.
     */
    @Volatile var restoredProcess = false

    val prefs = Prefs(context)
    val secureStore = SecureStore(context)
    val settings = SettingsStore(prefs)
    val authRepository = AuthRepository(secureStore)
    val session = SessionManager(secureStore, authRepository, prefs, scope)

    /** Latest Reddit API rate-limit snapshot. */
    val rateLimit = MutableStateFlow<RateLimit?>(null)

    val client = RedditClient(
        store = secureStore,
        auth = authRepository,
        cache = ResponseCache(context.cacheDir),
        onRateLimit = { rateLimit.value = it },
        cacheEnabled = { settings.value.offlineCache },
    )
    val repository = RedditRepository(client)

    // Shared, per-account local state.
    val postOverrides = PostOverrides()
    val commentVotes = CommentVotes()
    val authorAvatars = xyz.draumaz.kestrel.state.AuthorAvatars()
    /** Reddit's own notification list (website sign-in), read like Home. */
    val redditNotifications = RedditNotifications(this)
    /**
     * Posts hidden with Hide (also hidden on Reddit). Kept on the device too —
     * Home's id-fetched posts and saved first pages don't honour Reddit's
     * hidden flag, so a session-only set let them reappear after a restart.
     */
    val hiddenPosts = ExpiringIds(prefs, "hiddenPostIds") { settings.value.hiddenPostDays * 24L * 60 * 60_000L }
    /** Posts swiped away from Home / For You ("Dismiss"), for a day. */
    val dismissedPosts = ExpiringIds(prefs, "dismissedPosts") { 24 * 60 * 60_000L }
    /** Subreddits hidden from Home / For You for a week (lowercase names). */
    val hiddenHomeSubs = ExpiringIds(prefs, "hiddenHomeSubs") { 7 * 24 * 60 * 60_000L }
    val summaries = SummaryStore(context)
    val history = HistoryStore(prefs)
    val recentSubreddits = RecentSubreddits(prefs) { settings.value.recentSubsByVisits }
    val threadVisits = ThreadVisits(prefs, settings)
    val contentFilters = ContentFiltersStore(prefs)
    val offline = OfflineStore(context, prefs, repository)
    val drafts = Drafts(prefs) { session.username }
    val savedSearches = xyz.draumaz.kestrel.state.SavedSearches(prefs) { session.username }
    val notificationLog = xyz.draumaz.kestrel.feature.inbox.NotificationLog(prefs)

    // Feature modules (each owns its feature's singletons).
    val feed by lazy { FeedModule(this) }
    val post by lazy { PostModule(this) }
    val forYou by lazy { ForYouModule(this) }
    val inbox by lazy { InboxModule(this) }
    val media by lazy { MediaModule(this) }

    /** The identity whose per-account stores are loaded (after a login/switch settles). */
    val loadedIdentity = MutableStateFlow<String?>(null)

    /** Set by KestrelActivity while it's alive. */
    @Volatile var navigatorOrNull: AppNavigator? = null
    val navigator: AppNavigator get() = navigatorOrNull ?: error("No activity")

    private val userScoped: List<UserScoped>
        get() = listOf(postOverrides, commentVotes, redditNotifications, hiddenPosts, dismissedPosts, hiddenHomeSubs, summaries, history, recentSubreddits, threadVisits, contentFilters, offline, feed, post, forYou, inbox, media)

    /** Called once by [KestrelApp] after [app] is assigned (modules may use it). */
    fun start() {
        // Load per-account stores for the logged-out identity right away, so
        // they're usable before the session finishes loading.
        userScoped.forEach { it.onUserChanged("") }
        settings.state.map { it.subsCacheEnabled to it.subsCacheMinutes }.distinctUntilChanged().let { flow ->
            scope.launch {
                flow.collect { (on, minutes) ->
                    repository.subsCacheEnabled = on
                    repository.subsCacheTtlMillis = minutes * 60_000L
                }
            }
        }
        // A new "Hide posts for" time applies at once (a shorter one un-hides now).
        scope.launch { settings.state.map { it.hiddenPostDays }.distinctUntilChanged().drop(1).collect { hiddenPosts.refresh() } }
        // Recently visited takes a new order at once.
        scope.launch { settings.state.map { it.recentSubsByVisits }.distinctUntilChanged().drop(1).collect { recentSubreddits.refresh() } }
                // On any account change (switch, or logout then login as someone else):
        // re-read auth config and reload everything that belongs to an account.
        scope.launch {
            var previous: String? = null
            session.state.collect { s ->
                client.invalidateAuthConfig()
                // A full logout drops back to the logged-out identity (bare keys),
                // so the previous account's stores don't stay loaded.
                val identity = when (s) {
                    is SessionState.LoggedIn -> s.session.identity
                    // Only a real logout (has_account cleared) — not a transient
                    // keystore read failure, which must keep everything loaded.
                    SessionState.LoggedOut -> if (session.hasAccount) return@collect else ""
                    SessionState.Loading -> return@collect
                }
                if (identity != previous) {
                    if (previous != null) repository.clearSubsCache()
                    val first = previous == null
                    previous = identity
                    val username = (s as? SessionState.LoggedIn)?.session?.username ?: ""
                    // start() already loaded the logged-out identity.
                    if (!(first && identity.isEmpty())) userScoped.forEach { it.onUserChanged(username) }
                }
                loadedIdentity.value = identity
            }
        }
    }
}

lateinit var appInstance: AppContainer

/** The app's singletons. */
val app: AppContainer get() = appInstance
