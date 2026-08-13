package kz.meloman.genredeck.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.meloman.genredeck.data.Genre

private enum class Tab(val label: String) { DAY("День"), DECK("Колода"), PLAYLIST("Плейлист") }

@Composable
fun GenreDeckApp(vm: GenreDeckViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.DAY) }
    val genres by vm.genres.collectAsState()
    val dayCard by vm.genreOfDay.collectAsState()
    val cards by vm.cards.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val playlist by vm.playlist.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(when (t) {
                            Tab.DAY -> Icons.Filled.DateRange
                            Tab.DECK -> Icons.Filled.Star
                            Tab.PLAYLIST -> Icons.Filled.List
                        }, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.DAY -> DayScreen(dayCard, favorites.contains(dayCard?.genre?.name), onToggle = { dayCard?.let { vm.toggleFavorite(it.genre.name) } })
                Tab.DECK -> DeckScreen(genres, cards, favorites, vm::loadCard, vm::toggleFavorite)
                Tab.PLAYLIST -> PlaylistScreen(playlist, cards, favorites, vm::loadCard, vm::toggleFavorite)
            }
        }
    }
}

// ---------- Экран «Жанр дня» ----------
@Composable
private fun DayScreen(card: GenreCard?, isFav: Boolean, onToggle: () -> Unit) {
    val fallIn = remember { Animatable(0f) }
    LaunchedEffect(card?.genre?.name) { fallIn.snapTo(0f); fallIn.animateTo(1f, tween(500)) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (card == null) {
            Text("Загружаю жанр дня…", color = MaterialTheme.colorScheme.onBackground)
        } else {
            val offsetY = (1f - fallIn.value) * -800f
            Column(
                Modifier.fillMaxWidth().offset(y = offsetY.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ЖАНР ДНЯ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                GenreCardView(card, isFav, onToggle)
            }
        }
    }
}

// ---------- Экран «Колода» (картотека) ----------
@Composable
private fun DeckScreen(
    genres: List<Genre>,
    cards: Map<String, GenreCard>,
    favorites: Set<String>,
    onLoad: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(genres) { g ->
            val card = cards[g.name] ?: GenreCard(g)
            if (card.preview == null && !card.loading && !card.error) {
                LaunchedEffect(g.name) { onLoad(g.name) }
            }
            GenreCardView(card, g.name in favorites, { onToggle(g.name) })
        }
    }
}

// ---------- Экран «Плейлист» ----------
@Composable
private fun PlaylistScreen(
    playlist: List<GenreCard>,
    cards: Map<String, GenreCard>,
    favorites: Set<String>,
    onLoad: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Плейлист недели", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("10 жанров, собранных вокруг жанра дня", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        items(playlist) { pc ->
            val card = cards[pc.genre.name] ?: pc
            if (card.preview == null && !card.loading && !card.error) {
                LaunchedEffect(pc.genre.name) { onLoad(pc.genre.name) }
            }
            GenreCardView(card, pc.genre.name in favorites, { onToggle(pc.genre.name) })
        }
    }
}

// ---------- Карточка жанра ----------
@Composable
fun GenreCardView(card: GenreCard, isFav: Boolean, onToggle: () -> Unit) {
    val g = card.genre
    val accent = runCatching { Color(android.graphics.Color.parseColor(g.colorHex)) }.getOrDefault(Color(0xFF1DB954))
    var expanded by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Обложка
            Box(Modifier.fillMaxWidth().height(180.dp).background(accent.copy(alpha = 0.25f))) {
                val art = card.preview?.artworkUrl
                if (art != null) {
                    AsyncImage(
                        model = art,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(Modifier.fillMaxSize().background(accent.copy(alpha = 0.15f)))
                Text(
                    g.name.uppercase(),
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                IconButton(onClick = onToggle, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f))) {
                    Icon(
                        if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (isFav) Color(0xFFFF3D71) else Color.White
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                if (g.origin.isNotBlank()) {
                    Text(g.origin, style = MaterialTheme.typography.labelMedium, color = accent)
                }
                Spacer(Modifier.height(6.dp))
                Text(g.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (g.artists.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        g.artists.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Раскрытие
                Text(
                    if (expanded) "Свернуть" else "Подробнее",
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (expanded) {
                    Column(Modifier.padding(top = 8.dp)) {
                        if (g.vibe.isNotBlank()) {
                            Text("Вайб: ${g.vibe}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (card.preview != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Сейчас играет: ${card.preview.title} — ${card.preview.artist}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Плеер
                Spacer(Modifier.height(12.dp))
                when {
                    card.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Ищу превью…", style = MaterialTheme.typography.bodySmall)
                    }
                    card.preview?.previewUrl != null -> Button(
                        onClick = {
                            if (playing) {
                                player.value?.stop(); player.value?.release(); player.value = null; playing = false
                            } else {
                                val mp = MediaPlayer()
                                try {
                                    mp.setDataSource(card.preview.previewUrl)
                                    mp.prepare()
                                    mp.start()
                                    mp.setOnCompletionListener { playing = false }
                                    player.value = mp
                                    playing = true
                                } catch (_: Exception) { }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Icon(if (playing) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (playing) "Стоп" else "Слушать 30 сек")
                    }
                    card.error -> Text("Не удалось найти превью 😕", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                card.preview?.externalUrl?.let { url ->
                    TextButton(onClick = { openUrl(context, url) }) {
                        Text("Открыть в iTunes")
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
