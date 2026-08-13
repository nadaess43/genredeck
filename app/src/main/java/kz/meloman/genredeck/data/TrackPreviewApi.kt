package kz.meloman.genredeck.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Ищет 30-сек превью трека. Сначала iTunes Search API (без ключей),
 * если не нашёл/превью нет — фолбэк на Deezer.
 */
object TrackPreviewApi {

    data class TrackResult(
        val title: String,
        val artist: String,
        val previewUrl: String?,
        val artworkUrl: String?,
        val externalUrl: String? = null
    )

    suspend fun findPreview(query: String): TrackResult? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")

        // 1) iTunes
        try {
            val url = URL("https://itunes.apple.com/search?term=$q&media=music&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "GenreDeck/1.0")
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val r = results.getJSONObject(0)
                    val preview = r.optString("previewUrl", null)?.takeIf { it.isNotBlank() }
                    val track = TrackResult(
                        title = r.optString("trackName", query),
                        artist = r.optString("artistName", ""),
                        previewUrl = preview,
                        artworkUrl = r.optString("artworkUrl100", null)?.takeIf { it.isNotBlank() },
                        externalUrl = r.optString("trackViewUrl", null)?.takeIf { it.isNotBlank() }
                    )
                    if (track.previewUrl != null) return@withContext track
                }
            }
            conn.disconnect()
        } catch (_: Exception) { }

        // 2) Deezer
        try {
            val url = URL("https://api.deezer.com/search?q=$q&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val r = data.getJSONObject(0)
                    val preview = r.optString("preview", null)?.takeIf { it.isNotBlank() }
                    val artist = r.optJSONObject("artist")?.optString("name", "") ?: ""
                    val album = r.optJSONObject("album")?.optString("cover_medium", null)?.takeIf { it.isNotBlank() }
                    return@withContext TrackResult(
                        title = r.optString("title", query),
                        artist = artist,
                        previewUrl = preview,
                        artworkUrl = album,
                        externalUrl = r.optString("link", null)?.takeIf { it.isNotBlank() }
                    )
                }
            }
            conn.disconnect()
        } catch (_: Exception) { }

        null
    }
}
