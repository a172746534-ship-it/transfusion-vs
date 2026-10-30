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
 * URI, so setting it again replaces it instead of duplicating:
 *   transfusion checkpoint: tvs://alarm/<kind>/<id>   (unchanged since v0.1, so alarms set by older
 *                                                     versions stay valid after an update)
 *   turn slot:              tvs://alarm/TURN/<kind>/<id>
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun set(request: AlarmRequest) {
        val op = pendingIntent(request.kind.name, request.checkpointId, request.turn, create = true)!!
        when (request.kind) {
            // The checkpoint and its snooze re-ring use setAlarmClock: the system never delays it
            // and leaves Doze to deliver it.
            AlarmKind.CHECKPOINT, AlarmKind.SNOOZE, AlarmKind.REMIND -> setAlarmClock(op, request.at)
            AlarmKind.TIMEOUT, AlarmKind.PREALERT, AlarmKind.MISS -> setExact(op, request.at)
        }
    }

    /** One immediate alarm that rings everything overdue together (used after boot, time change, edits). */
    fun setRingNow(now: Long) {
        setAlarmClock(pendingIntent(KIND_RING_NOW, 0, turn = false, create = true)!!, now + 1_000)
    }

    fun cancel(checkpointId: Long) = cancel(checkpointId, turn = false)

    fun cancelTurn(slotId: Long) = cancel(slotId, turn = true)

    private fun cancel(id: Long, turn: Boolean) {
        for (kind in AlarmKind.entries) {
            pendingIntent(kind.name, id, turn, create = false)?.let {
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

    private fun pendingIntent(kind: String, id: Long, turn: Boolean, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .setData(Uri.parse(if (turn) "tvs://alarm/TURN/$kind/$id" else "tvs://alarm/$kind/$id"))
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        const val KIND_RING_NOW = "RING_NOW"

        /** Parses an alarm URI back into (kind, id, turn). */
        fun parse(data: Uri?): Parsed? {
            val s = data?.pathSegments ?: return null
            if (data.host != "alarm") return null
            return when {
                s.size == 2 -> Parsed(s[0], s[1].toLongOrNull() ?: return null, turn = false)
                s.size == 3 && s[0] == "TURN" -> Parsed(s[1], s[2].toLongOrNull() ?: return null, turn = true)
                else -> null
            }
        }
    }

    data class Parsed(val kind: String, val id: Long, val turn: Boolean)
}
