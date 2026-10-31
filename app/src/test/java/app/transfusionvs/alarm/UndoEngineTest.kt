package app.transfusionvs.alarm

import android.content.Context
import androidx.room.Room
import app.transfusionvs.data.AppDatabase
import app.transfusionvs.data.Checkpoint
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.Settings
import app.transfusionvs.data.Transfusion
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.UndoAction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Every action that can be undone restores the exact previous rows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UndoEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var engine: Engine
    private val now get() = System.currentTimeMillis()

    @Before
    fun open() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        engine = Engine(context, db.dao(), db.turnDao(), db.wardDao(), Settings(context), AlarmScheduler(context))
    }

    @After
    fun close() = db.close()

    private suspend fun ringingCheck(snoozes: Int = 1): Checkpoint {
        val id = db.dao().insertTransfusion(
            Transfusion(bedLabel = "Bed 1", colorIndex = 0, startAt = now - 15 * 60_000, plannedDurationMin = 120, baselineConfirmed = true),
        )
        val rowId = db.dao().insertCheckpoints(
            listOf(Checkpoint(transfusionId = id, type = CheckpointType.M15, offsetMin = 15, scheduledAt = now - 1_000, firedAt = now - 1_000, snoozeCount = snoozes, status = CheckpointStatus.RINGING)),
        ).first()
        return db.dao().checkpoints(listOf(rowId)).first()
    }

    private suspend fun ringingSlot(bed: String): Pair<TurnPlan, TurnSlot> {
        val slotAt = now - 60_000
        val planId = db.turnDao().insertPlan(
            TurnPlan(
                bedLabel = bed, colorIndex = 0, intervalMin = 120, startAt = slotAt - 60_000, firstSlotAt = slotAt,
                rotation = Position.toCsv(Position.DEFAULT_ROTATION), currentPosition = Position.BACK,
            ),
        )
        db.turnDao().insertSlots(listOf(TurnSlot(planId = planId, slotAt = slotAt, firedAt = slotAt, positionFrom = Position.BACK, status = TurnSlotStatus.RINGING, silenced = true)))
        return db.turnDao().plan(planId)!! to db.turnDao().slotsFor(planId).first()
    }

    private suspend fun latest(): UndoAction = db.wardDao().undoAll().first()

    @Test
    fun acknowledgeThenUndoIsTheExactRow() = runBlocking {
        val before = ringingCheck()
        engine.acknowledge(listOf(before.id))
        val done = db.dao().checkpoints(listOf(before.id)).first()
        assertEquals(CheckpointStatus.ACKED, done.status)
        assertEquals("Bed 1 · 15-min check done", latest().label)
        assertEquals(UndoAction.transfusion(before.transfusionId), latest().ownerKey)

        engine.undo(latest().id)
        assertEquals(before, db.dao().checkpoints(listOf(before.id)).first())
        assertTrue(db.wardDao().undoAll().isEmpty())
    }

    @Test
    fun aRepeatedAcknowledgeChangesNothingMoreAndAddsNoEntry() = runBlocking {
        val before = ringingCheck()
        engine.acknowledge(listOf(before.id))
        val first = db.dao().checkpoints(listOf(before.id)).first()
        engine.acknowledge(listOf(before.id))
        assertEquals(first, db.dao().checkpoints(listOf(before.id)).first())
        assertEquals(1, db.wardDao().undoAll().size)
    }

    @Test
    fun endThenUndoRestoresTheBedAndItsSchedule() = runBlocking {
        val id = engine.startTransfusion("Bed 2", now, 120)
        val before = db.dao().transfusion(id)!!
        val pending = db.dao().checkpointsFor(id).filter { it.status == CheckpointStatus.PENDING }
        assertTrue(pending.isNotEmpty())

        engine.endTransfusion(id)
        assertEquals(TransfusionStatus.ENDED, db.dao().transfusion(id)!!.status)
        assertTrue(db.dao().checkpointsFor(id).any { it.type == CheckpointType.END })

        engine.undo(latest().id)
        assertEquals(before, db.dao().transfusion(id)!!)
        assertTrue(db.dao().checkpointsFor(id).none { it.type == CheckpointType.END })
        assertEquals(pending.map { it.id to it.status }, db.dao().checkpointsFor(id).filter { it.status == CheckpointStatus.PENDING }.map { it.id to it.status })
    }

    @Test
    fun stopForReactionThenUndoRestoresWhatWasRinging() = runBlocking {
        val before = ringingCheck()
        val id = before.transfusionId
        engine.stopForReaction(id)
        assertEquals(TransfusionStatus.STOPPED_REACTION, db.dao().transfusion(id)!!.status)
        assertEquals(CheckpointStatus.CANCELLED, db.dao().checkpoints(listOf(before.id)).first().status)

        engine.undo(latest().id)
        assertEquals(TransfusionStatus.RUNNING, db.dao().transfusion(id)!!.status)
        assertNull(db.dao().transfusion(id)!!.endAt)
        assertEquals(before, db.dao().checkpoints(listOf(before.id)).first())
    }

    @Test
    fun undoingAStartRemovesTheBed() = runBlocking {
        val id = engine.startTransfusion("Bed 3", now, 120)
        engine.undo(db.wardDao().undoAll().first { it.ownerKey == UndoAction.transfusion(id) }.id)
        assertNull(db.dao().transfusion(id))
        assertTrue(db.dao().checkpointsFor(id).isEmpty())
    }

    @Test
    fun turnedThenUndoRestoresSlotAndPosition() = runBlocking {
        val (plan, slot) = ringingSlot("Bed 4")
        engine.turned(slot.id, Position.RIGHT_30)
        assertEquals(Position.RIGHT_30, db.turnDao().plan(plan.id)!!.currentPosition)
        assertEquals(TurnSlotStatus.ACKED, db.turnDao().slots(listOf(slot.id)).first().status)

        engine.undo(latest().id)
        assertEquals(plan, db.turnDao().plan(plan.id))
        assertEquals(slot, db.turnDao().slots(listOf(slot.id)).first())
    }

    @Test
    fun cantTurnThenUndoRestoresTheSlot() = runBlocking {
        val (_, slot) = ringingSlot("Bed 4")
        engine.cantTurn(slot.id, SkipReason.PROCEDURE)
        assertEquals(SkipReason.PROCEDURE, db.turnDao().slots(listOf(slot.id)).first().skipReason)
        engine.undo(latest().id)
        assertEquals(slot, db.turnDao().slots(listOf(slot.id)).first())
    }

    @Test
    fun allTurnedMovesEveryBedAndUndoesAsOne() = runBlocking {
        val (p1, s1) = ringingSlot("Bed 5")
        val (p2, s2) = ringingSlot("Bed 6")
        engine.allTurned()
        assertEquals(Position.RIGHT_30, db.turnDao().plan(p1.id)!!.currentPosition)
        assertEquals(Position.RIGHT_30, db.turnDao().plan(p2.id)!!.currentPosition)
        assertTrue(db.turnDao().ringingSlots().isEmpty())
        val whole = latest()
        assertEquals(UndoAction.ROUND, whole.ownerKey)

        engine.undo(whole.id)
        assertEquals(p1, db.turnDao().plan(p1.id))
        assertEquals(p2, db.turnDao().plan(p2.id))
        assertEquals(s1, db.turnDao().slots(listOf(s1.id)).first())
        assertEquals(s2, db.turnDao().slots(listOf(s2.id)).first())
        assertTrue("the per-bed entries are spent too", db.wardDao().undoAll().isEmpty())
    }

    @Test
    fun oneBedOfARoundCanGoBackAlone() = runBlocking {
        val (p1, s1) = ringingSlot("Bed 5")
        val (p2, _) = ringingSlot("Bed 6")
        engine.allTurned()
        val entry = db.wardDao().undoAll().first { it.ownerKey == UndoAction.plan(p1.id) }
        engine.undo(entry.id)
        assertEquals(s1, db.turnDao().slots(listOf(s1.id)).first())
        assertEquals(Position.RIGHT_30, db.turnDao().plan(p2.id)!!.currentPosition)
        assertTrue("the whole-round entry can no longer apply", db.wardDao().undoAll().none { it.ownerKey == UndoAction.ROUND })
    }

    @Test
    fun anExpiredEntryChangesNothing() = runBlocking {
        val before = ringingCheck()
        engine.acknowledge(listOf(before.id))
        val done = db.dao().checkpoints(listOf(before.id)).first()
        val entry = latest()
        db.wardDao().deleteUndo(entry.id)
        val expired = db.wardDao().insertUndo(entry.copy(id = 0, expiresAt = now - 1))
        engine.undo(expired)
        assertEquals(done.status, db.dao().checkpoints(listOf(before.id)).first().status)
    }

    @Test
    fun stoppingAPlanThenUndoKeepsItRunning() = runBlocking {
        val (plan, _) = ringingSlot("Bed 7")
        engine.stopTurnPlan(plan.id)
        assertEquals(TurnPlanStatus.STOPPED, db.turnDao().plan(plan.id)!!.status)
        engine.undo(latest().id)
        assertEquals(TurnPlanStatus.RUNNING, db.turnDao().plan(plan.id)!!.status)
        assertNull(db.turnDao().plan(plan.id)!!.stopAt)
    }
}
