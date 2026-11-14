package com.malayalam.live.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.malayalam.live.data.Channel

private val Canvas = Color(0xFF091712)
private val Panel = Color(0xFF12251E)
private val PanelRaised = Color(0xFF1A3027)
private val PanelSoft = Color(0xFF203A30)
private val Accent = Color(0xFFD8EF83)
private val AccentWarm = Color(0xFFFF9B75)
private val Ink = Color(0xFFF4F5EF)
private val Muted = Color(0xFFACB9AF)
private val Hairline = Color(0xFF30473C)

private data class ChannelSection(val title: String, val channels: List<Channel>)

@Composable
fun GuideScreen(
    state: GuideUiState,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRefresh: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    val categoryFocus = remember { FocusRequester() }
    val firstChannelFocus = remember { FocusRequester() }
    val firstCategory = remember(state.channels) {
        listOf("All", "Favorites") + state.channels.map(Channel::category).distinct().sorted()
    }

    LaunchedEffect(Unit) { categoryFocus.requestFocus() }

    val filtered = remember(state.channels, state.favorites, search, selectedCategory) {
        state.channels.filter { channel ->
            val categoryMatches = when (selectedCategory) {
                "All" -> true
                "Favorites" -> channel.id in state.favorites
                else -> channel.category == selectedCategory
            }
            val query = search.trim()
            val searchMatches = query.isBlank() || channel.name.contains(query, ignoreCase = true) ||
                channel.category.contains(query, ignoreCase = true)
            categoryMatches && searchMatches
        }.sortedBy(Channel::name)
    }
    val recentChannels = remember(state.channels, state.recentIds) {
        state.recentIds.mapNotNull { id -> state.channels.firstOrNull { it.id == id } }
    }
    val favoriteChannels = remember(state.channels, state.favorites) {
        state.channels.filter { it.id in state.favorites }.sortedBy(Channel::name)
    }
    val featuredChannel = recentChannels.firstOrNull()

    val channelSections = remember(filtered, selectedCategory, search) {
        when {
            search.isNotBlank() -> listOf(ChannelSection("Search results", filtered))
            selectedCategory == "Favorites" -> listOf(ChannelSection("Your favorites", filtered))
            selectedCategory != "All" -> listOf(ChannelSection(selectedCategory, filtered))
            else -> filtered.groupBy(Channel::category)
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (category, channels) -> ChannelSection(category.ifBlank { "Other channels" }, channels) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(start = 54.dp, end = 54.dp, top = 26.dp, bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(Accent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("M", color = Color(0xFF102018), fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("MALAYALAM LIVE", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Kerala, right at home", color = Muted, fontSize = 13.sp)
                }
            }
            SearchBox(search, { search = it }, Modifier.width(400.dp).height(52.dp))
        }

        Spacer(Modifier.height(17.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(firstCategory, key = { it }) { category ->
                CategoryChip(
                    category = category,
                    selected = selectedCategory == category,
                    modifier = if (category == "All") Modifier.focusRequester(categoryFocus) else Modifier,
                ) { selectedCategory = category }
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            state.isLoading && state.channels.isEmpty() -> CenterMessage("Loading your Malayalam channels…", Modifier.weight(1f))
            state.error != null && state.channels.isEmpty() -> ErrorPanel(
                message = state.error,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item(key = "featured") {
                    FeaturedPanel(
                        channel = featuredChannel,
                        onPlay = { featuredChannel?.let(onPlay) },
                        onBrowse = { firstChannelFocus.requestFocus() },
                    )
                }

                if (search.isBlank() && selectedCategory == "All" && favoriteChannels.isNotEmpty()) {
                    item(key = "favorites-row") {
                        ChannelSectionRow(
                            title = "Your favorites",
                            channels = favoriteChannels,
                            favorites = state.favorites,
                            onPlay = onPlay,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                }

                if (search.isBlank() && selectedCategory == "All" && recentChannels.isNotEmpty()) {
                    item(key = "recent-row") {
                        ChannelSectionRow(
                            title = "Recently watched",
                            channels = recentChannels,
                            favorites = state.favorites,
                            onPlay = onPlay,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            message = if (selectedCategory == "Favorites") {
                                "Your favorites will show up here. Select the heart on any channel to save it."
                            } else {
                                "No channels match that search. Try another name or category."
                            },
                        )
                    }
                } else {
                    channelSections.forEachIndexed { sectionIndex, section ->
                        item(key = "section-${section.title}") {
                            ChannelSectionRow(
                                title = section.title,
                                channels = section.channels,
                                favorites = state.favorites,
                                onPlay = onPlay,
                                onToggleFavorite = onToggleFavorite,
                                firstCardFocus = if (sectionIndex == 0) firstChannelFocus else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .background(Panel, RoundedCornerShape(16.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) Accent else Hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("⌕", color = if (focused) Accent else Muted, fontSize = 27.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = TextStyle(color = Ink, fontSize = 18.sp),
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Search channels", color = Muted, fontSize = 17.sp)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Surface(onClick = { onValueChange("") }, shape = RoundedCornerShape(8.dp)) {
                Box(Modifier.background(PanelRaised, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 6.dp)) {
                    Text("Clear", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp).onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(23.dp),
    ) {
        Box(
            modifier = Modifier
                .background(if (selected) Accent else PanelRaised, RoundedCornerShape(23.dp))
                .border(if (focused && !selected) 2.dp else 0.dp, if (focused && !selected) Accent else Color.Transparent, RoundedCornerShape(23.dp))
                .padding(horizontal = 21.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                category,
                color = if (selected) Color(0xFF132018) else Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FeaturedPanel(channel: Channel?, onPlay: () -> Unit, onBrowse: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier.fillMaxWidth().height(208.dp)
            .background(Panel, shape)
            .border(1.dp, Hairline, shape)
            .padding(start = 30.dp, end = 28.dp, top = 24.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                if (channel != null) "CONTINUE WATCHING" else "WELCOME HOME",
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                channel?.name ?: "A little Kerala,\na lot of home.",
                color = Ink,
                fontSize = if (channel != null) 31.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 36.sp,
            )
            Text(
                channel?.let { "${it.category}  ·  Recently watched" } ?: "Discover Malayalam news, entertainment, culture and more.",
                color = Muted,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            ActionButton(
                text = if (channel != null) "▶   Watch again" else "Browse channels",
                onClick = if (channel != null) onPlay else onBrowse,
            )
        }

        Box(
            modifier = Modifier.width(270.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(166.dp).background(PanelSoft, CircleShape), contentAlignment = Alignment.Center) {
                if (channel?.logoUrl.isNullOrBlank()) {
                    Text(channel?.name?.take(2)?.uppercase() ?: "മ", color = Accent, fontSize = 38.sp, fontWeight = FontWeight.Black)
                } else {
                    AsyncImage(
                        model = channel?.logoUrl,
                        contentDescription = "${channel?.name} logo",
                        modifier = Modifier.size(122.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 7.dp)
                    .background(Color(0xFF2C463B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(if (channel != null) "LIVE CHANNEL" else "MALAYALAM TV", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun ChannelSectionRow(
    title: String,
    channels: List<Channel>,
    favorites: Set<String>,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    firstCardFocus: FocusRequester? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.background(PanelRaised, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                Text(channels.size.toString(), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    favorite = channel.id in favorites,
                    onPlay = { onPlay(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                    modifier = Modifier.width(366.dp).height(140.dp),
                    focusRequester = if (channel == channels.firstOrNull()) firstCardFocus else null,
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    favorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    var cardFocused by remember(channel.id) { mutableStateOf(false) }
    var favoriteFocused by remember(channel.id) { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (cardFocused || favoriteFocused) 1.025f else 1f, label = "channel-card-scale")
    val shape = RoundedCornerShape(20.dp)

    Box(modifier = modifier.scale(cardScale)) {
        Surface(
            onClick = onPlay,
            modifier = Modifier.fillMaxSize()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { cardFocused = it.isFocused },
            shape = shape,
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
                    .background(Panel, shape)
                    .border(if (cardFocused || favoriteFocused) 2.dp else 1.dp, if (cardFocused || favoriteFocused) Accent else Hairline, shape)
                    .padding(start = 16.dp, end = 66.dp, top = 15.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                ChannelLogo(channel, Modifier.size(82.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        channel.name,
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp,
                    )
                    Text(channel.category, color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Surface(
            onClick = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(42.dp)
                .onFocusChanged { favoriteFocused = it.isFocused }
                .semantics {
                    contentDescription = if (favorite) "Remove ${channel.name} from favorites" else "Add ${channel.name} to favorites"
                },
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(if (favorite) Color(0xFF49352E) else PanelRaised, CircleShape)
                    .border(if (favoriteFocused) 2.dp else 1.dp, if (favoriteFocused) Accent else Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (favorite) "♥" else "♡", color = if (favorite) AccentWarm else Muted, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PanelSoft, RoundedCornerShape(16.dp)).padding(9.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logoUrl.isNullOrBlank()) {
            Text(channel.name.take(2).uppercase(), color = Accent, fontSize = 21.sp, fontWeight = FontWeight.Black)
        } else {
            AsyncImage(model = channel.logoUrl, contentDescription = "${channel.name} logo", modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp).onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(
            modifier = Modifier
                .background(if (focused) Color(0xFFE6F6AE) else Accent, RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color(0xFF132018), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Text(message, color = Muted, fontSize = 16.sp, lineHeight = 23.sp)
    }
}

@Composable
private fun ErrorPanel(message: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("The channel guide couldn’t load", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Muted, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(20.dp))
        ActionButton("Try again", onRefresh)
    }
}

@Composable
private fun CenterMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Muted, fontSize = 20.sp)
    }
}
