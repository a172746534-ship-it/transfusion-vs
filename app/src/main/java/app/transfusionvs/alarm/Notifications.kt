package app.transfusionvs.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import app.transfusionvs.R
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.MainActivity

object Notifications {
    const val CH_ALARM = "alarm"
    const val CH_OVERDUE = "overdue"
    const val CH_PREALERT = "prealert"

    const val ID_ALARM = 1
    private const val TAG_OVERDUE = "overdue"
    private const val TAG_PREALERT = "prealert"

    private const val NAVY = 0xFF13233F.toInt()
    private const val RED = 0xFFB3121F.toInt()

    fun createChannels(context: Context) {
        val alarm = NotificationChannel(CH_ALARM, "Checkpoint alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Full-screen alarm when vital signs are due"
            setSound(null, null) // the alarm service plays the sound itself, as USAGE_ALARM
            enableVibration(false) // …and vibrates itself
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val overdue = NotificationChannel(CH_OVERDUE, "Overdue checkpoints", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Stays until the missed checkpoint is acknowledged"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val preAlert = NotificationChannel(CH_PREALERT, "Pre-alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Gentle heads-up a few minutes before a checkpoint"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
        }
        manager(context).createNotificationChannels(listOf(alarm, overdue, preAlert))
    }

    // ---- The ringing alarm ----

    /** Full-screen, category ALARM, not local-only (so bands and watches get it too). */
    fun alarm(context: Context, spec: RingSpec?): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(NAVY)
            .setContentTitle(spec?.title ?: "Transfusion VS")
            .setContentText(
                when {
                    spec == null -> "Checking alarms…"
                    spec.beds.size == 1 -> spec.beds[0].subtitle
                    else -> spec.beds.joinToString(" · ") { it.shortLabel }
                },
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setDeleteIntent(receiverIntent(context, AlarmReceiver.ACTION_REPOST, "tvs://repost/alarm"))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (spec != null) {
            if (spec.beds.size > 1) {
                builder.setStyle(NotificationCompat.InboxStyle().also { s -> spec.beds.forEach { s.addLine(it.title) } })
            }
            // One ✓ per bed (the notification has room for two beds plus Snooze).
            if (spec.beds.size == 1) {
                builder.addAction(0, "Measured & documented ✓", ackIntent(context, spec.beds[0].checkpointIds))
            } else if (spec.beds.size == 2) {
                spec.beds.forEach { builder.addAction(0, "✓ ${it.shortLabel}", ackIntent(context, it.checkpointIds)) }
            }
            if (spec.canSnooze) {
                builder.addAction(
                    0, "Snooze ${spec.snoozeMin} min",
                    receiverIntent(context, AlarmReceiver.ACTION_SNOOZE, "tvs://snooze"),
                )
            }
        }
        return builder.build()
    }

    /** Posts or updates the alarm notification (also the fallback if the service can't start). */
    fun notifyAlarm(context: Context, spec: RingSpec) {
        notifySafe(context, null, ID_ALARM, alarm(context, spec))
    }

    fun cancelAlarm(context: Context) = manager(context).cancel(ID_ALARM)

    // ---- Overdue (missed, not yet acknowledged) ----

    /** One persistent "Overdue — Bed X" notification per bed, until it is acknowledged. */
    fun syncOverdue(context: Context, missed: List<CheckpointWithBed>) {
        val nm = manager(context)
        val byBed = missed.groupBy { it.transfusionId }
        val keep = byBed.keys.map { it.toInt() }.toSet()
        nm.activeNotifications
            .filter { it.tag == TAG_OVERDUE && it.id !in keep }
            .forEach { nm.cancel(TAG_OVERDUE, it.id) }

        for ((transfusionId, rows) in byBed) {
            val first = rows.first()
            val bed = if (first.isTest) "Test alarm" else first.bedLabel
            val what = rows.joinToString(", ") { "${Labels.title(it.type, it.offsetMin)} (${Labels.clock(it.scheduledAt)})" }
            val n = NotificationCompat.Builder(context, CH_OVERDUE)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setColor(RED)
                .setContentTitle("Overdue — $bed")
                .setContentText("$what not acknowledged. Tap ✓ once measured & documented.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$what not acknowledged. Tap ✓ once measured & documented."))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setLocalOnly(false)
                .setContentIntent(openApp(context))
                .setDeleteIntent(receiverIntent(context, AlarmReceiver.ACTION_REPOST, "tvs://repost/overdue/$transfusionId"))
                .addAction(0, "Measured & documented ✓", ackIntent(context, rows.map { it.id }))
                .build()
            notifySafe(context, TAG_OVERDUE, transfusionId.toInt(), n)
        }
    }

    // ---- Pre-alert ----

    fun preAlert(context: Context, row: CheckpointWithBed, minutesBefore: Int) {
        val n = NotificationCompat.Builder(context, CH_PREALERT)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(NAVY)
            .setContentTitle("In $minutesBefore min: ${Labels.title(row.type, row.offsetMin)} — ${row.bedLabel}")
            .setContentText("Due at ${Labels.clock(row.scheduledAt)}")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setLocalOnly(false)
            .setTimeoutAfter((row.scheduledAt - System.currentTimeMillis()).coerceAtLeast(1_000))
            .setContentIntent(openApp(context))
            .build()
        notifySafe(context, TAG_PREALERT, row.id.toInt(), n)
    }

    // ---- Helpers ----

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    private fun notifySafe(context: Context, tag: String?, id: Int, n: Notification) {
        // Throws only if notifications are blocked on some OEM builds; Setup check shows that red.
        runCatching { manager(context).notify(tag, id, n) }
    }

    private fun ackIntent(context: Context, ids: List<Long>): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ACK)
            .setData(Uri.parse("tvs://ack/${ids.joinToString(",")}"))
            .putExtra(AlarmReceiver.EXTRA_IDS, ids.toLongArray())
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun receiverIntent(context: Context, action: String, data: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setAction(action).setData(Uri.parse(data)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
