package app.transfusionvs.lab

import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.app.NotificationManager
import android.media.AudioManager
import android.net.Uri
import android.os.StrictMode
import android.os.Vibrator
import app.transfusionvs.App
import app.transfusionvs.alarm.AlarmService
import app.transfusionvs.app
import app.transfusionvs.data.Position
import app.transfusionvs.data.SkipReason
import app.transfusionvs.schedule.TurnGrid
import app.transfusionvs.ui.screens.TurnPlanDraft
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only remote control for lab/reliability.py (`am broadcast -n ...`). It drives the same
 * Engine entry points the screens use, and reads state back as JSON, so the emulator suite can
 * arrange a situation without typing into the UI. Never compiled into the release APK.
 */
class LabReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.app
        val pending = goAsync()
        app.scope.launch {
            try {
                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                when (intent.action) {
                    "lab.START_TRANSFUSION" -> {
                        val ago = intent.getIntExtra("ago", 0)
                        app.engine.startTransfusion(
                            intent.getStringExtra("bed") ?: "7654801",
                            now - ago * 60_000L,
                            intent.getIntExtra("dur", 120),
                        )
                    }
                    "lab.START_PLAN" -> {
                        val interval = intent.getIntExtra("interval", 60)
                        app.engine.startTurnPlan(
                            TurnPlanDraft(
                                bedLabel = intent.getStringExtra("bed") ?: "7654802",
                                intervalMin = interval,
                                rotation = Position.DEFAULT_ROTATION,
                                current = Position.BACK,
                                firstSlotAt = TurnGrid.nextSlotAfter(now, interval, zone),
                                stopAt = null,
                            ),
                        )
                    }
                    "lab.TEST_ALARM" -> app.engine.scheduleTest(intent.getIntExtra("seconds", 60) * 1000L)
                    "lab.ACK_ALL" -> app.engine.acknowledge(rows(app, "SELECT id FROM checkpoint WHERE status IN ('RINGING','MISSED')").map { it.getLong("id") })
                    "lab.SNOOZE" -> app.engine.snooze()
                    "lab.START_ROUND" -> app.engine.startRound()
                    "lab.TURN_ALL" -> rows(app, "SELECT id FROM turn_slot WHERE status='RINGING'").forEach { app.engine.turned(it.getLong("id"), Position.LEFT_30) }
                    "lab.CANT_TURN_ALL" -> rows(app, "SELECT id FROM turn_slot WHERE status='RINGING'").forEach { app.engine.cantTurn(it.getLong("id"), SkipReason.PROCEDURE) }
                    "lab.END_ALL" -> rows(app, "SELECT id FROM transfusion WHERE status='RUNNING'").forEach { app.engine.endTransfusion(it.getLong("id")) }
                    "lab.RINGER" -> context.getSystemService(AudioManager::class.java).ringerMode = when (intent.getStringExtra("mode")) {
                        "SILENT" -> AudioManager.RINGER_MODE_SILENT
                        "VIBRATE" -> AudioManager.RINGER_MODE_VIBRATE
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    "lab.DND" -> context.getSystemService(NotificationManager::class.java).setInterruptionFilter(
                        if (intent.getStringExtra("mode") == "off") NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                    )
                    "lab.DUMP_HEAP" -> leakcanary.LeakCanary.dumpHeap()
                    "lab.SET_MISSED_TIMEOUT" -> app.settings.missedTimeoutMin = intent.getIntExtra("min", 10)
                }
                pending.setResultData(dump(app, now, zone).toString())
            } catch (e: Exception) {
                pending.setResultData("""{"error":"${e.javaClass.simpleName}: ${e.message}"}""")
            } finally {
                pending.finish()
            }
        }
    }

    private fun dump(app: App, now: Long, zone: ZoneId): JSONObject = JSONObject()
        .put("now", now)
        .put("zone", zone.id)
        .put("service", AlarmService.isRunning)
        .put("hasVibrator", app.getSystemService(Vibrator::class.java).hasVibrator())
        .put("policyAccess", app.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted)
        .put("checkpoints", JSONArray(rows(app, "SELECT c.id,c.type,c.status,c.scheduledAt,c.firedAt,c.ackAt,c.snoozeCount FROM checkpoint c JOIN transfusion t ON t.id=c.transfusionId WHERE c.status!='CANCELLED' AND c.type!='START' ORDER BY c.scheduledAt LIMIT 12")))
        .put("slots", JSONArray(rows(app, "SELECT s.id,s.status,s.slotAt,s.firedAt,s.ackAt,s.silenced,s.skipReason FROM turn_slot s JOIN turn_plan p ON p.id=s.planId WHERE s.status!='CANCELLED' ORDER BY s.slotAt LIMIT 12")))

    private fun rows(app: App, sql: String): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        app.db.openHelper.readableDatabase.query(sql).use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                for (i in 0 until c.columnCount) {
                    when (c.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> o.put(c.getColumnName(i), c.getLong(i))
                        Cursor.FIELD_TYPE_NULL -> o.put(c.getColumnName(i), JSONObject.NULL)
                        else -> o.put(c.getColumnName(i), c.getString(i))
                    }
                }
                out += o
            }
        }
        return out
    }
}

/** A provider only because providers initialise before Application.onCreate. Logs every violation. */
class LabStrictMode : ContentProvider() {
    override fun onCreate(): Boolean {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}
