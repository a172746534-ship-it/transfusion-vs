package app.transfusionvs.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.transfusionvs.schedule.AlarmKind
import app.transfusionvs.schedule.AlarmRequest
import app.transfusionvs.ui.MainActivity

/**
 * Thin wrapper over AlarmManager. Every alarm has its own PendingIntent, made unique by its data
 * URI (tvs://alarm/<kind>/<checkpointId>), so setting it again replaces it instead of duplicating.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun set(request: AlarmRequest) {
        val op = pendingIntent(request.kind.name, request.checkpointId, create = true)!!
        when (request.kind) {
            // The checkpoint and its snooze re-ring use setAlarmClock: the system never delays it
            // and leaves Doze to deliver it.
            AlarmKind.CHECKPOINT, AlarmKind.SNOOZE -> setAlarmClock(op, request.at)
            AlarmKind.TIMEOUT, AlarmKind.PREALERT -> setExact(op, request.at)
        }
    }

    /** One immediate alarm that rings everything overdue together (used after boot, time change, edits). */
    fun setRingNow(now: Long) {
        setAlarmClock(pendingIntent(KIND_RING_NOW, 0, create = true)!!, now + 1_000)
    }

    fun cancelRingNow() {
        pendingIntent(KIND_RING_NOW, 0, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun cancel(checkpointId: Long) {
        for (kind in AlarmKind.entries) {
            pendingIntent(kind.name, checkpointId, create = false)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun setAlarmClock(op: PendingIntent, at: Long) {
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), op)
        } catch (e: SecurityException) {
            // Only possible on Android 12 if the user revoked "Alarms & reminders" (Setup check shows it red).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    private fun setExact(op: PendingIntent, at: Long) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }

    private fun pendingIntent(kind: String, checkpointId: Long, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .setData(Uri.parse("tvs://alarm/$kind/$checkpointId"))
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        const val KIND_RING_NOW = "RING_NOW"

        /** Parses tvs://alarm/<kind>/<id> back into (kind, id). */
        fun parse(data: Uri?): Pair<String, Long>? {
            val segments = data?.pathSegments ?: return null
            if (data.host != "alarm" || segments.size != 2) return null
            return segments[0] to (segments[1].toLongOrNull() ?: return null)
        }
    }
}
