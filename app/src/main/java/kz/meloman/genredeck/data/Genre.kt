package kz.meloman.genredeck.data

/**
 * Музыкальный жанр.
 *
 * @param name название жанра (рус.)
 * @param description короткое описание «что это»
 * @param origin откуда и когда появился
 * @param vibe характер/вайб жанра
 * @param artists ключевые исполнители (для поиска и контекста)
 * @param searchTrack пример трека для поиска превью в iTunes/Deezer
 * @param colorHex акцентный цвет карточки
 */
data class Genre(
    val name: String,
    val description: String,
    val origin: String,
    val vibe: String,
    val artists: List<String>,
    val searchTrack: String,
    val colorHex: String
)
