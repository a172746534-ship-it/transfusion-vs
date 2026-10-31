package app.transfusionvs.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.transfusionvs.App
import app.transfusionvs.data.Settings
import app.transfusionvs.schedule.NightRule
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** For code outside composition, e.g. dimming the alarm window. */
fun nightNow(settings: Settings, configuration: Configuration): Boolean = NightRule.isNight(
    settings.themeMode,
    LocalTime.now().hour,
    settings.nightStartHour,
    settings.nightEndHour,
    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
)

/** Follows the clock (checked every minute), the phone's setting, and changes made in Settings. */
@Composable
fun rememberNight(app: App): Boolean {
    val now by rememberNow(60_000)
    app.themeVersion.intValue // recomposes when Settings changes the rule
    val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    val s = app.settings
    return NightRule.isNight(s.themeMode, hour, s.nightStartHour, s.nightEndHour, isSystemInDarkTheme())
}
