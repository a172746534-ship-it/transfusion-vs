package app.transfusionvs.data

import android.content.Context
import androidx.core.content.edit

/**
 * User settings (the Settings screen arrives in Phase 3; the defaults below match the spec).
 * Stored in device-protected storage, like the database, so they work before first unlock.
 */
class Settings(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    val toleranceMin: Int get() = prefs.getInt("toleranceMin", 5)
    val snoozeMin: Int get() = prefs.getInt("snoozeMin", 2)
    val missedTimeoutMin: Int get() = prefs.getInt("missedTimeoutMin", 10)
    /** 0 = off; otherwise 1, 2 or 5 minutes before each checkpoint. */
    val preAlertMin: Int get() = prefs.getInt("preAlertMin", 0)
    val limitMin: Int get() = prefs.getInt("limitMin", 240)
    val warnBeforeMin: Int get() = prefs.getInt("warnBeforeMin", 15)
    /** False = Vibrate-only (night mode). */
    val soundOn: Boolean get() = prefs.getBoolean("soundOn", true)
    /** Chosen alarm sound; null = the phone's default alarm sound. */
    val alarmSoundUri: String? get() = prefs.getString("alarmSoundUri", null)

    var setupSeen: Boolean
        get() = prefs.getBoolean("setupSeen", false)
        set(value) = prefs.edit { putBoolean("setupSeen", value) }

    companion object {
        const val MAX_SNOOZES = 3
    }
}
