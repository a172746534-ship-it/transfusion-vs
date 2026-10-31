package app.transfusionvs.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.transfusionvs.R
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.schedule.Labels
import app.transfusionvs.ui.MainActivity

object Notifications {
    const val CH_ALARM = "alarm"
    const val CH_OVERDUE = "overdue_v2"
    const val CH_PREALERT = "prealert"

    const val ID_ALARM = 1
    private const val TAG_OVERDUE = "overdue"
    private const val TAG_PREALERT = "prealert"
    private const val TAG_TURN_OVERDUE = "overdue_turns"

    /** Three short pulses: overdue feels different from the alarm's long buzz and the ✓ double tap. */
    private val OVERDUE_PATTERN = longArrayOf(0, 180, 120, 180, 120, 180)

    private fun navy(context: Context) = ContextCompat.getColor(context, R.color.ward_navy)
    private fun red(context: Context) = ContextCompat.getColor(context, R.color.ward_overdue)

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
            vibrationPattern = OVERDUE_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val preAlert = NotificationChannel(CH_PREALERT, "Pre-alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Gentle heads-up a few minutes before a checkpoint"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
        }
        manager(context).createNotificationChannels(listOf(alarm, overdue, preAlert))
        manager(context).deleteNotificationChannel("overdue") // replaced by overdue_v2, with its own pattern
    }

    // ---- The ringing alarm ----

    /** Full-screen, category ALARM, not local-only (so bands and watches get it too). */
    fun alarm(context: Context, spec: RingSpec?): Notification {
        val fullScreen = alarmScreen(context)
        val builder = NotificationCompat.Builder(context, CH_ALARM)
            .setSmallIcon(if (spec?.turnOnly == true) R.drawable.ic_stat_turn else R.drawable.ic_stat_drop)
            .setColor(navy(context))
            .setContentTitle(spec?.title ?: "Transfusion VS")
            .setContentText(
                when {
                    spec == null -> "Checking alarms…"
                    spec.turnOnly -> spec.round?.beds.orEmpty().joinToString(" · ") { it.bedLabel }
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
            val round = spec.round
            if (spec.beds.size > 1 || round != null) {
                builder.setStyle(
                    NotificationCompat.InboxStyle().also { s ->
                        spec.beds.forEach { s.addLine(it.title) }
                        if (round != null) {
                            if (spec.beds.isNotEmpty()) s.addLine(RingSpec.roundTitle(round))
                            round.beds.forEach { s.addLine("${it.bedLabel}: ${it.from.label} → ${it.suggested.label}") }
                        }
                    },
                )
            }
            // One ✓ per bed (the notification has room for two beds plus Snooze).
            if (spec.beds.size == 1) {
                builder.addAction(0, Labels.ACK, ackIntent(context, spec.beds[0].checkpointIds))
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

    fun alarmScreen(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // ---- Turning round overdue (past the missed timer, still open until the next slot) ----

    /** One persistent "Overdue — Bed 3, Bed 7" listing the beds not ticked; gone once none is left. */
    fun syncTurnOverdue(context: Context, beds: List<String>, canRemind: Boolean) {
        if (beds.isEmpty()) {
            manager(context).cancel(TAG_TURN_OVERDUE, 0)
            return
        }
        val text = "Turn not done. Open the round and tick each bed."
        val builder = NotificationCompat.Builder(context, CH_OVERDUE)
            .setSmallIcon(R.drawable.ic_stat_turn)
            .setColor(red(context))
            .setContentTitle("Overdue — ${beds.joinToString(", ")}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(false)
            .setContentIntent(alarmScreen(context))
            .setDeleteIntent(receiverIntent(context, AlarmReceiver.ACTION_REPOST, "tvs://repost/turns"))
            .addAction(0, "Open round", alarmScreen(context))
        if (canRemind) {
            builder.addAction(0, "Remind me in 30 min", receiverIntent(context, AlarmReceiver.ACTION_REMIND, "tvs://remind"))
        }
        notifySafe(context, TAG_TURN_OVERDUE, 0, builder.build())
    }

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
                .setColor(red(context))
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
                .addAction(0, Labels.ACK, ackIntent(context, rows.map { it.id }))
                .build()
            notifySafe(context, TAG_OVERDUE, transfusionId.toInt(), n)
        }
    }

    // ---- Pre-alert ----

    fun preAlert(context: Context, row: CheckpointWithBed, minutesBefore: Int) {
        val n = NotificationCompat.Builder(context, CH_PREALERT)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(navy(context))
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

    fun ackIntent(context: Context, ids: List<Long>): PendingIntent {
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

    fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
