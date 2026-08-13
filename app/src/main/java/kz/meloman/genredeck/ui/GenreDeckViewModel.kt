package kz.meloman.genredeck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kz.meloman.genredeck.data.Genre
import kz.meloman.genredeck.data.GenreRepository
import kz.meloman.genredeck.data.TrackPreviewApi
import java.time.LocalDate
import java.time.ZoneId

data class GenreCard(
    val genre: Genre,
    val preview: TrackPreviewApi.TrackResult? = null,
    val loading: Boolean = false,
    val error: Boolean = false
)

class GenreDeckViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GenreRepository(app)

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres

    private val _cards = MutableStateFlow<Map<String, GenreCard>>(emptyMap())
    val cards: StateFlow<Map<String, GenreCard>> = _cards

    private val _genreOfDay = MutableStateFlow<GenreCard?>(null)
    val genreOfDay: StateFlow<GenreCard?> = _genreOfDay

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    private val _openedCount = MutableStateFlow(0)
    val openedCount: StateFlow<Int> = _openedCount

    private val _playlist = MutableStateFlow<List<GenreCard>>(emptyList())
    val playlist: StateFlow<List<GenreCard>> = _playlist

    init {
        _genres.value = repo.loadGenres()
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        loadCard(repo.genreOfDay(today).name)
        // плейлист: 10 жанров, начиная с жанра дня
        val list = _genres.value
        if (list.isNotEmpty()) {
            val start = (today % list.size).toInt()
            val pl = (0 until 10).map { list[(start + it) % list.size] }
            _playlist.value = pl.map { GenreCard(it) }
            pl.forEach { loadCard(it.name) }
        }
    }

    fun loadCard(name: String) {
        val genre = _genres.value.find { it.name == name } ?: return
        val current = _cards.value[name]
        if (current?.preview != null || current?.loading == true) return

        _cards.value = _cards.value + (name to GenreCard(genre, loading = true))
        viewModelScope.launch {
            val result = try {
                TrackPreviewApi.findPreview(genre.searchTrack.ifBlank { "${genre.artists.firstOrNull() ?: ""} ${genre.name}" })
            } catch (_: Exception) { null }
            val prev = _cards.value[name]
            if (prev != null) {
                _cards.value = _cards.value + (name to prev.copy(
                    preview = result,
                    loading = false,
                    error = result == null
                ))
            }
        }
    }

    fun toggleFavorite(name: String) {
        _favorites.value = if (name in _favorites.value) _favorites.value - name else _favorites.value + name
    }

    fun markOpened() {
        _openedCount.value += 1
    }
}
