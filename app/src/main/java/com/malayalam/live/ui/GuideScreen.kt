package com.malayalam.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.malayalam.live.data.Channel
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

private val Background = Color(0xFF10251D)
private val CardColor = Color(0xFF19342A)
private val FocusGold = Color(0xFFF0C76B)
private val MutedText = Color(0xFFB6C4B9)

@Composable
fun GuideScreen(
    state: GuideUiState,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRefresh: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    val firstCategoryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstCategoryFocus.requestFocus() }

    val categoryNames = remember(state.channels) {
        listOf("All", "Favorites") + state.channels.map(Channel::category).distinct().sorted()
    }
    val filtered = remember(state.channels, state.favorites, search, selectedCategory) {
        state.channels.filter { channel ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Favorites" -> channel.id in state.favorites
                else -> channel.category == selectedCategory
            }
            val matchesSearch = search.isBlank() ||
                channel.name.contains(search.trim(), ignoreCase = true) ||
                channel.category.contains(search.trim(), ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }
    val recentChannels = remember(state.channels, state.recentIds) {
        state.recentIds.mapNotNull { id -> state.channels.firstOrNull { it.id == id } }
    }
    val gridRows = remember(filtered) { filtered.chunked(4) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 48.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MALAYALAM LIVE", color = Color(0xFFDCE8D9), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Browse Malayalam TV", color = Color(0xFFF5F1E7), fontSize = 31.sp, fontWeight = FontWeight.Bold)
            }
            SearchBox(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.width(370.dp).height(56.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(categoryNames, key = { _, category -> category }) { index, category ->
                CategoryChip(
                    category,
                    selected = selectedCategory == category,
                    modifier = if (index == 0) Modifier.focusRequester(firstCategoryFocus) else Modifier,
                ) {
                    selectedCategory = category
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading && state.channels.isEmpty() -> CenterMessage("Loading Malayalam channels…", Modifier.weight(1f))
            state.error != null && state.channels.isEmpty() -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("The channel guide could not be loaded", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(state.error, color = MutedText, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(18.dp))
                TvAction("Try again", onClick = onRefresh)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (recentChannels.isNotEmpty() && selectedCategory != "Favorites" && search.isBlank()) {
                    item(key = "recent-title") { SectionTitle("Recently watched", recentChannels.size) }
                    item(key = "recent-row") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recentChannels, key = { "recent-${it.id}" }) { channel ->
                                ChannelTile(
                                    channel = channel,
                                    isFavorite = channel.id in state.favorites,
                                    onPlay = onPlay,
                                    onToggleFavorite = onToggleFavorite,
                                    modifier = Modifier.width(300.dp),
                                )
                            }
                        }
                    }
                }
                item(key = "all-title") {
                    SectionTitle(
                        if (selectedCategory == "Favorites") "Your favorites" else if (selectedCategory == "All") "All channels" else selectedCategory,
                        filtered.size,
                    )
                }
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (selectedCategory == "Favorites") "No favorites yet. Focus a channel and hold OK to add it." else "No channels match this search.",
                            color = MutedText,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(vertical = 28.dp),
                        )
                    }
                } else {
                    itemsIndexed(gridRows, key = { _, row -> "row-${row.first().id}" }) { _, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { channel ->
                                ChannelTile(
                                    channel = channel,
                                    isFavorite = channel.id in state.favorites,
                                    onPlay = onPlay,
                                    onToggleFavorite = onToggleFavorite,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(CardColor, RoundedCornerShape(14.dp))
            .border(if (isFocused) 3.dp else 1.dp, if (isFocused) FocusGold else Color(0xFF466353), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text("Search channels", color = MutedText, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
private fun CategoryChip(category: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(48.dp)) {
        Box(
            modifier = Modifier
                .background(if (selected) Color(0xFFDCE8D9) else Color(0xFF234234), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(category, color = if (selected) Color(0xFF174B39) else Color(0xFFE6EEE5), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Text(count.toString(), color = MutedText, fontSize = 14.sp)
    }
}

@Composable
private fun ChannelTile(
    channel: Channel,
    isFavorite: Boolean,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var channelFocused by remember(channel.id) { mutableStateOf(false) }
    var favoriteFocused by remember(channel.id) { mutableStateOf(false) }
    Row(modifier = modifier.height(104.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            onClick = { onPlay(channel) },
            modifier = Modifier.weight(1f).fillMaxHeight()
                .onFocusChanged { channelFocused = it.isFocused }
                .border(if (channelFocused) 3.dp else 1.dp, if (channelFocused) FocusGold else Color(0xFF315044), RoundedCornerShape(14.dp)),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().background(CardColor, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "${channel.name} logo",
                        modifier = Modifier.size(54.dp).background(Color(0xFF294B3C), RoundedCornerShape(12.dp)).padding(4.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(54.dp).background(Color(0xFF294B3C), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(channel.name.take(2).uppercase(), color = Color(0xFFDCE8D9), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(channel.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(channel.category, color = MutedText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Surface(
            onClick = { onToggleFavorite(channel) },
            modifier = Modifier.width(52.dp).fillMaxHeight()
                .onFocusChanged { favoriteFocused = it.isFocused }
                .border(if (favoriteFocused) 3.dp else 1.dp, if (favoriteFocused) FocusGold else Color(0xFF315044), RoundedCornerShape(14.dp))
                .semantics { contentDescription = if (isFavorite) "Remove ${channel.name} from favorites" else "Add ${channel.name} to favorites" },
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(CardColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isFavorite) "♥" else "♡", color = if (isFavorite) Color(0xFFF08B70) else MutedText, fontSize = 25.sp)
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MutedText, fontSize = 21.sp)
    }
}

@Composable
private fun TvAction(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.height(50.dp)) {
        Box(
            Modifier.background(Color(0xFFDCE8D9), RoundedCornerShape(12.dp)).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color(0xFF174B39), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
