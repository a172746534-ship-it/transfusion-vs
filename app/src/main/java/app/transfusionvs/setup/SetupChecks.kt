package app.transfusionvs.setup

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.transfusionvs.alarm.Notifications

/** One line of the Setup check. [fix] is tried in order; the last resort is the app's info page. */
data class CheckItem(
    val title: String,
    val detail: String,
    val ok: Boolean,
    val fix: List<Intent>,
    /** Android 13+: ask with the system dialog first, before sending the user to Settings. */
    val askNotificationPermission: Boolean = false,
)

object SetupChecks {

    fun evaluate(context: Context): List<CheckItem> {
        val pkg = context.packageName
        val nm = context.getSystemService(NotificationManager::class.java)
        val am = context.getSystemService(AlarmManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)
        val audio = context.getSystemService(AudioManager::class.java)
        val packageUri = Uri.parse("package:$pkg")

        val items = mutableListOf<CheckItem>()

        // 1. Notifications (the alarm channel must not be blocked either)
        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val channelOn = nm.getNotificationChannel(Notifications.CH_ALARM)?.importance != NotificationManager.IMPORTANCE_NONE
        items += CheckItem(
            title = "Notifications allowed",
            detail = "Needed for the full-screen alarm and the Overdue reminder.",
            ok = NotificationManagerCompat.from(context).areNotificationsEnabled() && channelOn,
            askNotificationPermission = !permissionGranted,
            fix = listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
            ),
        )

        // 2. Exact alarms (always granted on Android 13+ through USE_EXACT_ALARM)
        items += CheckItem(
            title = "Exact alarms allowed",
            detail = "Lets every checkpoint ring at the exact minute, even in deep sleep.",
            ok = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms(),
            fix = if (Build.VERSION.SDK_INT >= 31) {
                listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
            } else {
                emptyList()
            },
        )

        // 3. Full-screen intent (Android 14+ can switch it off per app)
        items += CheckItem(
            title = "Full-screen alarm allowed",
            detail = "Shows the alarm over the lock screen.",
            ok = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent(),
            fix = if (Build.VERSION.SDK_INT >= 34) {
                listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
            } else {
                emptyList()
            },
        )

        // 4. Battery optimization
        items += CheckItem(
            title = "Battery optimization off",
            detail = "Stops Android from putting the app to sleep.",
            ok = pm.isIgnoringBatteryOptimizations(pkg),
            fix = listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            ),
        )

        // 5. Alarm volume
        items += CheckItem(
            title = "Alarm volume above 0",
            detail = "Alarms ring on silent and vibrate, but not if the alarm volume is at 0.",
            ok = audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0,
            fix = listOfNotNull(
                if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_VOLUME) else null,
                Intent(Settings.ACTION_SOUND_SETTINGS),
            ),
        )

        // 6. Do Not Disturb (Total silence mutes alarms too)
        items += CheckItem(
            title = "Do Not Disturb lets alarms through",
            detail = "\"Total silence\" mutes alarms. Use Alarms-only or allow alarms.",
            ok = dndAllowsAlarms(nm),
            fix = listOf(Intent("android.settings.ZEN_MODE_SETTINGS"), Intent(Settings.ACTION_SOUND_SETTINGS)),
        )

        return items
    }

    private fun dndAllowsAlarms(nm: NotificationManager): Boolean = when (nm.currentInterruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_NONE -> false
        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> runCatching {
            Build.VERSION.SDK_INT < 28 ||
                nm.notificationPolicy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
        }.getOrDefault(true)
        else -> true
    }

    /** Opens the first intent that works; falls back to this app's info page. */
    fun open(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next one
            } catch (e: SecurityException) {
                // OEM page not exported on this build — try the next one
            }
        }
        appInfo(context)
    }

    fun appInfo(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
