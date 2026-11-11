# Kestrel for Reddit

A fast, modern Reddit client for Android, built natively with Kotlin and
Jetpack Compose in a Material 3 Expressive design. You bring your own Reddit API credentials. The app talks
directly to Reddit and sends no analytics or tracking data.

## Features

- Browse the frontpage, subreddits, multireddits, and users
- A personalized **For You** feed built entirely on-device, with explainable
  "why you're seeing this" labels and per-post tuning
- Full participation: vote, comment, reply, submit text/link/image/gallery/video
  posts, edit, and delete — with image/video attachments (and clipboard paste)
  in replies and messages
- Inbox and private messages, with optional background notifications (polling
  only — no Firebase/push services), plus swipe to mark read/unread or delete
- Saved, upvoted, and locally-stored history
- Moderation actions on subreddits you moderate
- Three feed layouts, swipe-to-vote, NSFW blur, AMOLED and dynamic-color themes
- Offline cache and rate-limit awareness

## How the "For You" feed works

Reddit doesn't give third-party apps access to its own recommendation engine, so
Kestrel builds one on your device. Nothing about your interests ever leaves the
phone.

1. **Candidates.** Kestrel pulls a pool of posts from the sources that actually
   matter to you: your subscription frontpage, fresh posts from your favourite
   subreddits, the communities you engage with most, what's rising in your
   subscriptions, and a small slice of r/popular for discovery. The feed is
   endless — scrolling past the first batch pages deeper into those sources and
   re-ranks as you go.
2. **On-device learning.** As you use the app it quietly learns which
   subreddits you care about — commenting counts most, then saving, upvoting,
   sharing, viewing media, and opening posts. It also learns *within* a
   community from post titles (a tiny local keyword model — so it can tell your
   F1 from your NBA). Everything decays over time, so the feed tracks your
   current taste, not last month's binge. All of it lives only in local
   storage, separately per account.
3. **Ranking.** Each candidate is scored by *community weight* (favourites ≫
   subscribed ≫ discovery, boosted by what you've learned), the post's
   popularity *relative to its own community* (so small subreddits aren't
   drowned out by big ones), recency, upvote ratio, and your keyword affinity.
   Posts you've opened — or that were shown twice and ignored — are demoted, so
   refreshes feel fresh.
4. **Diversity.** No single subreddit can dominate — posts are capped per
   community, and discovery picks are sprinkled in at roughly 1 in 6 so the
   feed stays mostly *your* communities without becoming repetitive. Discovery
   that matches your learned keywords outranks generic popular posts.
5. **Explainable & tunable.** Every post shows why it's there ("★ Favourite",
   "Because you engage with r/…", "Because you read posts about …",
   "Trending"), and a long-press lets you ask for more or less of a community,
   or mute it entirely.

## First run

**Recommended — official API (bring your own key):**

1. Go to <https://www.reddit.com/prefs/apps> and create an app of type
   **installed app**.
2. Set the redirect URI to exactly `kestrel://oauth`.
3. Copy the client ID (shown under the app name) into the login screen and
   connect.

**Fallback — sign in via website (no API key):** Reddit has begun restricting
who can create API keys. If you can't, the login screen offers *"Sign in via
website"*, which logs you in through reddit.com directly (no key needed).

> ⚠️ This fallback is **not** Reddit's official API. It may stop working if
> Reddit changes their site, and Reddit could treat it as against their usage
> policy and restrict or ban accounts that use it. It's off by default and gated
> behind an explicit warning — use it at your own risk. The API-key method above
> is always preferred. (See `docs/hydra-fallback.md`.)

## Build from source

Requires Android Studio (its bundled JDK) and the Android SDK.

```
./gradlew :app:assembleRelease
```

Release signing reads `key.properties` and the keystore from the repo root
(not committed). Without them, build a debug APK with `./gradlew :app:assembleDebug`;
it installs as `xyz.draumaz.kestrel.debug`, beside a release build.

The in-app update checker and anonymous analytics are off. To turn them on, set
`GITHUB_REPO` in `core/RedditConstants.kt` (releases are read from that
"owner/repo") and an Aptabase `APP_KEY` in `core/Analytics.kt`.

## Tech

Kotlin, Jetpack Compose with Material 3 Expressive, Navigation Compose, OkHttp,
kotlinx.serialization, Coil, Media3, WorkManager. Reddit OAuth2
(installed-app flow), credentials stored in Android's encrypted storage.
No Google Play Services.

Licensed under the MIT License (see `LICENSE`). Not affiliated with Reddit, Inc.
