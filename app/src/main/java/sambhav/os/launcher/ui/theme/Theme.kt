package sambhav.os.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Concept A, "Luxury Editorial": warm charcoal, ivory and brass. */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val sub: Color,
    val accent: Color,
    val scrim: Color,
    val tileA: Color,
    val tileB: Color,
)

private val AccentDark = listOf(Color(0xFFC8A96A), Color(0xFFD98BA0), Color(0xFF8FBF9F), Color(0xFF8DB4E8))
private val AccentLight = listOf(Color(0xFF7A6130), Color(0xFF9A4A62), Color(0xFF3F7A56), Color(0xFF3B6AA8))
val AccentNames = listOf("Brass", "Rose", "Sage", "Ice")
fun accentSwatch(i: Int): Color = AccentDark[i.coerceIn(0, AccentDark.size - 1)]

val LocalPal = staticCompositionLocalOf<Palette> { error("SambhavTheme is missing") }

@Composable
fun SambhavTheme(mode: Int, accent: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val i = accent.coerceIn(0, AccentDark.size - 1)
    val pal = remember(dark, i) {
        if (dark) Palette(true, Color(0xFF1B1A18), Color(0xFF242220), Color(0xFFF4EEE0), Color(0xFFB9B1A0),
            AccentDark[i], Color.Black, Color(0xFF2A2825), Color(0xFF171614))
        else Palette(false, Color(0xFFF4EEE0), Color(0xFFFFFBF2), Color(0xFF1B1A18), Color(0xFF5E584D),
            AccentLight[i], Color(0xFFF4EEE0), Color(0xFFF1E9D8), Color(0xFFE2D8C2))
    }
    val scheme = if (dark) darkColorScheme(primary = pal.accent, background = pal.bg, surface = pal.surface,
        onSurface = pal.text, onBackground = pal.text)
    else lightColorScheme(primary = pal.accent, background = pal.bg, surface = pal.surface,
        onSurface = pal.text, onBackground = pal.text)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPal provides pal, content = content)
    }
}
