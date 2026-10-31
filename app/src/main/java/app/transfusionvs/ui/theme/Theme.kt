package app.transfusionvs.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.transfusionvs.schedule.Urgency

/**
 * Ward Card colours, designed together for day and night. The values come from lab/palettes.json,
 * which lab/color_check.py verifies against the fixed thresholds; WardColorsTest keeps them identical.
 * Red (overdue, danger) means danger only and is never decorative.
 */
@Immutable
data class WardColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val calm: Color,
    val onCalm: Color,
    val soon: Color,
    val onSoon: Color,
    val now: Color,
    val onNow: Color,
    val overdue: Color,
    val onOverdue: Color,
    val danger: Color,
    val onDanger: Color,
    val typeTx: Color,
    val typeRp: Color,
    val alarmBg: Color,
    val alarmInk: Color,
    val alarmAction: Color,
    val alarmOnAction: Color,
    val dangerFill: Color,
    val onDangerFill: Color,
    val dangerEdge: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryEdge: Color,
    /** The header band: the navy of the card by day, a dark card by night. */
    val header: Color,
    val onHeader: Color,
    val headerCaps: Color,
    val alarmCaps: Color,
    /** "1 MEASURE" on the alarm screen. */
    val alarmStep: Color,
    val isNight: Boolean,
) {
    val track: Color get() = muted.copy(alpha = 0.3f)
    val alarmMuted: Color get() = alarmInk.copy(alpha = 0.8f)
    val alarmTint: Color get() = alarmInk.copy(alpha = 0.09f)
    val alarmEdge: Color get() = alarmInk.copy(alpha = 0.35f)
    val onHeaderMuted: Color get() = onHeader.copy(alpha = 0.85f)

    fun of(u: Urgency): Color = when (u) {
        Urgency.CALM -> calm
        Urgency.SOON -> soon
        Urgency.NOW -> now
        Urgency.OVERDUE -> overdue
    }

    fun on(u: Urgency): Color = when (u) {
        Urgency.CALM -> onCalm
        Urgency.SOON -> onSoon
        Urgency.NOW -> onNow
        Urgency.OVERDUE -> onOverdue
    }
}

val WardDay = WardColors(
    bg = Color(0xFFEEF1F6), card = Color(0xFFFFFFFF), ink = Color(0xFF13233F), muted = Color(0xFF4C5870),
    calm = Color(0xFF2F6FD0), onCalm = Color(0xFFFFFFFF), soon = Color(0xFFD28000), onSoon = Color(0xFF13233F),
    now = Color(0xFF13233F), onNow = Color(0xFFFFFFFF), overdue = Color(0xFFA80F1C), onOverdue = Color(0xFFFFFFFF),
    danger = Color(0xFFA80F1C), onDanger = Color(0xFFFFFFFF), typeTx = Color(0xFF8A5A00), typeRp = Color(0xFF0F6E74),
    alarmBg = Color(0xFF13233F), alarmInk = Color(0xFFFFFFFF), alarmAction = Color(0xFFF5A623), alarmOnAction = Color(0xFF13233F),
    dangerFill = Color(0xFFA80F1C), onDangerFill = Color(0xFFFFFFFF), dangerEdge = Color(0xFFA80F1C),
    primary = Color(0xFF13233F), onPrimary = Color(0xFFFFFFFF), primaryEdge = Color(0xFF13233F),
    header = Color(0xFF13233F), onHeader = Color(0xFFFFFFFF), headerCaps = Color(0xFFF5A623),
    alarmCaps = Color(0xFFF5A623), alarmStep = Color(0xFF8FB4F5), isNight = false,
)

val WardNight = WardColors(
    bg = Color(0xFF0A1220), card = Color(0xFF141F33), ink = Color(0xFFE8EDF6), muted = Color(0xFFA5B1C6),
    calm = Color(0xFF7585A4), onCalm = Color(0xFF0A1220), soon = Color(0xFFF9DC6E), onSoon = Color(0xFF0A1220),
    now = Color(0xFFA6C4FF), onNow = Color(0xFF0A1220), overdue = Color(0xFFFF7269), onOverdue = Color(0xFF0A1220),
    danger = Color(0xFFFF7269), onDanger = Color(0xFF0A1220), typeTx = Color(0xFFD9A44A), typeRp = Color(0xFF5CC0C4),
    alarmBg = Color(0xFF060B14), alarmInk = Color(0xFFE8EDF6), alarmAction = Color(0xFFC98F36), alarmOnAction = Color(0xFF060B14),
    dangerFill = Color(0xFF4A1518), onDangerFill = Color(0xFFFFDAD6), dangerEdge = Color(0xFFFF7269),
    primary = Color(0xFF1E2C47), onPrimary = Color(0xFFE8EDF6), primaryEdge = Color(0xFF7585A4),
    header = Color(0xFF141F33), onHeader = Color(0xFFE8EDF6), headerCaps = Color(0xFFF9DC6E),
    alarmCaps = Color(0xFFC98F36), alarmStep = Color(0xFFA6C4FF), isNight = true,
)

private val LocalWardColors = staticCompositionLocalOf { WardDay }

/** The current theme's colours. */
object Ward {
    val c: WardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWardColors.current
}

private fun scheme(c: WardColors): ColorScheme =
    if (c.isNight) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.soon, onSecondary = c.onSoon, tertiary = c.now,
            error = c.danger, onError = c.onDanger, background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.bg, onSurfaceVariant = c.muted, outline = c.muted, surfaceContainerHigh = c.card,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.soon, onSecondary = c.onSoon, tertiary = c.calm,
            error = c.danger, onError = c.onDanger, background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.bg, onSurfaceVariant = c.muted, outline = c.muted, surfaceContainerHigh = c.card,
        )
    }

/**
 * [night] picks the night palette. The UI language is English on every phone, so the layout never
 * mirrors, whatever the phone's language.
 */
@Composable
fun TvsTheme(night: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (night) WardNight else WardDay
    CompositionLocalProvider(LocalWardColors provides colors, LocalLayoutDirection provides LayoutDirection.Ltr) {
        MaterialTheme(colorScheme = scheme(colors), typography = TvsTypography, content = content)
    }
}
