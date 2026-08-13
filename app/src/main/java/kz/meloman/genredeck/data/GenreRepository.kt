package kz.meloman.genredeck.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Загружает базу жанров из assets/genres.json.
 */
class GenreRepository(private val context: Context) {

    private var cache: List<Genre>? = null

    fun loadGenres(): List<Genre> {
        cache?.let { return it }
        val raw = context.assets.open("genres.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val list = ArrayList<Genre>(arr.length())
        for (i in 0 until arr.length()) {
            val o: JSONObject = arr.getJSONObject(i)
            val artistsArr = o.optJSONArray("artists") ?: JSONArray()
            val artists = ArrayList<String>()
            for (j in 0 until artistsArr.length()) artists.add(artistsArr.getString(j))
            list.add(
                Genre(
                    name = o.getString("name"),
                    description = o.optString("description", ""),
                    origin = o.optString("origin", ""),
                    vibe = o.optString("vibe", ""),
                    artists = artists,
                    searchTrack = o.optString("searchTrack", ""),
                    colorHex = o.optString("colorHex", "#1DB954")
                )
            )
        }
        cache = list
        return list
    }

    /** Жанр дня — детерминированный выбор по дате. */
    fun genreOfDay(dayIndex: Long): Genre {
        val list = loadGenres()
        if (list.isEmpty()) return Genre("Рок","","","", emptyList(),"AC/DC Back In Black","#E53935")
        return list[(dayIndex % list.size).toInt()]
    }
}
