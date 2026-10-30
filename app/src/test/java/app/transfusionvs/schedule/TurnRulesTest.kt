package app.transfusionvs.schedule

import app.transfusionvs.data.Position
import app.transfusionvs.data.TurnPlan
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnSlot
import app.transfusionvs.data.TurnSlotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TurnRulesTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(hm: String): Long = LocalDateTime.parse("2026-01-15T$hm").atZone(cairo).toInstant().toEpochMilli()

    private fun plan(stop: String? = null, id: Long = 1) = TurnPlan(
        id = id, bedLabel = "Bed 3", colorIndex = 0, intervalMin = 120, startAt = at("07:30"), firstSlotAt = at("08:00"),
        stopAt = stop?.let { at(it) }, rotation = Position.toCsv(Position.DEFAULT_ROTATION), currentPosition = Position.BACK,
    )

    private fun slot(id: Long, hm: String, status: TurnSlotStatus = TurnSlotStatus.PENDING, fired: String? = null, planId: Long = 1) =
        TurnSlot(id = id, planId = planId, slotAt = at(hm), firedAt = fired?.let { at(it) }, positionFrom = Position.LEFT_30, status = status)

    @Test
    fun aLateAckDoesNotShift_theNextAlarmIsStillTheNextGridSlot() {
        val slots = listOf(
            slot(1, "10:00", TurnSlotStatus.ACKED, fired = "10:00").copy(ackAt = at("10:25")),
            slot(2, "12:00"),
        )
        val plan = TurnRules.arm(plan(), slots, now = at("10:30"), missedTimeoutMin = 10, zone = cairo)
        assertEquals(listOf(at("12:00")), plan.requests.filter { it.kind == AlarmKind.CHECKPOINT }.map { it.at })
        assertTrue(plan.requests.all { it.turn })
    }

    @Test
    fun theMissedTimerStopsTheSound_butTheSlotStaysOpen() {
        val ringing = slot(1, "10:00", TurnSlotStatus.RINGING, fired = "10:00")
        val changed = TurnRules.step(plan(), listOf(ringing), now = at("10:10"), missedTimeoutMin = 10, zone = cairo).single()
        assertEquals(TurnSlotStatus.RINGING, changed.status)
        assertTrue(changed.silenced)
        assertFalse(TurnRules.isAudible(changed, at("10:10")))
        assertTrue(TurnRules.isOverdue(changed, at("10:10"), 10))
    }

    @Test
    fun anUnacknowledgedSlotBecomesMissedWhenTheNextSlotFires_andTheNewSlotRings() {
        val open = slot(1, "10:00", TurnSlotStatus.RINGING, fired = "10:00").copy(silenced = true)
        val next = slot(2, "12:00")
        val changed = TurnRules.step(plan(), listOf(open, next), now = at("12:00"), missedTimeoutMin = 10, zone = cairo)
            .associateBy { it.id }
        assertEquals(TurnSlotStatus.MISSED, changed.getValue(1).status)
        assertEquals(TurnSlotStatus.RINGING, changed.getValue(2).status)
        assertEquals(Position.BACK, changed.getValue(2).positionFrom) // where the patient lies now
    }

    @Test
    fun remindIn30RingsAgainWithAFreshTimer() {
        val reminded = slot(1, "10:00", TurnSlotStatus.RINGING, fired = "10:00").copy(silenced = true, remindAt = at("10:40"))
        val changed = TurnRules.step(plan(), listOf(reminded), now = at("10:40"), missedTimeoutMin = 10, zone = cairo).single()
        assertFalse(changed.silenced)
        assertEquals(at("10:40"), changed.firedAt)
        assertTrue(TurnRules.isAudible(changed, at("10:40")))
    }

    @Test
    fun slotsInTheSameMinuteAcrossBedsRingTogether() {
        val a = slot(1, "10:00", planId = 1)
        val b = slot(2, "10:00", planId = 2)
        val now = at("10:00") + 300
        val ringA = TurnRules.step(plan(id = 1), listOf(a), now, 10, cairo).single()
        val ringB = TurnRules.step(plan(id = 2), listOf(b), now, 10, cairo).single()
        assertEquals(TurnSlotStatus.RINGING, ringA.status)
        assertEquals(TurnSlotStatus.RINGING, ringB.status)
        assertEquals(ringA.firedAt, ringB.firedAt)
    }

    @Test
    fun afterTheAppWasOff_closedSlotsAreMissed_andOnlyTheCurrentOneRings() {
        val created = TurnRules.materialize(plan(), emptyList(), now = at("11:05"), zone = cairo)
        val byTime = created.associate { Labels.clock(it.slotAt, cairo) to it.status }
        assertEquals(TurnSlotStatus.MISSED, byTime["08:00"]) // its next slot (10:00) already fired
        assertEquals(TurnSlotStatus.PENDING, byTime["10:00"]) // still open until 12:00 → rings now
        assertEquals(TurnSlotStatus.PENDING, byTime["12:00"])
        assertTrue(TurnRules.arm(plan(), created.mapIndexed { i, s -> s.copy(id = i + 1L) }, at("11:05"), 10, cairo).ringNow)
    }

    @Test
    fun aPlanRunningOverADayDoesNotRecreateCleanedUpSlotsAsMissed() {
        val twoDaysLater = at("08:00") + 2 * 24 * 60 * MINUTE_MS + 65 * MINUTE_MS // day 3, 09:05
        val created = TurnRules.materialize(plan(), emptyList(), twoDaysLater, cairo)
        assertTrue(created.all { it.slotAt >= twoDaysLater - TurnRules.KEEP_MS })
        assertEquals(11, created.count { it.status == TurnSlotStatus.MISSED }) // only the last 24 h (10:00 … 06:00)
    }

    @Test
    fun aTimezoneChangeMovesUpcomingSlotsOntoTheNewLocalGrid() {
        val kolkata = ZoneId.of("Asia/Kolkata") // +5:30: Cairo's even hours are off this grid
        val now = at("09:00")
        val cairoSlots = TurnRules.materialize(plan(), emptyList(), now, cairo).mapIndexed { i, s -> s.copy(id = i + 1L) }
        assertTrue(TurnRules.offGridPending(plan(), cairoSlots, now, cairo).isEmpty())

        val stale = TurnRules.offGridPending(plan(), cairoSlots, now, kolkata)
        assertEquals(cairoSlots.count { it.status == TurnSlotStatus.PENDING && it.slotAt > now }, stale.size)
        val fresh = TurnRules.materialize(plan(), cairoSlots - stale.toSet(), now, kolkata)
        assertTrue(fresh.isNotEmpty())
        fresh.filter { it.slotAt > now }.forEach { assertTrue(Labels.clock(it.slotAt, kolkata).endsWith(":00")) }
    }

    @Test
    fun theOverdueTimerStaysArmedAfterStartRound() {
        val started = slot(1, "10:00", TurnSlotStatus.RINGING, fired = "10:00").copy(silenced = true)
        val plan = TurnRules.arm(plan(), listOf(started), now = at("10:03"), missedTimeoutMin = 10, zone = cairo)
        assertTrue(plan.requests.any { it.kind == AlarmKind.TIMEOUT && it.at == at("10:10") })
        assertTrue(plan.requests.any { it.kind == AlarmKind.MISS && it.at == at("12:00") })
        assertFalse(plan.ringNow) // silenced: no sound
    }

    @Test
    fun noSlotAtOrAfterTheStopTime_andThePlanStopsItself() {
        val created = TurnRules.materialize(plan(stop = "14:00"), emptyList(), now = at("09:00"), zone = cairo)
        assertEquals(listOf("08:00", "10:00", "12:00"), created.filter { it.status == TurnSlotStatus.PENDING }.map { Labels.clock(it.slotAt, cairo) })
        val done = created.map { it.copy(status = TurnSlotStatus.ACKED) }
        assertTrue(TurnRules.shouldAutoStop(plan(stop = "14:00"), done, at("14:00")))
        assertFalse(TurnRules.shouldAutoStop(plan(stop = "14:00"), done, at("13:59")))
        assertFalse(TurnRules.materialize(plan(stop = "14:00").copy(status = TurnPlanStatus.STOPPED), emptyList(), at("09:00"), cairo).isNotEmpty())
    }
}
