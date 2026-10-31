package app.transfusionvs.data

import android.content.Context
import androidx.core.content.edit

/**
 * User settings (Settings screen). Defaults match the spec.
 * Stored in device-protected storage, like the database, so they work before first unlock.
 */
class Settings(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun int(key: String, default: Int, range: IntRange) = prefs.getInt(key, default).coerceIn(range)

    /** ± minutes around the scheduled time that still count as "on time". */
    var toleranceMin: Int
        get() = int("toleranceMin", 5, TOLERANCE_RANGE)
        set(v) = prefs.edit { putInt("toleranceMin", v.coerceIn(TOLERANCE_RANGE)) }

    var snoozeMin: Int
        get() = int("snoozeMin", 2, SNOOZE_RANGE)
        set(v) = prefs.edit { putInt("snoozeMin", v.coerceIn(SNOOZE_RANGE)) }

    var missedTimeoutMin: Int
        get() = int("missedTimeoutMin", 10, MISSED_RANGE)
        set(v) = prefs.edit { putInt("missedTimeoutMin", v.coerceIn(MISSED_RANGE)) }

    /** 0 = off; otherwise 1, 2 or 5 minutes before each checkpoint. */
    var preAlertMin: Int
        get() = prefs.getInt("preAlertMin", 0).takeIf { it in PRE_ALERT_CHOICES } ?: 0
        set(v) = prefs.edit { putInt("preAlertMin", if (v in PRE_ALERT_CHOICES) v else 0) }

    /** Maximum transfusion duration. */
    var limitMin: Int
        get() = int("limitMin", 240, LIMIT_RANGE)
        set(v) = prefs.edit { putInt("limitMin", v.coerceIn(LIMIT_RANGE)) }

    /** Warning this many minutes before the limit (always less than the limit). */
    var warnBeforeMin: Int
        get() = int("warnBeforeMin", 15, WARN_RANGE).coerceAtMost(limitMin - 5)
        set(v) = prefs.edit { putInt("warnBeforeMin", v.coerceIn(WARN_RANGE)) }

    /** False = Vibrate-only (night mode). */
    var soundOn: Boolean
        get() = prefs.getBoolean("soundOn", true)
        set(v) = prefs.edit { putBoolean("soundOn", v) }

    /** Chosen alarm sound; null = the phone's default alarm sound. */
    var alarmSoundUri: String?
        get() = prefs.getString("alarmSoundUri", null)
        set(v) = prefs.edit { putString("alarmSoundUri", v) }

    // ---- Repositioning ----

    /** Default ward interval for new plans: 60, 120 or 240 min. */
    var turnIntervalMin: Int
        get() = prefs.getInt("turnIntervalMin", 120).takeIf { it in TURN_INTERVALS } ?: 120
        set(v) = prefs.edit { putInt("turnIntervalMin", if (v in TURN_INTERVALS) v else 120) }

    /** ± minutes around a slot that still count as "on time". */
    var turnWindowMin: Int
        get() = int("turnWindowMin", 15, TURN_WINDOW_RANGE)
        set(v) = prefs.edit { putInt("turnWindowMin", v.coerceIn(TURN_WINDOW_RANGE)) }

    /** Turn alarm sound; null = the bundled turning chime (distinct from the transfusion alarm). */
    var turnSoundUri: String?
        get() = prefs.getString("turnSoundUri", null)
        set(v) = prefs.edit { putString("turnSoundUri", v) }

    var setupSeen: Boolean
        get() = prefs.getBoolean("setupSeen", false)
        set(value) = prefs.edit { putBoolean("setupSeen", value) }

    // ---- Defaults instead of questions ----

    /** The duration preselected on Start: the last one used. */
    var lastDurationMin: Int
        get() = prefs.getInt("lastDurationMin", 120)
        set(v) = prefs.edit { putInt("lastDurationMin", v) }

    // ---- Night theme ----

    /** See NightRule. By default night follows the clock. */
    var themeMode: Int
        get() = prefs.getInt("themeMode", 0)
        set(v) = prefs.edit { putInt("themeMode", v) }

    var nightStartHour: Int
        get() = int("nightStartHour", 20, HOUR_RANGE)
        set(v) = prefs.edit { putInt("nightStartHour", v.coerceIn(HOUR_RANGE)) }

    var nightEndHour: Int
        get() = int("nightEndHour", 8, HOUR_RANGE)
        set(v) = prefs.edit { putInt("nightEndHour", v.coerceIn(HOUR_RANGE)) }

    companion object {
        val HOUR_RANGE = 0..23
        const val MAX_SNOOZES = 3
        val TOLERANCE_RANGE = 1..15
        val SNOOZE_RANGE = 1..2
        val MISSED_RANGE = 5..30
        val LIMIT_RANGE = 60..480
        val WARN_RANGE = 5..60
        val PRE_ALERT_CHOICES = listOf(0, 1, 2, 5)
        val TURN_INTERVALS = listOf(60, 120, 240)
        val TURN_WINDOW_RANGE = 5..30
    }
}
