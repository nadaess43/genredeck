package kz.meloman.genredeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Тёмная тема — основная. Светлая — в разработке.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color.Black,
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFE8E8EA),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFE8E8EA),
    surfaceVariant = Color(0xFF232326),
    onSurfaceVariant = Color(0xFF9E9EA6),
    outline = Color(0xFF3A3A40),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF141416),
    surface = Color.White,
    onSurface = Color(0xFF141416),
)

@Composable
fun GenreDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
