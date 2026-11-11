package xyz.draumaz.kestrel.feature.feed

import xyz.draumaz.kestrel.ui.SearchIcon
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.draumaz.kestrel.app
import xyz.draumaz.kestrel.nav.LocalNavigator
import xyz.draumaz.kestrel.nav.Route
import kotlinx.coroutines.launch

/**
 * The frontpage as a pushed page: Explore's All button opens it when the Posts
 * tab shows r/all (Settings). Plain, like r/all itself: no For You / Blend / Home.
 */
@Composable
fun FrontpageScreen() {
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val fab = remember { FabScroll() }
    val listState = rememberFeedListState()
    var refreshRequests by remember { mutableIntStateOf(0) } // the Refresh floating button
    var hideReadRequests by remember { mutableIntStateOf(0) } // the Hide read posts floating button
    Scaffold(
        topBar = {
            BloomTopBar(
                "Frontpage",
                actions = {
                    IconButton(onClick = { nav.push(Route.Search()) }) { Icon(SearchIcon, "Search") }
                },
            )
        },
        floatingActionButton = {
            FeedFab(
                settings.homeFab,
                fab.expanded,
                onNewPost = { nav.push(Route.Submit()) },
                onSearch = { nav.push(Route.Search()) },
                onTop = { scope.launch { listState.animateScrollToItem(0) } },
                onRefresh = { refreshRequests++ },
                onHideRead = { hideReadRequests++ },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        PostListView(
            feedKey = "",
            modifier = Modifier.padding(top = padding.calculateTopPadding()).nestedScroll(fab),
            listState = listState,
            refreshRequests = refreshRequests,
            hideReadRequests = hideReadRequests,
        )
    }
}
