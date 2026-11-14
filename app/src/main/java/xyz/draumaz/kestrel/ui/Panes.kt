package xyz.draumaz.kestrel.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.delay

/*
 * Hinge-aware list-detail, after Lexeme's and TwoPane's Panes.kt. The app's own pages (feeds,
 * Explore, inbox, settings…) live in the start pane; threads open in the end pane beside them.
 * A fold or hinge splits the window right at it, so nothing straddles the crease: side by side
 * across a vertical one (the Surface Duo's two screens, a book-style foldable), stacked across a
 * horizontal one (the Duo on its side, a foldable half-open on a table). A wide enough tablet
 * gets a fixed-width start pane and gives the rest to the thread.
 */

/**
 * A two-pane split: [start] pane, [gap] (a hinge, or a crease's gutter) and [end] pane, side by
 * side or [stacked] top and bottom; sizes run along that axis. [hinge]: the gap is a physical
 * one between two screens (the Duo), so a full-window overlay would be cut in two.
 */
@Immutable
data class PaneSplit(val start: Dp, val gap: Dp, val end: Dp, val stacked: Boolean = false, val hinge: Boolean = false) {
    /** Where the end pane begins, from the window's start (or top) edge. */
    val endOffset: Dp get() = start + gap

    /** Sizes something to the start pane alone (at the window's start or top). */
    val startPane: Modifier
        get() = if (stacked) Modifier.fillMaxWidth().height(start) else Modifier.fillMaxHeight().width(start)

    /** Places and sizes something over the end pane alone. */
    val endPane: Modifier
        get() = if (stacked) Modifier.padding(top = endOffset).fillMaxSize() else Modifier.padding(start = endOffset).fillMaxSize()
}

/** The current split, or null for the single-pane phone layout. */
val LocalPaneSplit = staticCompositionLocalOf<PaneSplit?> { null }

/** The id of the post whose thread is open in the thread pane, so the feed can mark it. */
val LocalOpenThread = androidx.compose.runtime.compositionLocalOf<String?> { null }

/** Which pane a composable is in: null on one pane, else whether it's the end (thread) pane. */
val LocalInEndPane = staticCompositionLocalOf { false }

/** Expanded width (Material's window size class): enough for two panes on a plain tablet. */
private val ExpandedWidth = 840.dp

/** Each side of a vertical fold must be at least this wide to hold a pane. */
private val MinPaneWidth = 300.dp

/** Each side of a horizontal fold must be at least this tall to hold a pane. */
private val MinPaneHeight = 280.dp

/** The least room left at a fold, so nothing sits on a crease or behind a hinge. */
private val FoldGutter = 24.dp

/** Shorter than this, two panes side by side are a phone on its side: too little height. */
private val MinSplitHeight = 400.dp

/**
 * The split for this window, or null for one pane. Recomputed as the window or its folds change
 * (spanning the Duo, unfolding, rotating) without recreating the activity: KestrelActivity
 * handles those configuration changes itself.
 */
@Composable
fun rememberPaneSplit(): PaneSplit? {
    val activity = LocalActivity.current
    val layoutFlow = remember(activity) {
        activity?.let { WindowInfoTracker.getOrCreate(it).windowLayoutInfo(it) }
    }
    val layoutInfo = layoutFlow?.collectAsStateWithLifecycle(initialValue = null)?.value
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    // The last fold seen, and the window size it was seen at. Spanning, the window widens a beat
    // before the hinge is reported; until it is, a window that size still has that hinge, so the
    // panes don't flash a tablet split first and then jump to the hinge.
    // The Surface Duo's hinge, which its newer (community) builds report as a zero-width,
    // seamless fold through the middle: really 84px between two separate screens.
    val duoHinge = with(density) { if (IsSurfaceDuo) DuoHingePx.toDp() else 0.dp }
    val known = remember { arrayOfNulls<Pair<androidx.compose.ui.unit.IntSize, FoldingFeature>>(1) }
    return remember(layoutInfo, size, density) {
        with(density) {
            val width = size.width.toDp()
            val height = size.height.toDp()
            val reported = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()
            reported?.firstOrNull()?.let { known[0] = size to it }
            val folds = reported?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(known[0]?.takeIf { it.first == size }?.second)
            val hingeDevice = known[0] != null
            val vertical = folds.firstOrNull {
                it.orientation == FoldingFeature.Orientation.VERTICAL &&
                    it.bounds.left.toDp() >= MinPaneWidth && width - it.bounds.right.toDp() >= MinPaneWidth
            }
            // A flat, seamless horizontal fold across a wide screen reads better side by side;
            // a hinge, a half-open fold, or a narrow window (the rotated Duo) stacks at it.
            val horizontal = folds.firstOrNull {
                it.orientation == FoldingFeature.Orientation.HORIZONTAL &&
                    it.bounds.top.toDp() >= MinPaneHeight && height - it.bounds.bottom.toDp() >= MinPaneHeight &&
                    (it.isSeparating || it.state == FoldingFeature.State.HALF_OPENED || width < ExpandedWidth)
            }
            // On the Duo the fold is reported a moment after the first frame: spanned (a window
            // across both screens), start at the hinge rather than flash a tablet split first.
            if (layoutInfo == null && IsSurfaceDuo && maxOf(size.width, size.height) > DuoSpannedPx) {
                val stacked = size.height > size.width
                val (start, gap) = gutter((if (stacked) height else width) / 2, (if (stacked) height else width) / 2, duoHinge)
                return@with PaneSplit(start, gap, (if (stacked) height else width) - start - gap, stacked = stacked, hinge = true)
            }
            when {
                vertical != null && height >= MinSplitHeight -> {
                    val (start, gap) = gutter(vertical.bounds.left.toDp(), vertical.bounds.right.toDp(), duoHinge)
                    PaneSplit(start, gap, width - start - gap, hinge = vertical.isHinge())
                }
                horizontal != null -> {
                    val (start, gap) = gutter(horizontal.bounds.top.toDp(), horizontal.bounds.bottom.toDp(), duoHinge)
                    PaneSplit(start, gap, height - start - gap, stacked = true, hinge = horizontal.isHinge())
                }
                // A device with a hinge, on one screen (or a fold reported a beat late): one pane.
                !hingeDevice && width >= ExpandedWidth && height >= MinSplitHeight -> {
                    // The feed keeps a phone's width; the thread gets the rest.
                    val start = (width * 0.42f).coerceIn(360.dp, 520.dp)
                    val gap = 0.dp
                    PaneSplit(start, gap, width - start - gap)
                }
                else -> null
            }
        }
    }
}

/** A Surface Duo (or Duo 2): two screens either side of a hinge, whatever its fold reports. */
private val IsSurfaceDuo = android.os.Build.MANUFACTURER.equals("Microsoft", ignoreCase = true) &&
    android.os.Build.DEVICE.startsWith("duo")

/** The Duo's hinge, in pixels: what Microsoft's own builds reported as the fold's bounds. */
private const val DuoHingePx = 84

/** Longer than one Duo screen (1800px): the window spans both. */
private const val DuoSpannedPx = 2400

/** A gap between two separate screens, not a crease: a dialog across it is cut in two. */
private fun FoldingFeature.isHinge() = isSeparating || bounds.width() > 0 && bounds.height() > 0 || IsSurfaceDuo

/**
 * The start pane's size and the gap for a fold spanning [from] to [to]: a gutter at least
 * FoldGutter across, centred on the fold, since some devices report a hinge (or a crease) as a
 * zero-width line; at least [least] across, for a hinge we know is wider.
 */
private fun gutter(from: Dp, to: Dp, least: Dp): Pair<Dp, Dp> {
    val gap = maxOf(to - from, FoldGutter, least)
    return ((from + to) / 2 - gap / 2) to gap
}

/*
 * The unfold, after TwoPane's: when the window goes from one screen to two (the Duo opened out,
 * the app spanned), both panes swing open from the hinge like a book's covers, on a spring that
 * carries them a touch past flat before they settle. Progress runs 0 (shut) to 1 (open flat) and
 * is read only in the draw phase, so it costs redraws, never recomposition.
 */

/** Unfold progress for [split], which starts from shut whenever the window spans a hinge. */
@Composable
fun rememberUnfold(split: PaneSplit?): () -> Float {
    val spanned = split != null && split.hinge && !split.stacked
    val was = remember { mutableStateOf(spanned) }
    val unfold = remember(spanned) { Animatable(if (spanned && !was.value) 0f else 1f) }
    SideEffect { was.value = spanned }
    LaunchedEffect(unfold) {
        if (unfold.value < 1f) {
            // The second screen is still lighting up as the window spans: wait until it's on,
            // so the swing plays out where it can be seen.
            delay(250)
            unfold.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 90f))
        }
    }
    return remember(unfold) { { unfold.value } }
}

/** The start pane, hinged on its trailing edge: tips back a little. */
fun Modifier.unfoldStart(unfold: () -> Float): Modifier = graphicsLayer {
    val p = unfold()
    if (p != 1f) {
        rotationY = -(1f - p) * 28f
        transformOrigin = TransformOrigin(1f, 0.5f)
        cameraDistance = 30f * density
        alpha = (0.35f + p).coerceIn(0f, 1f)
    }
}

/** The end pane, hinged on its leading edge: swings in from edge-on. */
fun Modifier.unfoldEnd(unfold: () -> Float): Modifier = graphicsLayer {
    val p = unfold()
    if (p != 1f) {
        rotationY = (1f - p) * 92f
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 30f * density
        alpha = (p * 2.5f).coerceIn(0f, 1f)
    }
}

/**
 * Moves a window-centred overlay (a dialog, a bottom sheet) into one pane when the split has a
 * hinge, so it isn't cut in two by it: into the end pane from a thread, else the start pane.
 * Centred over [windowWidth], an overlay shifts by the distance between the window's centre and
 * the pane's.
 */
@Composable
fun Modifier.clearOfHinge(): Modifier {
    val split = LocalPaneSplit.current?.takeIf { it.hinge } ?: return this
    val inEnd = LocalInEndPane.current
    val total = split.start + split.gap + split.end
    val paneCentre = if (inEnd) split.endOffset + split.end / 2 else split.start / 2
    val shift = paneCentre - total / 2
    return if (split.stacked) this.offset(y = shift) else this.offset(x = shift)
}

/**
 * The size of the pane this is composed in, for things sized as a share of the screen (a swipe's
 * reach, a sheet's height, how wide to decode a feed image). On one pane it's the configuration's
 * screen size, as before; on two, the pane's own.
 */
@Composable
fun paneSize(): androidx.compose.ui.unit.DpSize {
    val split = LocalPaneSplit.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    if (split == null) return androidx.compose.ui.unit.DpSize(config.screenWidthDp.dp, config.screenHeightDp.dp)
    val window = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.let { androidx.compose.ui.unit.DpSize(it.width.toDp(), it.height.toDp()) } }
    val along = if (LocalInEndPane.current) split.end else split.start
    return if (split.stacked) window.copy(height = along) else window.copy(width = along)
}
