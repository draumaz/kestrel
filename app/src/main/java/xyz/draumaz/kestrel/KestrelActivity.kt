package xyz.draumaz.kestrel

import xyz.draumaz.kestrel.feature.auth.SETUP_DONE
import xyz.draumaz.kestrel.feature.auth.needsSetup
import xyz.draumaz.kestrel.feature.auth.SetupScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.Easing
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import xyz.draumaz.kestrel.feature.feed.CustomFeedsScreen
import xyz.draumaz.kestrel.feature.about.WhatsNewScreen
import android.view.KeyEvent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import xyz.draumaz.kestrel.auth.OAuthFlow
import xyz.draumaz.kestrel.auth.SessionState
import xyz.draumaz.kestrel.core.isRedditHost
import xyz.draumaz.kestrel.core.redditUrlInText
import xyz.draumaz.kestrel.core.routeForRedditUrl
import xyz.draumaz.kestrel.core.isShareLink
import xyz.draumaz.kestrel.core.resolveShareLink
import xyz.draumaz.kestrel.core.isRedditMediaHost
import xyz.draumaz.kestrel.feature.post.VolumeKeys
import xyz.draumaz.kestrel.feature.auth.LoginScreen
import xyz.draumaz.kestrel.feature.auth.WebLoginScreen
import xyz.draumaz.kestrel.feature.compose.SubmitScreen
import xyz.draumaz.kestrel.feature.feed.HistoryScreen
import xyz.draumaz.kestrel.feature.post.SummariesScreen
import xyz.draumaz.kestrel.feature.feed.ManageMultiredditScreen
import xyz.draumaz.kestrel.feature.feed.MultiredditScreen
import xyz.draumaz.kestrel.feature.feed.OfflineScreen
import xyz.draumaz.kestrel.feature.feed.SavedScreen
import xyz.draumaz.kestrel.feature.feed.SearchScreen
import xyz.draumaz.kestrel.feature.feed.FrontpageScreen
import xyz.draumaz.kestrel.feature.feed.SubredditScreen
import xyz.draumaz.kestrel.settings.postsTabKey
import xyz.draumaz.kestrel.feature.feed.UserScreen
import xyz.draumaz.kestrel.feature.home.HomeScreen
import xyz.draumaz.kestrel.feature.home.SideBarPage
import xyz.draumaz.kestrel.nav.SIDE_BAR_PAGE
import xyz.draumaz.kestrel.feature.compose.DraftsScreen
import xyz.draumaz.kestrel.feature.inbox.NotificationActivityScreen
import xyz.draumaz.kestrel.feature.inbox.ComposeMessageScreen
import xyz.draumaz.kestrel.feature.inbox.MessageThreadScreen
import xyz.draumaz.kestrel.feature.media.MediaViewerHost
import xyz.draumaz.kestrel.feature.post.PostDetailScreen
import xyz.draumaz.kestrel.feature.settings.ContentFiltersScreen
import xyz.draumaz.kestrel.feature.settings.ManageForYouScreen
import xyz.draumaz.kestrel.feature.settings.PolicyScreen
import xyz.draumaz.kestrel.feature.settings.SettingsScreen
import xyz.draumaz.kestrel.feature.web.RedditWebScreen
import xyz.draumaz.kestrel.nav.AppNavigator
import xyz.draumaz.kestrel.nav.LocalNavigator
import xyz.draumaz.kestrel.nav.Route
import xyz.draumaz.kestrel.nav.ViewerStack
import xyz.draumaz.kestrel.ui.OverlayHost
import xyz.draumaz.kestrel.ui.theme.KestrelTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.draumaz.kestrel.core.Analytics
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.compose.runtime.SideEffect
import xyz.draumaz.kestrel.core.AppIcon
import xyz.draumaz.kestrel.ui.AppIconPreview
import xyz.draumaz.kestrel.ui.LocalInEndPane
import xyz.draumaz.kestrel.ui.LocalPaneSplit
import xyz.draumaz.kestrel.ui.LocalOpenThread
import xyz.draumaz.kestrel.ui.PaneSplit
import xyz.draumaz.kestrel.ui.rememberPaneSplit
import xyz.draumaz.kestrel.ui.rememberUnfold
import xyz.draumaz.kestrel.ui.unfoldEnd
import xyz.draumaz.kestrel.ui.unfoldStart

/** Once per process (the activity can be recreated). */
private var appStartTracked = false

/** Set once the first KestrelActivity of this process has been created. */
private var activityCreatedInProcess = false

class KestrelActivity : ComponentActivity() {
    /** The latest incoming deep link (VIEW or share intent), consumed by the UI. */
    private val pendingLink = MutableStateFlow<Uri?>(null)
    private var lastLink: String? = null
    private var lastLinkAt = 0L
    private var lastInboxSync = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash up until the stored session is read (one keystore read).
        splash.setKeepOnScreenCondition { app.session.state.value == SessionState.Loading }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        // Restored after Android killed the app in the background (a swipe
        // away from Recents starts fresh, with no saved state).
        // (Only the first activity of the process: a later recreation, e.g. a
        // configuration change, also has saved state but a live process.)
        if (savedInstanceState != null && !activityCreatedInProcess) app.restoredProcess = true
        activityCreatedInProcess = true
        if (savedInstanceState == null) {
            prestartFrontpage()
            trackAppStarted()
            // Reopened from Recents after the process died: the launch intent is
            // the old one (e.g. a notification tap) — don't replay it.
            if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) handleIntent(intent)
        }
        setContent {
            val settings by app.settings.state.collectAsStateWithLifecycle()
            KestrelTheme(settings) { AppRoot(pendingLink) }
        }
    }

    // Settings → Volume keys move between comments: a thread on screen takes
    // volume up / down (press and release both, so the volume doesn't change),
    // unless a media viewer is open — its video keeps the volume keys.
    private fun volumeNavigation(keyCode: Int): ((Boolean) -> Boolean)? {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return null
        if (!app.settings.value.volumeKeyNavigation || app.navigatorOrNull?.viewers?.isOpen == true) return null
        return VolumeKeys.handler
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val h = volumeNavigation(keyCode) ?: return super.onKeyDown(keyCode, event)
        h(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (volumeNavigation(keyCode) != null) true else super.onKeyUp(keyCode, event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Anonymous: which login method this install uses. Sent from the activity,
     * not Application.onCreate — that also runs for the background inbox poll,
     * which would count as a launch every 15 minutes.
     */
    private fun trackAppStarted() {
        if (appStartTracked) return
        appStartTracked = true
        app.scope.launch {
            val username = app.secureStore.username()
            val method = when {
                username.isNullOrEmpty() -> "logged_out"
                app.secureStore.authMode() == "web" -> "website"
                else -> "api"
            }
            // Which frontpage feed this install uses (and whether Reddit Home is
            // allowed at all), to see how many actually use Home.
            val s = app.settings.value
            val feed = when {
                s.postsTabAll -> "all"
                s.redditHomeAllowed && s.redditHomeFeed -> "home"
                s.forYouFeed && s.blendHome && s.redditHomeAllowed -> "blend"
                s.forYouFeed -> "for_you"
                else -> "frontpage"
            }
            Analytics.track("app_started", mapOf("login_method" to method, "feed" to feed, "home_allowed" to s.redditHomeAllowed))
        }
    }

    /**
     * Starts loading the frontpage (For You's sources, or Reddit Home's hidden
     * browser) as soon as the stored session is read, while the splash and
     * first frame are still being set up, instead of when Home first composes.
     * Not in Application.onCreate: that also runs for the background inbox poll.
     */
    private fun prestartFrontpage() {
        app.scope.launch {
            val s = app.session.state.first { it != SessionState.Loading } as? SessionState.LoggedIn ?: return@launch
            // After the account's stores are loaded: that also resets the feeds.
            app.loadedIdentity.first { it == s.session.identity }
            app.feed.controller(app.settings.value.postsTabKey).start()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (app.inbox.handleLaunchIntent(intent)) return
        // "Open in Kestrel" in another app's share sheet: the link is in the text.
        val shared = intent.action == Intent.ACTION_SEND
        val uri = (if (shared) redditUrlInText(intent.getStringExtra(Intent.EXTRA_TEXT))?.let(Uri::parse) else intent.data)
            ?: return
        if (!shared && intent.action != Intent.ACTION_VIEW || !isRedditHost(uri.host)) return
        // Dedupe: the same link can arrive twice in a row (cold start +
        // resume) — but tapping it again later must still open it.
        val now = System.currentTimeMillis()
        if (uri.toString() == lastLink && now - lastLinkAt < 2_000) return
        lastLink = uri.toString()
        lastLinkAt = now
        pendingLink.value = uri
    }

    override fun onPause() {
        super.onPause()
        OAuthFlow.onAppPaused()
    }

    override fun onStart() {
        super.onStart()
        app.forYou.redditHome.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        // The hidden Reddit Home page keeps running its scripts otherwise.
        app.forYou.redditHome.onAppBackground()
        Analytics.flushPostsOpened()
    }

    override fun onResume() {
        super.onResume()
        OAuthFlow.onAppResumed()
        // Re-read the keystore to restore auth after background/resume.
        app.session.reload()
        // Re-sync the inbox (items read elsewhere show as read), at most every 2 minutes.
        val now = System.currentTimeMillis()
        if (now - lastInboxSync > 2 * 60_000) {
            lastInboxSync = now
            app.inbox.onAppResumed()
        }
    }
}

@Composable
private fun AppRoot(pendingLink: MutableStateFlow<Uri?>) {
    val context = LocalContext.current
    val controller = rememberNavController()
    val scope = rememberCoroutineScope()
    val viewers = rememberSaveable(saver = ViewerStack.Saver) { ViewerStack() }
    val navigator = remember(controller) { AppNavigator(context, controller, scope, viewers) }
    DisposableEffect(navigator) {
        app.navigatorOrNull = navigator
        onDispose { if (app.navigatorOrNull === navigator) app.navigatorOrNull = null }
    }
    val session by app.session.state.collectAsStateWithLifecycle()
    val link by pendingLink.collectAsStateWithLifecycle()

    if (session == SessionState.Loading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        return
    }
    val loggedIn = session is SessionState.LoggedIn
    // A transient keystore read failure can momentarily read as logged out;
    // don't bounce a known account to the login screen.
    val showHome = loggedIn || app.session.hasAccount
    // A fresh install starts with the setup (#114), then sign-in.
    val start: Route = remember { if (showHome) Route.Home else if (needsSetup()) Route.Setup else Route.Login }

    LaunchedEffect(showHome) {
        val route = controller.currentDestination?.route.orEmpty()
        val atLogin = route.contains("Login") || route.contains("Setup")
        // Someone who already has an account never sees the setup, even after signing out.
        if (showHome && needsSetup()) app.prefs.setBool(SETUP_DONE, true)
        if (showHome && atLogin) navigator.resetTo(Route.Home)
        if (!showHome && !atLogin) navigator.resetTo(Route.Login)
    }
    LaunchedEffect(link, showHome) {
        val uri = link ?: return@LaunchedEffect
        if (!showHome) return@LaunchedEffect
        // Clearing the link re-keys this effect, so the work (a share link
        // resolves over the network) runs on the navigator's scope, not here.
        pendingLink.value = null
        navigator.scope.launch {
            // Share links (/r/<sub>/s/<code>) redirect to the real post first.
            val target = if (isShareLink(uri)) resolveShareLink(uri) ?: uri else uri
            when {
                // i.redd.it / v.redd.it / preview.redd.it: straight into the viewer.
                isRedditMediaHost(target.host) -> navigator.openLink(target.toString())
                else -> routeForRedditUrl(target)?.let(navigator::push)
                    // A reddit page Kestrel can't show (wiki, settings…): the browser,
                    // keeping whatever was open here.
                    ?: navigator.openInBrowser(target)
            }
        }
    }

    // Two panes on tablets and across a foldable's fold or the Surface Duo's hinge: the app in
    // the start pane, threads in the end pane beside it. Signed out, one pane.
    val split = rememberPaneSplit().takeIf { showHome }
    val unfold = rememberUnfold(split)
    // The threads open in the end pane, so they can move to the main pane if it goes away.
    val detailThreads = remember { mutableStateListOf<Route.Post>() }
    LaunchedEffect(split != null) {
        if (split != null) {
            // Spanned (or the window widened): threads on top of the main pane move across.
            val moved = ArrayDeque<Route.Post>()
            while (controller.currentBackStackEntry?.destination?.hasRoute<Route.Post>() == true) {
                moved.addFirst(controller.currentBackStackEntry!!.toRoute<Route.Post>())
                controller.popBackStack()
            }
            val detail = snapshotFlow { navigator.detail }.first { it != null }!!
            moved.forEach { detail.controller.navigate(it) }
        } else if (detailThreads.isNotEmpty()) {
            // Back to one screen: the threads that were open beside the app come along, on top.
            detailThreads.toList().forEach { controller.navigate(it) }
            detailThreads.clear()
        }
    }

    // Back acts on the pane in use: while it's the thread pane, with a thread open, only that
    // pane's pages (and their back handlers) take it; otherwise only the main pane's. Home's
    // always takes Back (it never leaves the app), so without this a thread could only be closed
    // with its own arrow.
    val threadTakesBack = split != null && navigator.threadPaneActive && detailThreads.isNotEmpty()

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalPaneSplit provides split,
        LocalOpenThread provides if (split != null) detailThreads.lastOrNull()?.postId else null,
    ) {
        // A Surface (not a plain background) so content colour = onSurface everywhere.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) { Box(Modifier.fillMaxSize()) {
            // Always this one Box, split or not, so spanning never recreates the app's pages.
            Box(
                (split?.startPane ?: Modifier.fillMaxSize())
                    .then(if (split != null) Modifier.unfoldStart(unfold).paneInsets(split, end = false) else Modifier)
                    .notePane(navigator, end = false),
            ) {
                PaneBack(enabled = !threadTakesBack) { AppNavHost(controller, start) }
            }
            if (split != null) {
                PaneBack(enabled = threadTakesBack) {
                    ThreadPane(split, navigator, viewers, detailThreads, Modifier.unfoldEnd(unfold))
                }
            }
            // Media viewers: a transparent overlay above the current screen. Across a hinge, on
            // the thread pane's screen, so a picture is never cut in two.
            Box(if (split?.hinge == true) split.endPane else Modifier.fillMaxSize()) {
                MediaViewerHost(viewers)
            }
            // Overlays drawn in place (the top bar's ⋯ panel) cover only the pane in use, so they
            // never run across the hinge; sheets and dialogs are windows of their own, which
            // PaneOverlays moves onto the same screen.
            val overlayInEnd = split != null && navigator.threadPaneActive
            Box(
                when {
                    split == null -> Modifier.fillMaxSize()
                    overlayInEnd -> split.endPane.paneInsets(split, end = true)
                    else -> split.startPane.paneInsets(split, end = false)
                },
            ) {
                CompositionLocalProvider(LocalInEndPane provides overlayInEnd) { OverlayHost() }
            }
            // Snackbars in the main pane, above its floating nav.
            Box(split?.startPane ?: Modifier.fillMaxSize()) {
                SnackbarHost(
                    navigator.snackbar,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = navigator.snackbarBottomPadding),
                )
            }
        } }
    }
}

/**
 * Clips a pane (its pages slide in and out) and, stacked, drops the system bar it doesn't touch:
 * the top pane has no navigation bar beneath it, the bottom one no status bar above.
 */
@Composable
private fun Modifier.paneInsets(split: PaneSplit, end: Boolean) = clipToBounds().then(
    when {
        !split.stacked -> Modifier
        end -> Modifier.consumeWindowInsets(WindowInsets.statusBars)
        else -> Modifier.consumeWindowInsets(WindowInsets.navigationBars)
    },
)

/**
 * The back handlers of [content] (a pane's pages) as a branch that can be switched off as one,
 * so Back goes to the pane in use. Switched in the same frame the panes' NavHosts update their
 * own handlers, never a frame later.
 */
@Composable
private fun PaneBack(enabled: Boolean, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalNavigationEventDispatcherOwner.current)
    val owner = remember(parent) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher(parent.navigationEventDispatcher)
        }
    }
    SideEffect { owner.navigationEventDispatcher.isEnabled = enabled }
    DisposableEffect(owner) { onDispose { owner.navigationEventDispatcher.dispose() } }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
}

/** Notes which pane is in use (see AppNavigator.threadPaneActive) as soon as it's touched. */
private fun Modifier.notePane(navigator: AppNavigator, end: Boolean) = pointerInput(navigator, end) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (navigator.threadPaneActive != end) navigator.threadPaneActive = end
    }
}

/**
 * The thread pane: its own back stack of threads, over a resting page when none is open. Threads
 * opened from the main pane replace what's here; one linked from a thread stacks on it.
 */
@Composable
private fun ThreadPane(
    split: PaneSplit,
    main: AppNavigator,
    viewers: ViewerStack,
    threads: SnapshotStateList<Route.Post>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = rememberNavController()
    val navigator = remember(controller) { AppNavigator(context, controller, scope, viewers, parent = main) }
    DisposableEffect(navigator) {
        main.detail = navigator
        onDispose { if (main.detail === navigator) main.detail = null }
    }
    // Mirrors the threads open here, so they can move to the main pane if this one goes away.
    LaunchedEffect(controller) {
        controller.currentBackStack.collect { stack ->
            val open = stack.filter { it.destination.hasRoute<Route.Post>() }.map { it.toRoute<Route.Post>() }
            threads.clear()
            threads.addAll(open)
        }
    }
    CompositionLocalProvider(LocalNavigator provides navigator, LocalInEndPane provides true) {
        Box(split.endPane.then(modifier).paneInsets(split, end = true).notePane(main, end = true)) {
            NavHost(
                navController = controller,
                startDestination = Route.NoThread,
                enterTransition = { slideInHorizontally(tween(300, easing = Emphasized)) { it / 3 } + fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { slideOutHorizontally(tween(300, easing = Emphasized)) { it / 3 } + fadeOut(tween(200)) },
                predictivePopEnterTransition = { fadeIn(tween(300, easing = LinearEasing)) },
                predictivePopExitTransition = { slideOutHorizontally(tween(300, easing = LinearEasing)) { it / 3 } + fadeOut(tween(300, easing = LinearEasing)) },
            ) {
                screen<Route.NoThread> { NoThreadPage() }
                screen<Route.Post> {
                    val r = it.toRoute<Route.Post>(); PostDetailScreen(r.subreddit, r.postId, r.focusCommentId)
                }
            }
        }
    }
}

/** The thread pane at rest: the mark, and where threads will open. */
@Composable
private fun NoThreadPage() {
    val settings by app.settings.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconPreview(AppIcon.parse(settings.appIcon), 72.dp, Modifier.alpha(0.9f))
        Spacer(Modifier.height(20.dp))
        Text("No thread open", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Posts you open will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The app's pages, in the main pane (or the only one). */
@Composable
private fun AppNavHost(controller: NavHostController, start: Route) {
    NavHost(
        navController = controller,
        startDestination = start,
        // A fast full-width slide: the new page slides in from the
        // right over a slight parallax of the old one. The back gesture
        // scrubs the reverse (linear, so the page stays under the finger).
        // Search instead fades in over the page, from the top, like Gmail's (#77).
        // The side bar's pages switch in place, as its tabs do (#118).
        enterTransition = {
            if (targetState.isSideBarPage()) EnterTransition.None
            else if (targetState.destination.hasRoute<Route.Search>()) searchIn(Emphasized)
            else slideInHorizontally(tween(300, easing = Emphasized)) { it }
        },
        exitTransition = {
            if (targetState.isSideBarPage()) ExitTransition.None
            else if (targetState.destination.hasRoute<Route.Search>()) ExitTransition.KeepUntilTransitionsFinished
            else slideOutHorizontally(tween(300, easing = Emphasized)) { -it / 4 }
        },
        popEnterTransition = {
            if (initialState.isSideBarPage() || initialState.destination.hasRoute<Route.Search>()) EnterTransition.None
            else slideInHorizontally(tween(300, easing = Emphasized)) { -it / 4 }
        },
        popExitTransition = {
            if (initialState.isSideBarPage()) ExitTransition.None
            else if (initialState.destination.hasRoute<Route.Search>()) searchOut(Emphasized)
            else slideOutHorizontally(tween(300, easing = Emphasized)) { it }
        },
        predictivePopEnterTransition = {
            if (initialState.destination.hasRoute<Route.Search>()) EnterTransition.None
            else slideInHorizontally(tween(300, easing = LinearEasing)) { -it / 4 }
        },
        predictivePopExitTransition = {
            if (initialState.destination.hasRoute<Route.Search>()) searchOut(LinearEasing)
            else slideOutHorizontally(tween(300, easing = LinearEasing)) { it }
        },
    ) {
        screen<Route.Login> { LoginScreen() }
        screen<Route.Setup> { SetupScreen() }
        screen<Route.WebLogin> { WebLoginScreen(it.toRoute<Route.WebLogin>().clearFirst) }
        screen<Route.Home> { HomeScreen() }
        screen<Route.Settings> { SettingsScreen() }
        screen<Route.ThemeColors> { xyz.draumaz.kestrel.feature.settings.ThemeColorsScreen() }
        screen<Route.SettingsSection> { SettingsScreen(it.toRoute<Route.SettingsSection>().title) }
        screen<Route.ManageForYou> { ManageForYouScreen() }
        screen<Route.ContentFilters> { ContentFiltersScreen() }
        screen<Route.Policy> { PolicyScreen() }
        screen<Route.WhatsNew> { WhatsNewScreen() }
        screen<Route.CustomFeeds> { CustomFeedsScreen() }
        screen<Route.History> { HistoryScreen() }
        screen<Route.Summaries> { SummariesScreen() }
        screen<Route.Saved> { SavedScreen() }
        screen<Route.Offline> { OfflineScreen() }
        screen<Route.NotificationActivity> { NotificationActivityScreen() }
        screen<Route.Drafts> { DraftsScreen() }
        screen<Route.RedditWeb> { val r = it.toRoute<Route.RedditWeb>(); RedditWebScreen(r.url, r.title) }
        screen<Route.Search> { val r = it.toRoute<Route.Search>(); SearchScreen(r.subreddit, r.query, r.sort, r.time) }
        screen<Route.Submit> { SubmitScreen(it.toRoute<Route.Submit>().subreddit) }
        screen<Route.MessageThread> { MessageThreadScreen(it.toRoute<Route.MessageThread>().fullname) }
        screen<Route.ComposeMessage> { ComposeMessageScreen(it.toRoute<Route.ComposeMessage>().to) }
        screen<Route.Subreddit> { SubredditScreen(it.toRoute<Route.Subreddit>().name) }
        screen<Route.Frontpage> { FrontpageScreen() }
        screen<Route.User> { UserScreen(it.toRoute<Route.User>().username) }
        screen<Route.Multireddit> { val r = it.toRoute<Route.Multireddit>(); MultiredditScreen(r.username, r.name) }
        screen<Route.ManageMultireddit> {
            val r = it.toRoute<Route.ManageMultireddit>(); ManageMultiredditScreen(r.username, r.name)
        }
        screen<Route.Post> {
            val r = it.toRoute<Route.Post>(); PostDetailScreen(r.subreddit, r.postId, r.focusCommentId)
        }
    }
}

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Opened from the side bar (AppNavigator.openFromSideBar). */
private fun NavBackStackEntry.isSideBarPage() = savedStateHandle.get<Boolean>(SIDE_BAR_PAGE) == true

private fun searchIn(easing: Easing) =
    fadeIn(tween(200)) + expandVertically(tween(300, easing = easing), Alignment.Top) { it / 6 }

private fun searchOut(easing: Easing) =
    fadeOut(tween(200)) + shrinkVertically(tween(300, easing = easing), Alignment.Top) { it / 6 }

/** A destination drawn on an opaque surface, so sliding pages never show through. */
private inline fun <reified T : Route> NavGraphBuilder.screen(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T> { entry ->
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Opened from the side bar (#111): one of its destinations, with the side bar over it.
        val sideBarRoute = remember(entry) { if (entry.savedStateHandle.get<Boolean>(SIDE_BAR_PAGE) == true) entry.toRoute<T>() else null }
        if (sideBarRoute != null) SideBarPage(sideBarRoute) { content(entry) } else content(entry)
    }
}
