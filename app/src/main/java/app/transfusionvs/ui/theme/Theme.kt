package app.transfusionvs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colours sampled from the card images in /design. */
object Tvs {
    val Navy = Color(0xFF13233F)
    val Red = Color(0xFFB3121F)
    val Pink = Color(0xFFFDECEC)
    val Amber = Color(0xFFF5A623)
    val Blue = Color(0xFF1D4E9E)
    val Panel = Color(0xFFF5F7FB)
    val Muted = Color(0xFF5B6577)
    val Outline = Color(0xFFD0D3D9)
    val White = Color(0xFFFFFFFF)
    /** Setup check "OK" and "on time" (not on the card). */
    val Green = Color(0xFF1E8E3E)

    /** Auto-assigned bed colours. Red is kept for reactions and overdue only. */
    val BedColors = listOf(
        Color(0xFF1D4E9E), // blue
        Color(0xFFF5A623), // amber
        Color(0xFF0F8A84), // teal
        Color(0xFF6B4FBB), // purple
        Color(0xFF2E7D32), // green
        Color(0xFFB0367C), // magenta
        Color(0xFFE8711A), // orange
        Color(0xFF546E7A), // slate
    )

    fun bedColor(index: Int): Color = BedColors[Math.floorMod(index, BedColors.size)]
}

private val colors = lightColorScheme(
    primary = Tvs.Navy,
    onPrimary = Tvs.White,
    secondary = Tvs.Amber,
    onSecondary = Tvs.Navy,
    tertiary = Tvs.Blue,
    error = Tvs.Red,
    background = Tvs.Panel,
    onBackground = Tvs.Navy,
    surface = Tvs.White,
    onSurface = Tvs.Navy,
    surfaceVariant = Tvs.Panel,
    onSurfaceVariant = Tvs.Muted,
    outline = Tvs.Outline,
)

/** Light theme only (the alarm screen draws its own navy background). */
@Composable
fun TvsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = TvsTypography, content = content)
}
