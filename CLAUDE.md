# Kestrel for Reddit — native Kotlin

A Reddit client for Android in Kotlin/Jetpack Compose. Originally ported from a
Flutter app, which is why some code and comments refer to Flutter/Dart.

## Identifiers
applicationId/namespace `xyz.draumaz.kestrel` (debug builds:
`xyz.draumaz.kestrel.debug`, via `applicationIdSuffix`; code that needs the
namespace, e.g. activity-alias names, uses `BuildConfig::class.java.packageName`,
not `context.packageName`). OAuth redirect `kestrel://oauth`
(+ `redreader://rr_oauth_redir`). Update checks (`RedditConstants.GITHUB_REPO`)
and analytics (`Analytics.APP_KEY`) are off while those are empty.

## Build
```
./gradlew :app:assembleDebug        # compile check
./gradlew :app:testDebugUnitTest    # JVM unit tests
```
Needs `local.properties` (sdk.dir) or ANDROID_HOME; both gitignored.
Release: `./gradlew :app:assembleRelease` (needs `key.properties` + the .jks at
the repo root; both gitignored). Bump versionCode/versionName in
`app/build.gradle.kts` and add the release at the top of `CHANGELOG` in
`feature/about/WhatsNew.kt` (WhatsNewTest checks they match).

Stack: AGP 9.4 (built-in Kotlin), Kotlin 2.4, Compose BOM 2026.09 +
Material 3 1.5 (Expressive), Navigation Compose (type-safe routes), OkHttp 5,
kotlinx.serialization (JsonElement parsing), Coil 3, Media3, WorkManager,
commonmark-java. minSdk 33 (Android 13; no lower). No Play Services (F-Droid/IzzyOnDroid-safe).

## Architecture (package `xyz.draumaz.kestrel`)
- `app` (global, `AppContainer`) — manual DI: `app.prefs`, `app.secureStore`,
  `app.settings` (StateFlow<Settings>), `app.session` (SessionManager),
  `app.repository` (every Reddit endpoint), `app.client`, `app.rateLimit`,
  shared stores `app.postOverrides`, `app.hiddenPosts`, `app.history`,
  `app.threadVisits`, `app.contentFilters`, `app.offline`, `app.drafts`, and
  feature modules `app.feed`, `app.post`, `app.forYou`, `app.inbox`, `app.media`.
  Module constructors must use their `c: AppContainer` param, not `app`
  (the global is assigned after construction); `app` is fine inside methods.
- Per-account state implements `UserScoped.onUserChanged(username)`; keys via
  `userScopedKey(prefs, username, base)`.
- `core/` — Json helpers (`el["a"]["b"].str()`), RedditConstants, deep links,
  media links, net (RedditClient, Http, Catbox, Redgifs), storage (Prefs with
  Flutter-compatible types, SecureStore), Analytics
  (Aptabase, off without a key), Backup, UpdateChecker.
- `model/` — Post, Comment, InboxItem, Subreddit, RedditUser, Multireddit,
  Flair, Listing (immutable data classes, parsed exactly like the Dart models).
- `nav/` — `Route` (all destinations), `NavCache` (pass Post/InboxItem by id),
  `AppNavigator` = `LocalNavigator.current` / `app.navigator`: push, pop,
  openPost, openLink, openInBrowser, openImage/openGallery/openVideo,
  openPostVideo, share, shareWithTitle, showSnackbar, showActionError.
- `ui/` — theme (`KestrelTheme`, Bloom palettes, `LocalVoteColors`, fonts),
  `Overlays.show { done -> … }` for imperative bottom sheets/dialogs (the
  Flutter `showModalBottomSheet` pattern), `ErrorView`, `friendlyError`.
- `feature/*` — screens. ViewModels hold screen state as StateFlow.

## Code rules
- Keep the "why" comments and the speed tricks (cache-first paint, prefetch,
  retries, debounces).
- Material 3 "Bloom" design: cards
  `surfaceContainerLow` radius 28, stadium buttons/chips, filled inputs
  radius 18, floating pill nav, etc.
- Lists: LazyColumn with stable `key`s and `contentType`; images via Coil
  `AsyncImage` sized to the slot; no work in composition that can be remembered.
