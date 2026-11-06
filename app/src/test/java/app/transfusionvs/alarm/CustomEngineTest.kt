package app.transfusionvs.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import app.transfusionvs.data.AppDatabase
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.CustomDraft
import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.Position
import app.transfusionvs.data.Settings
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.schedule.MINUTE_MS
import app.transfusionvs.schedule.ScheduleCalculator
import app.transfusionvs.schedule.TurnGrid
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Custom timers through the real engine and Room: start, duplicates, edit, stop, undo, retention,
 * masking, and the same id as a checkpoint, a turn slot and a custom alarm at once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomEngineTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var engine: Engine
    private val now get() = System.currentTimeMillis()
    private val minute get() = ScheduleCalculator.truncateToMinute(now)

    /** The next slot on the turning grid: a pending turn slot anywhere else is rebuilt by the turn source. */
    private val grid get() = TurnGrid.nextSlotAfter(now, 120, ZoneId.systemDefault())

    @Before
    fun open() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        engine = Engine(context, db.dao(), db.turnDao(), db.wardDao(), Settings(context), AlarmScheduler(context))
    }

    @After
    fun close() = db.close()

    // ---- Helpers ----

    /** Whether the system alarm with this address is set. */
    private fun armed(uri: String) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_ALARM).setData(Uri.parse(uri)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
    ) != null

    private suspend fun timers() = db.wardDao().allCustomTimers()
    private suspend fun slotsOf(id: Long) = db.wardDao().customSlotsFor(id)

    private suspend fun checkpoint(id: Long, status: CheckpointStatus, at: Long): Checkpoint {
        val t = db.dao().insertTransfusion(
            Transfusion(bedLabel = "7654801", colorIndex = 0, startAt = at - 15 * MINUTE_MS, plannedDurationMin = 120, baselineConfirmed = true),
        )
        db.dao().insertCheckpoints(
            listOf(
                Checkpoint(
                    id = id, transfusionId = t, type = CheckpointType.M15, offsetMin = 15, scheduledAt = at,
                    firedAt = if (status == CheckpointStatus.RINGING) at else null, status = status,
                ),
            ),
        )
        return db.dao().checkpoints(listOf(id)).single()
    }

    private suspend fun turnSlot(id: Long, status: TurnSlotStatus, at: Long): TurnSlot {
        val plan = db.turnDao().insertPlan(
            TurnPlan(
                bedLabel = "7654802", colorIndex = 1, intervalMin = 120, startAt = at - 60 * MINUTE_MS, firstSlotAt = at,
                rotation = Position.toCsv(Position.DEFAULT_ROTATION), currentPosition = Position.BACK,
            ),
        )
        db.turnDao().insertSlots(
            listOf(TurnSlot(id = id, planId = plan, slotAt = at, firedAt = if (status == TurnSlotStatus.RINGING) at else null, positionFrom = Position.BACK, status = status)),
        )
        return db.turnDao().slots(listOf(id)).single()
    }

    private suspend fun customSlot(id: Long, status: CustomSlotStatus, at: Long, bed: String = "7654803", name: String = "Check drain"): Pair<CustomTimer, CustomSlot> {
        val start = at - 60 * MINUTE_MS
        val timer = db.wardDao().insertCustomTimer(
            CustomTimer(bedLabel = bed, name = name, intervalMin = 60, startAt = start, anchorAt = start, fromAt = start),
        )
        db.wardDao().insertCustomSlots(
            listOf(CustomSlot(id = id, timerId = timer, slotAt = at, firedAt = if (status == CustomSlotStatus.RINGING) at else null, status = status)),
        )
        return db.wardDao().customTimer(timer)!! to db.wardDao().customSlots(listOf(id)).single()
    }

    private suspend fun cp(id: Long) = db.dao().checkpoints(listOf(id)).single()
    private suspend fun ts(id: Long) = db.turnDao().slots(listOf(id)).single()
    private suspend fun cs(id: Long) = db.wardDao().customSlots(listOf(id)).single()

    // ---- Start, duplicates, several per patient ----

    @Test
    fun startingMakesTheFirstAlarmOneIntervalLater_andArmsIt() = runBlocking {
        assertNull(engine.startCustom(CustomDraft("7654812", "Check drain", 30)))
        val t = timers().single()
        assertEquals(minute, t.startAt)
        assertEquals(CustomTimerStatus.RUNNING, t.status)
        val first = slotsOf(t.id).single()
        assertEquals(t.startAt + 30 * MINUTE_MS, first.slotAt)
        assertEquals(CustomSlotStatus.PENDING, first.status)
        assertTrue(armed("tvs://alarm/CUSTOM/CHECKPOINT/${first.id}"))
        assertFalse(armed("tvs://alarm/CHECKPOINT/${first.id}"))
    }

    @Test
    fun theSameName_caseAndSpacesIgnored_opensTheExistingTimerAndChangesNothing() = runBlocking {
        engine.startCustom(CustomDraft("7654812", "Check drain", 30))
        val first = timers().single()
        val before = slotsOf(first.id)
        assertEquals(first.id, engine.startCustom(CustomDraft("7654812", "  check   DRAIN ", 120)))
        assertEquals(listOf(first), timers())
        assertEquals(before, slotsOf(first.id))
    }

    @Test
    fun theSameNameOnAnotherPatient_andSeveralTimersForOnePatient_areAllowed() = runBlocking {
        assertNull(engine.startCustom(CustomDraft("7654812", "Check drain", 30)))
        assertNull(engine.startCustom(CustomDraft("7654813", "Check drain", 30)))
        assertNull(engine.startCustom(CustomDraft("7654812", "Blood sugar", 60)))
        assertEquals(3, timers().size)
        assertEquals(2, timers().count { it.bedLabel == "7654812" })
        timers().forEach { assertEquals(1, slotsOf(it.id).count { s -> s.status == CustomSlotStatus.PENDING }) }
    }

    // ---- Edit ----

    @Test
    fun aRenameOrAnotherMrn_keepsTheSchedule() = runBlocking {
        engine.startCustom(CustomDraft("7654812", "Check drain", 60))
        val t = timers().single()
        val slot = slotsOf(t.id).single()
        engine.editCustom(t.id, CustomDraft("7654899", "Drain output", 60))
        val e = timers().single()
        assertEquals("7654899", e.bedLabel)
        assertEquals("Drain output", e.name)
        assertEquals(t.anchorAt, e.anchorAt)
        assertEquals(t.fromAt, e.fromAt)
        assertEquals(listOf(slot), slotsOf(t.id))
    }

    @Test
    fun aNewInterval_reschedulesFromNow_neverInThePast() = runBlocking {
        engine.startCustom(CustomDraft("7654812", "Check drain", 60))
        val t = timers().single()
        val old = slotsOf(t.id).single()
        engine.editCustom(t.id, CustomDraft("7654812", "Check drain", 15))
        val next = slotsOf(t.id).single()
        assertEquals(t.startAt + 15 * MINUTE_MS, next.slotAt)
        assertTrue(next.slotAt > now)
        assertTrue(old.id != next.id)
        assertFalse(armed("tvs://alarm/CUSTOM/CHECKPOINT/${old.id}"))
        assertTrue(armed("tvs://alarm/CUSTOM/CHECKPOINT/${next.id}"))
    }

    // ---- Stop, undo ----

    @Test
    fun stopLeavesHomeAndCancelsAlarms_undoPutsItBackExactly() = runBlocking {
        engine.startCustom(CustomDraft("7654812", "Check drain", 60))
        val t = timers().single()
        val slot = slotsOf(t.id).single()
        engine.stopCustom(t.id)
        assertEquals(CustomTimerStatus.STOPPED, timers().single().status)
        assertTrue(db.wardDao().customHomeOnce().isEmpty())
        assertFalse(armed("tvs://alarm/CUSTOM/CHECKPOINT/${slot.id}"))

        engine.undo(db.wardDao().undoAll().first().id)
        val back = timers().single()
        assertEquals(t, back)
        val next = slotsOf(t.id).single { it.status == CustomSlotStatus.PENDING }
        assertEquals(slot.slotAt, next.slotAt)
        assertTrue(armed("tvs://alarm/CUSTOM/CHECKPOINT/${next.id}"))
    }

    @Test
    fun undoingAStartRemovesTheTimer() = runBlocking {
        engine.startCustom(CustomDraft("7654812", "Check drain", 60))
        val slot = slotsOf(timers().single().id).single()
        engine.undo(db.wardDao().undoAll().first().id)
        assertTrue(timers().isEmpty())
        assertFalse(armed("tvs://alarm/CUSTOM/CHECKPOINT/${slot.id}"))
    }

    @Test
    fun doneThenUndo_isTheExactRow() = runBlocking {
        val (_, before) = customSlot(40, CustomSlotStatus.RINGING, minute)
        engine.rearm()
        val ringing = cs(40)
        engine.acknowledgeCustom(listOf(40))
        assertEquals(CustomSlotStatus.ACKED, cs(40).status)
        assertEquals("7654803 · Check drain done", db.wardDao().undoAll().first().label)
        engine.undo(db.wardDao().undoAll().first().id)
        assertEquals(ringing, cs(40))
        assertEquals(before.slotAt, cs(40).slotAt)
    }

    // ---- No record-keeping ----

    @Test
    fun aStoppedTimerIsGoneADayLater_withItsMrnAndName() = runBlocking {
        val old = now - Engine.RETENTION_MS - MINUTE_MS
        val id = db.wardDao().insertCustomTimer(
            CustomTimer(bedLabel = "7654812", name = "Check drain", intervalMin = 60, startAt = old - 3_600_000, anchorAt = old - 3_600_000, fromAt = old - 3_600_000, status = CustomTimerStatus.STOPPED, stoppedAt = old),
        )
        db.wardDao().insertCustomSlots(listOf(CustomSlot(timerId = id, slotAt = old, status = CustomSlotStatus.ACKED, ackAt = old)))
        engine.rearm()
        assertTrue(timers().isEmpty())
        assertTrue(slotsOf(id).isEmpty())
        assertTrue(db.wardDao().customBedLabels().isEmpty())
    }

    // ---- Masking ----

    @Test
    fun masksCountCustomMrns_soTheSuffixStaysUnique() = runBlocking {
        val inTen = minute + 10 * MINUTE_MS
        checkpoint(500, CheckpointStatus.PENDING, inTen).also {
            // The transfusion's MRN shares its last four digits with the custom timer's.
            db.dao().updateTransfusion(db.dao().transfusion(it.transfusionId)!!.copy(bedLabel = "7654812"))
        }
        customSlot(501, CustomSlotStatus.PENDING, inTen, bed = "7664812", name = "Drain 7654812")
        engine.onPreAlert(500)
        engine.onCustomPreAlert(501)
        val nm = context.getSystemService(NotificationManager::class.java)
        val titles = nm.activeNotifications.associate { it.tag to it.notification.extras.getCharSequence("android.title")?.toString().orEmpty() }
        assertTrue(titles["prealert"]!!, titles["prealert"]!!.contains("•••54812"))
        assertTrue(titles["prealert_custom"]!!, titles["prealert_custom"]!!.contains("•••64812"))
        titles.values.forEach { assertFalse(it, it.contains("7654812") || it.contains("7664812")) }
    }

    // ---- The same id as a checkpoint, a turn slot and a custom alarm ----

    @Test
    fun collision_doneOnEachOne_leavesTheOtherTwoExactly() = runBlocking {
        checkpoint(500, CheckpointStatus.RINGING, minute)
        turnSlot(500, TurnSlotStatus.RINGING, minute)
        customSlot(500, CustomSlotStatus.RINGING, minute)
        engine.rearm()
        val c0 = cp(500); val t0 = ts(500); val k0 = cs(500)
        assertTrue(armed("tvs://alarm/CUSTOM/MISS/500"))

        engine.acknowledgeCustom(listOf(500))
        assertEquals(CustomSlotStatus.ACKED, cs(500).status)
        assertEquals(c0, cp(500)); assertEquals(t0, ts(500))
        assertFalse(armed("tvs://alarm/CUSTOM/MISS/500"))
        assertTrue(armed("tvs://alarm/TIMEOUT/500"))
        assertTrue(armed("tvs://alarm/TURN/MISS/500"))

        val k1 = cs(500)
        engine.acknowledge(listOf(500))
        assertEquals(CheckpointStatus.ACKED, cp(500).status)
        assertEquals(t0, ts(500)); assertEquals(k1, cs(500))
        assertTrue(armed("tvs://alarm/TURN/MISS/500"))

        val c1 = cp(500)
        engine.turned(500, Position.LEFT_30)
        assertEquals(TurnSlotStatus.ACKED, ts(500).status)
        assertEquals(c1, cp(500)); assertEquals(k1, cs(500))
        assertEquals(k0.slotAt, k1.slotAt)
    }

    @Test
    fun collision_undoOnEachOne_restoresOnlyItsOwnRow() = runBlocking {
        checkpoint(500, CheckpointStatus.RINGING, minute)
        turnSlot(500, TurnSlotStatus.RINGING, minute)
        customSlot(500, CustomSlotStatus.RINGING, minute)
        engine.rearm()
        val k0 = cs(500)
        engine.acknowledge(listOf(500))
        engine.acknowledgeCustom(listOf(500))
        val c1 = cp(500); val t1 = ts(500)
        engine.undo(db.wardDao().undoAll().first().id) // the custom Done, the latest
        assertEquals(k0, cs(500))
        assertEquals(c1, cp(500)); assertEquals(t1, ts(500))
        assertEquals(CheckpointStatus.ACKED, cp(500).status)
    }

    @Test
    fun collision_snoozeWritesOnlyWhatIsSounding() = runBlocking {
        val later = minute + 30 * MINUTE_MS
        checkpoint(500, CheckpointStatus.PENDING, later)
        turnSlot(500, TurnSlotStatus.PENDING, grid)
        customSlot(500, CustomSlotStatus.RINGING, minute)
        engine.rearm()
        val c0 = cp(500); val t0 = ts(500)
        engine.snooze()
        assertEquals(1, cs(500).snoozeCount)
        assertTrue(armed("tvs://alarm/CUSTOM/SNOOZE/500"))
        assertEquals(c0, cp(500)); assertEquals(t0, ts(500))
        assertFalse(armed("tvs://alarm/SNOOZE/500"))
        assertFalse(armed("tvs://alarm/TURN/SNOOZE/500"))
        assertTrue(armed("tvs://alarm/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/TURN/CHECKPOINT/500"))
    }

    @Test
    fun collision_snoozingACheckpoint_leavesThePendingCustomAlarm() = runBlocking {
        val later = minute + 30 * MINUTE_MS
        checkpoint(500, CheckpointStatus.RINGING, minute)
        customSlot(500, CustomSlotStatus.PENDING, later)
        engine.rearm()
        val k0 = cs(500)
        engine.snooze()
        assertEquals(1, cp(500).snoozeCount)
        assertEquals(k0, cs(500))
        assertFalse(armed("tvs://alarm/CUSTOM/SNOOZE/500"))
        assertTrue(armed("tvs://alarm/CUSTOM/CHECKPOINT/500"))
    }

    @Test
    fun collision_preAlertsGoToTheirOwnType() = runBlocking {
        val inTen = minute + 10 * MINUTE_MS
        checkpoint(500, CheckpointStatus.PENDING, inTen)
        customSlot(500, CustomSlotStatus.PENDING, inTen, name = "Check drain")
        val nm = context.getSystemService(NotificationManager::class.java)

        val routed = AlarmScheduler.parse(Uri.parse("tvs://alarm/CUSTOM/PREALERT/500"))!!
        assertTrue(routed.custom)
        assertFalse(AlarmScheduler.parse(Uri.parse("tvs://alarm/PREALERT/500"))!!.custom)

        engine.onCustomPreAlert(500)
        val custom = nm.activeNotifications.single()
        assertEquals("prealert_custom", custom.tag)
        assertEquals(500, custom.id)
        assertTrue(custom.notification.extras.getCharSequence("android.title")!!.toString().contains("Check drain"))

        engine.onPreAlert(500)
        val transfusion = nm.activeNotifications.single { it.tag == "prealert" }
        assertTrue(transfusion.notification.extras.getCharSequence("android.title")!!.toString().contains("15-min check"))
        assertEquals(2, nm.activeNotifications.size)
    }

    @Test
    fun collision_cancelTouchesOnlyItsOwnAddress() = runBlocking {
        val later = minute + 30 * MINUTE_MS
        checkpoint(500, CheckpointStatus.PENDING, later)
        turnSlot(500, TurnSlotStatus.PENDING, grid)
        customSlot(500, CustomSlotStatus.PENDING, later)
        engine.rearm()
        val scheduler = AlarmScheduler(context)
        assertTrue(armed("tvs://alarm/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/TURN/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/CUSTOM/CHECKPOINT/500"))

        scheduler.cancelCustom(500)
        assertFalse(armed("tvs://alarm/CUSTOM/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/TURN/CHECKPOINT/500"))

        scheduler.cancel(500)
        assertFalse(armed("tvs://alarm/CHECKPOINT/500"))
        assertTrue(armed("tvs://alarm/TURN/CHECKPOINT/500"))
    }

    @Test
    fun collision_stopAndEditOfTheCustomTimer_leaveTheOtherTwo() = runBlocking {
        val later = minute + 30 * MINUTE_MS
        checkpoint(500, CheckpointStatus.RINGING, minute)
        turnSlot(500, TurnSlotStatus.RINGING, minute)
        val (timer, _) = customSlot(500, CustomSlotStatus.PENDING, later)
        engine.rearm()
        val c0 = cp(500); val t0 = ts(500)

        engine.editCustom(timer.id, CustomDraft(timer.bedLabel, timer.name, 15))
        assertTrue(db.wardDao().customSlots(listOf(500)).isEmpty())
        assertFalse(armed("tvs://alarm/CUSTOM/CHECKPOINT/500"))
        assertEquals(c0, cp(500)); assertEquals(t0, ts(500))
        assertTrue(armed("tvs://alarm/TIMEOUT/500"))
        assertTrue(armed("tvs://alarm/TURN/MISS/500"))

        engine.stopCustom(timer.id)
        assertEquals(CustomTimerStatus.STOPPED, db.wardDao().customTimer(timer.id)!!.status)
        assertEquals(c0, cp(500)); assertEquals(t0, ts(500))
        assertTrue(armed("tvs://alarm/TIMEOUT/500"))
        assertTrue(armed("tvs://alarm/TURN/MISS/500"))
        assertNotNull(db.dao().checkpoints(listOf(500L)).singleOrNull())
    }
}
