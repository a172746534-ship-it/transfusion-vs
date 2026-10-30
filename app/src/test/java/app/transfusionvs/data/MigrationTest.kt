package app.transfusionvs.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import app.transfusionvs.alarm.AlarmScheduler
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * The update from v1 (transfusions only, what phones have today) to v2 must keep everything:
 * a running transfusion and its pending checkpoints. The v1 database is built from the exact schema
 * exported by that version (app/schemas), then opened by the current code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private val name = "migration_test.db"

    private fun createV1(context: Context) {
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/app.transfusionvs.data.AppDatabase/1.json").readText()).getJSONObject("database")
        val db = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val table = e.getString("tableName")
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: continue // tables without indices omit the key
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))

        db.execSQL(
            "INSERT INTO transfusion (id, bedLabel, colorIndex, startAt, plannedDurationMin, endAt, status, baselineConfirmed, isTest) " +
                "VALUES (7, 'Bed 12', 2, 1000, 120, NULL, 'RUNNING', 1, 0)",
        )
        db.execSQL(
            "INSERT INTO checkpoint (id, transfusionId, type, offsetMin, scheduledAt, firedAt, ackAt, snoozeCount, snoozeUntil, status) " +
                "VALUES (70, 7, 'M15', 15, 901000, 901000, 950000, 1, NULL, 'ACKED')",
        )
        db.execSQL(
            "INSERT INTO checkpoint (id, transfusionId, type, offsetMin, scheduledAt, firedAt, ackAt, snoozeCount, snoozeUntil, status) " +
                "VALUES (71, 7, 'M30', 30, 1801000, NULL, NULL, 0, NULL, 'PENDING')",
        )
        db.version = 1
        db.close()
    }

    @Test
    fun updatingFromV1KeepsRunningTransfusionsAndPendingCheckpoints() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        createV1(context)

        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val running = room.dao().runningTransfusions()
            assertEquals(1, running.size)
            assertEquals("Bed 12", running[0].bedLabel)
            assertEquals(1000L, running[0].startAt)

            val rows = room.dao().checkpointsFor(7)
            assertEquals(listOf(70L, 71L), rows.map { it.id })
            assertEquals(CheckpointStatus.ACKED, rows[0].status)
            assertEquals(950000L, rows[0].ackAt)
            assertEquals(CheckpointStatus.PENDING, rows[1].status)
            assertEquals(1801000L, rows[1].scheduledAt)

            // The new tables exist and work.
            assertTrue(room.turnDao().allPlans().isEmpty())
            val planId = room.turnDao().insertPlan(
                TurnPlan(
                    bedLabel = "Bed 3", colorIndex = 0, intervalMin = 120, startAt = 1, firstSlotAt = 2,
                    rotation = Position.toCsv(Position.DEFAULT_ROTATION), currentPosition = Position.BACK,
                ),
            )
            room.turnDao().insertSlots(listOf(TurnSlot(planId = planId, slotAt = 2, positionFrom = Position.BACK)))
            assertEquals(1, room.turnDao().slotsFor(planId).size)
        } finally {
            room.close()
        }
    }

    /** Alarms already set by v0.3.x keep their addresses, so they still fire after the update. */
    @Test
    fun oldAlarmAddressesStillParse_andTurnAlarmsHaveTheirOwn() {
        val old = AlarmScheduler.parse(Uri.parse("tvs://alarm/CHECKPOINT/71"))!!
        assertEquals("CHECKPOINT", old.kind)
        assertEquals(71L, old.id)
        assertFalse(old.turn)

        val turn = AlarmScheduler.parse(Uri.parse("tvs://alarm/TURN/CHECKPOINT/71"))!!
        assertEquals("CHECKPOINT", turn.kind)
        assertEquals(71L, turn.id)
        assertTrue(turn.turn)
    }
}
