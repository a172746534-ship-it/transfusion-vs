package app.transfusionvs.schedule

import app.transfusionvs.alarm.PocketGuard
import app.transfusionvs.alarm.UndoCodec
import app.transfusionvs.alarm.UndoStep
import app.transfusionvs.data.CheckpointStatus
import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import app.transfusionvs.data.SkipReason
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlotStatus
import app.transfusionvs.screenshots.Fakes
import app.transfusionvs.screenshots.Fakes.with
import app.transfusionvs.screenshots.TurnFakes
import android.view.MotionEvent
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrgencyTest {

    private val now = Fakes.now

    /** Times print in the phone's zone; the sample data is in Cairo. */
    @Before
    fun cairo() = TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))

    @Test
    fun missedCheckIsOverdueWithItsLateness() {
        val v = BedViews.of(Fakes.bed12, now)
        assertEquals(Urgency.OVERDUE, v.urgency)
        assertEquals("30-min check", v.what)
        assertEquals(Fakes.at("2026-01-15T10:50"), v.time)
        assertEquals("OVERDUE +15 min", v.stateText)
        assertEquals(1, v.ackIds.size)
    }

    @Test
    fun theTemperatureFollowsTheTimeLeft() {
        val bed = Fakes.bed14 // 15-min check at 11:07
        assertEquals(Urgency.CALM, BedViews.of(bed, Fakes.at("2026-01-15T10:50")).urgency)
        val soon = BedViews.of(bed, Fakes.at("2026-01-15T10:58"))
        assertEquals(Urgency.SOON, soon.urgency)
        assertEquals("SOON · 9 min", soon.stateText)
        val ringing = bed.copy(checkpoints = bed.checkpoints.map { if (it.type == CheckpointType.M15) it.copy(status = CheckpointStatus.RINGING, firedAt = now) else it })
        val due = BedViews.of(ringing, Fakes.at("2026-01-15T11:07"))
        assertEquals(Urgency.NOW, due.urgency)
        assertEquals("DUE NOW", due.stateText)
        assertTrue(due.ackIds.isEmpty()) // the alarm screen is the place for that
    }

    @Test
    fun calmSaysWhenInWords() {
        val bed = Fakes.transfusion(9, "Bed 9", 0, "2026-01-15T10:30", 120).with(CheckpointType.M15, 15, 1).with(CheckpointType.M30, 30, 1)
        val v = BedViews.of(bed, Fakes.at("2026-01-15T11:05"))
        assertEquals(Urgency.CALM, v.urgency)
        assertEquals("in 25 min", v.stateText)
        assertEquals("1-h check", v.what)
    }

    @Test
    fun stoppedForReactionIsAboveEverything() {
        val stopped = Fakes.bed14.copy(transfusion = Fakes.bed14.transfusion.copy(status = TransfusionStatus.STOPPED_REACTION, endAt = now))
        val v = BedViews.of(stopped, now)
        assertTrue(v.stoppedForReaction)
        assertEquals("STOPPED — REACTION", v.stateText)
        val sorted = BedViews.sorted(listOf(BedViews.of(Fakes.bed12, now), v))
        assertTrue(sorted.first().stoppedForReaction)
    }

    @Test
    fun mostUrgentFirstThenSoonest() {
        val beds = BedViews.sorted(Fakes.sixBeds().map { BedViews.of(it, now) })
        val order = beds.map { it.urgency.ordinal }
        assertEquals(order.sortedDescending(), order)
        val calm = beds.filter { it.urgency == Urgency.CALM }.mapNotNull { it.time }
        assertEquals(calm.sorted(), calm)
    }

    @Test
    fun turnPlanOverdueOpenStopped() {
        val onTrack = BedViews.of(TurnFakes.bed3, now, 10)
        assertEquals(Urgency.CALM, onTrack.urgency)
        assertEquals(Fakes.at("2026-01-15T12:00"), onTrack.time)
        assertEquals("Back → Right 30°", onTrack.what)

        val overdue = BedViews.of(TurnFakes.bed7, now, 10)
        assertEquals(Urgency.OVERDUE, overdue.urgency)
        assertTrue(overdue.roundOpen)

        val stopped = BedViews.of(TurnFakes.bed3.copy(plan = TurnFakes.bed3.plan.copy(status = TurnPlanStatus.STOPPED)), now, 10)
        assertTrue(stopped.idle)
        assertEquals("Stopped", stopped.stateText)
    }

    @Test
    fun theSummaryTellsTheTruthOfTheWard() {
        assertEquals("Nothing running", BedViews.summary(emptyList()))
        assertEquals("NO BEDS RUNNING", BedViews.running(emptyList()))
        val mixed = listOf(BedViews.of(Fakes.bed12, now), BedViews.of(Fakes.bed14, now))
        assertEquals("1 overdue", BedViews.summary(mixed))
        assertEquals("2 BEDS RUNNING", BedViews.running(mixed))
        val calm = listOf(BedViews.of(TurnFakes.bed3, now, 10))
        assertEquals("All on time · next 12:00", BedViews.summary(calm))
    }

    @Test
    fun railFillsTowardTheNextDot() {
        val times = listOf(0L, 100L, 200L)
        assertEquals(RailState(3, 1, 0.5f), BedViews.rail(times, null, 50))
        assertEquals(RailState(3, 0, 0f), BedViews.rail(times, null, -1))
        assertEquals(RailState(3, 1, 1f), BedViews.rail(times, 1, 500))
        assertEquals(RailState(0, 0, 0f), BedViews.rail(emptyList(), null, 0))
    }

    @Test
    fun alarmRailPointsAtThisCheck() {
        val rail = BedViews.alarmRail(30, 120)
        assertEquals(2, rail.done) // start, 15, then 30
        assertTrue(rail.total >= 5)
        // Past the planned end the check still has a place on the rail.
        val late = BedViews.alarmRail(180, 120)
        assertEquals(late.total - 1, late.done)
    }

    @Test
    fun nightFollowsTheClockAcrossMidnight() {
        fun clock(h: Int) = NightRule.isNight(NightRule.AUTO_CLOCK, h, 20, 8, false)
        assertTrue(clock(20))
        assertTrue(clock(3))
        assertFalse(clock(8))
        assertFalse(clock(19))
        assertTrue(NightRule.isNight(NightRule.FOLLOW_SYSTEM, 12, 20, 8, true))
        assertFalse(NightRule.isNight(NightRule.FOLLOW_SYSTEM, 3, 20, 8, false))
        assertFalse(NightRule.isNight(NightRule.ALWAYS_DAY, 3, 20, 8, true))
        assertTrue(NightRule.isNight(NightRule.ALWAYS_NIGHT, 12, 20, 8, false))
        assertTrue(NightRule.isNight(NightRule.AUTO_CLOCK, 2, 1, 6, false))
        assertFalse(NightRule.isNight(NightRule.AUTO_CLOCK, 7, 1, 6, false))
    }

    @Test
    fun bedsTypedOnce() {
        assertEquals(listOf("Bed 1", "Bed 2", "Bed 3", "Bed 14", "ICU 3"), BedInput.parse("1-3, 14, ICU 3"))
        assertEquals(listOf("Bed 7"), BedInput.parse(" 7 , 7 "))
        assertTrue(BedInput.parse("").isEmpty())
        assertTrue(BedInput.parse("9-1").isEmpty())
        assertTrue(BedInput.parse("1-5000").isEmpty())
        assertEquals(16, BedInput.parse("A very long bed label indeed").first().length)
        assertEquals(listOf("Bed 2", "Bed 10", "ICU 1"), BedInput.sorted(listOf("Bed 10", "ICU 1", "Bed 2")))
    }

    @Test
    fun undoStepsSurviveTheirTextForm() {
        val steps = listOf(
            UndoStep.RestoreCheckpoint(1, CheckpointStatus.RINGING, 5, null, 2, 9),
            UndoStep.RestoreSlot(2, TurnSlotStatus.SKIPPED, null, 7, Position.BACK, Position.LEFT_30, SkipReason.OFF_UNIT, 1, null, true, 8),
            UndoStep.RestoreSlot(3, TurnSlotStatus.RINGING, 1, null, Position.PRONE, null, null, 0, null, false, null),
            UndoStep.RestoreTransfusion(4, TransfusionStatus.RUNNING, null),
            UndoStep.RestorePlan(5, TurnPlanStatus.RUNNING, 6, Position.HOB_UP),
            UndoStep.DeleteCheckpoint(6),
            UndoStep.DeleteTransfusion(7),
            UndoStep.DeletePlan(8),
        )
        assertEquals(steps, UndoCodec.decode(UndoCodec.encode(steps)))
        assertTrue(UndoCodec.decode("").isEmpty())
    }

    @Test
    fun aTimezoneChangeMovesAnUnfiredFirstSlotOntoTheNewGrid() {
        val kathmandu = ZoneId.of("Asia/Kathmandu")
        // 06:00 Cairo is 09:45 in Kathmandu: off its 2-hourly grid.
        val first = Fakes.at("2026-01-15T06:00")
        val slots = TurnGrid.slots(first, 120, null, first + 8 * 3_600_000, kathmandu)
        assertTrue(slots.all { TurnGrid.isOnGrid(it, 120, kathmandu) })
        assertTrue(slots.first() > first)
        // On the grid, nothing moves.
        val cairo = ZoneId.of("Africa/Cairo")
        assertEquals(first, TurnGrid.slots(first, 120, null, first + 3_600_000, cairo).first())
    }

    @Test
    fun pocketGuardIgnoresTheFirstSecondAndACoveredPhone() {
        val g = PocketGuard()
        g.onShown(1_000)
        assertFalse(g.allows(MotionEvent.ACTION_DOWN, 1_500))
        assertFalse(g.allows(MotionEvent.ACTION_MOVE, 1_600))
        // A gesture that began inside the second stays ignored, even when it ends after it.
        assertFalse(g.allows(MotionEvent.ACTION_UP, 2_500))
        assertTrue(g.allows(MotionEvent.ACTION_DOWN, 2_500))
        assertTrue(g.allows(MotionEvent.ACTION_UP, 2_600))

        g.covered = true
        assertFalse(g.allows(MotionEvent.ACTION_DOWN, 9_000))
        assertFalse(g.allows(MotionEvent.ACTION_UP, 9_100))
        g.covered = false
        assertTrue(g.allows(MotionEvent.ACTION_DOWN, 9_200))
    }
}
