package app.transfusionvs.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.transfusionvs.app
import app.transfusionvs.schedule.AlarmKind
import kotlinx.coroutines.launch

/** Our own alarms and notification buttons. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.app
        val pending = goAsync() // keeps the phone awake until the work below is done
        app.scope.launch {
            try {
                when (intent.action) {
                    ACTION_ALARM -> {
                        val parsed = AlarmScheduler.parse(intent.data) ?: return@launch
                        when {
                            // A custom alarm's id is not a checkpoint's: the same number can be both.
                            parsed.kind == AlarmKind.PREALERT.name && parsed.custom -> app.engine.onCustomPreAlert(parsed.id)
                            parsed.kind == AlarmKind.PREALERT.name -> app.engine.onPreAlert(parsed.id)
                            else -> app.engine.onAlarm()
                        }
                    }
                    ACTION_ACK -> app.engine.acknowledge(intent.getLongArrayExtra(EXTRA_IDS)?.toList().orEmpty())
                    ACTION_ACK_CUSTOM -> app.engine.acknowledgeCustom(intent.getLongArrayExtra(EXTRA_IDS)?.toList().orEmpty())
                    ACTION_SNOOZE -> app.engine.snooze()
                    ACTION_REPOST -> app.engine.refreshNotifications()
                    ACTION_REMIND -> app.engine.remindTurns()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "app.transfusionvs.action.ALARM"
        const val ACTION_ACK = "app.transfusionvs.action.ACK"
        const val ACTION_ACK_CUSTOM = "app.transfusionvs.action.ACK_CUSTOM"
        const val ACTION_SNOOZE = "app.transfusionvs.action.SNOOZE"
        const val ACTION_REPOST = "app.transfusionvs.action.REPOST"
        const val ACTION_REMIND = "app.transfusionvs.action.REMIND"
        const val EXTRA_IDS = "ids"
    }
}

/**
 * Re-arms every alarm from the database after: reboot (also before the first unlock), an app
 * update, a time or timezone change, and an exact-alarm permission change.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val app = context.app
        val pending = goAsync()
        app.scope.launch {
            try {
                app.engine.rearm()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, // "android.intent.action.TIME_SET"
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            "android.intent.action.QUICKBOOT_POWERON", // some OEMs' fast boot
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
