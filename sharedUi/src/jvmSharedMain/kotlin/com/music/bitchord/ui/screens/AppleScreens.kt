package com.music.bitchord.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.feedSkeleton
import org.jetbrains.compose.resources.stringResource
import com.music.bitchord.sharedui.resources.*
import androidx.compose.foundation.shape.CircleShape

/**
 * The New page, in Apple's own shape.
 *
 * Apple Music's New tab is not a list of shelves with a heading over each: it
 * is one large card at the head of the page, a grid of two-column cards under
 * it, and then a plain list of what is left. The order is the shape, so it
 * is drawn in that order rather than being approximated with the ordinary
 * shelf rows.
 *
 * The rows themselves are Apple's rows — they come from the Apple catalogue
 * through the installed manifest, whatever else the app is playing — so this
 * page is the *look* axis: Apple's layout over whatever the recommendation
 * axis supplied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleHomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    title: String? = null,
) {
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (title != null) {
                item(key = "apple:title") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 12.dp,
                            bottom = 14.dp,
                        ),
                    )
                }
            }
            when (val page = state) {
                is UiState.Loading -> feedSkeleton(firstIsHero = false)
                is UiState.Error -> item(key = "apple:error") {
                    MessageState(
                        message = page.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val shelves = page.data
                    // The head card: Apple's "Coming Soon" when the catalogue
                    // published one, and the first shelf's own card otherwise,
                    // so the page always leads with a large cover rather than
                    // with an empty gap.
                    val hero = shelves.firstOrNull { it.title.contains("coming soon", ignoreCase = true) }
                        ?: shelves.firstOrNull()
                    if (hero != null) {
                        item(key = "apple:hero") {
                            AppleHeroCard(
                                shelf = hero,
                                onClick = { item ->
                                    onItemClick(item, hero.title)
                                },
                            )
                        }
                    }
                    // The grid: two columns of cards, the way Apple's own page
                    // draws them.
                    val grid = shelves.filter { it !== hero }.take(4)
                    if (grid.isNotEmpty()) {
                        item(key = "apple:grid") {
                            AppleShelfGrid(
                                shelves = grid,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                    }
                    // Everything else, as a plain list under "More to Explore".
                    val rest = shelves.filter { it !== hero }.drop(4)
                    if (rest.isNotEmpty()) {
                        item(key = "apple:more-header") {
                            Text(
                                text = stringResource(Res.string.more_to_explore),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(
                                    start = PAGE_GUTTER,
                                    end = PAGE_GUTTER,
                                    top = 18.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                        items(rest, key = { "apple:shelf:${it.title}" }) { shelf ->
                            AppleExploreRow(
                                item = shelf.items.firstOrNull() ?: return@items,
                                shelfTitle = shelf.title,
                                onClick = { item -> onItemClick(item, shelf.title) },
                                onLongPress = onItemLongPress,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The large card at the head of the page.
 *
 * A cover with the title laid over it, the way Apple's own head card is
 * drawn — the artwork is the card, not a picture inside a card.
 */
@Composable
private fun AppleHeroCard(
    shelf: HomeShelf,
    onClick: (ShelfItem) -> Unit,
) {
    val item = shelf.items.firstOrNull() ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(item) },
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.artworkAt(HEADER_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}

/**
 * The two-column grid of cards.
 *
 * A row of two columns rather than a nested lazy grid: the page is a lazy
 * column already, and a grid inside one is a second scrolling surface for no
 * gain — there are four cards here, and they all fit.
 */
@Composable
private fun AppleShelfGrid(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        shelves.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                pair.forEach { shelf ->
                    val item = shelf.items.firstOrNull()
                    AppleGridCard(
                        shelf = shelf,
                        item = item,
                        onClick = { item -> onItemClick(item, shelf.title) },
                        onLongPress = onItemLongPress,
                        modifier = Modifier.weight(1f),
                    )
                }
                // A row of one card, when the number of them is odd, still
                // has to be two columns wide.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One card of the grid: a cover with the shelf's own name under it. */
@Composable
private fun AppleGridCard(
    shelf: HomeShelf,
    item: ShelfItem?,
    onClick: (ShelfItem) -> Unit,
    onLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (item != null) {
                        Modifier.clickable(
                            onClick = { onClick(item) },
                            onLongClick = onLongPress?.let { { it(item) } },
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (item != null) {
                AsyncImage(
                    model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!shelf.subtitle.isBlank()) {
            Text(
                text = shelf.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One row of the "More to Explore" list. */
@Composable
private fun AppleExploreRow(
    item: ShelfItem,
    shelfTitle: String,
    onClick: (ShelfItem) -> Unit,
    onLongPress: ((ShelfItem) -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onClick(item) },
                onLongClick = onLongPress?.let { { it(item) } },
            )
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle.ifBlank { shelfTitle },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
