package app.transfusionvs.schedule

import app.transfusionvs.data.Position.BACK
import app.transfusionvs.data.Position.LEFT_30
import app.transfusionvs.data.Position.RIGHT_30
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TurnGridTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(local: String, zone: ZoneId = cairo): Long = LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()
    private val stamp = DateTimeFormatter.ofPattern("dd HH:mm")
    private fun show(ms: Long, zone: ZoneId = cairo) = Instant.ofEpochMilli(ms).atZone(zone).format(stamp)
    private fun next(after: String, interval: Int = 120) = show(TurnGrid.nextSlotAfter(at(after), interval, cairo))

    // ---- The fixed ward-clock grid ----

    @Test
    fun evenHourGridAcrossMidnight() {
        assertEquals("15 10:00", next("2026-01-15T09:10"))
        assertEquals("15 22:00", next("2026-01-15T21:30"))
        assertEquals("16 00:00", next("2026-01-15T22:00")) // strictly after
        assertEquals("16 00:00", next("2026-01-15T23:59"))
        assertEquals("16 02:00", next("2026-01-16T00:00"))
    }

    @Test
    fun oneAndFourHourGridsKeep08And20() {
        assertEquals("15 10:00", next("2026-01-15T09:10", 60))
        assertEquals("15 12:00", next("2026-01-15T09:10", 240))
        val day = TurnGrid.slots(at("2026-01-15T00:00"), 240, null, at("2026-01-15T23:59"), cairo).map { show(it) }
        assertEquals(listOf("15 00:00", "15 04:00", "15 08:00", "15 12:00", "15 16:00", "15 20:00"), day)
    }

    // ---- Starting mid-slot ----

    @Test
    fun startingMidSlot_firstTurnIsTheNextSlot_unlessUnder30MinAway() {
        assertEquals("15 10:00", show(TurnGrid.firstSlot(at("2026-01-15T09:10"), 120, cairo)))
        assertEquals("15 10:00", show(TurnGrid.firstSlot(at("2026-01-15T09:30"), 120, cairo))) // exactly 30 min: kept
        assertEquals("15 12:00", show(TurnGrid.firstSlot(at("2026-01-15T09:45"), 120, cairo))) // 15 min away: next one
        // The preview offers both so the nurse can change it.
        assertEquals(listOf("15 10:00", "15 12:00"), TurnGrid.firstSlotChoices(at("2026-01-15T09:45"), 120, cairo).map { show(it) })
    }

    // ---- Late turns, missed slots, reminders ----

    @Test
    fun aLateAckDoesNotShiftTheSchedule() {
        val slots = TurnGrid.slots(at("2026-01-15T10:00"), 120, null, at("2026-01-15T14:00"), cairo).map { show(it) }
        assertEquals(listOf("15 10:00", "15 12:00", "15 14:00"), slots)
        // Turned at 10:25 → the next turn is still 12:00.
        assertEquals("15 12:00", next("2026-01-15T10:25"))
    }

    @Test
    fun aMissedSlotClosesWhenTheNextSlotFires() {
        assertEquals("15 12:00", show(TurnGrid.missAt(at("2026-01-15T10:00"), 120, cairo)))
        assertEquals("15 11:00", show(TurnGrid.missAt(at("2026-01-15T10:00"), 60, cairo)))
    }

    @Test
    fun remindIn30OnlyIfItLandsBeforeTheNextSlot() {
        val nextSlot = at("2026-01-15T12:00")
        assertTrue(TurnGrid.canRemind(at("2026-01-15T10:20"), nextSlot))
        assertFalse(TurnGrid.canRemind(at("2026-01-15T11:40"), nextSlot))
    }

    // ---- Stop time ----

    @Test
    fun noSlotAtOrAfterTheStopTime() {
        val slots = TurnGrid.slots(at("2026-01-15T10:00"), 120, at("2026-01-15T20:00"), at("2026-01-16T10:00"), cairo).map { show(it) }
        assertEquals("15 18:00", slots.last())
        assertFalse(slots.contains("15 20:00"))
    }

    @Test
    fun endOfMyShiftIsTheNext08Or20() {
        assertEquals("15 20:00", show(TurnGrid.endOfShift(at("2026-01-15T11:05"), cairo)))
        assertEquals("16 08:00", show(TurnGrid.endOfShift(at("2026-01-15T21:00"), cairo)))
        assertEquals("15 20:00", show(TurnGrid.endOfShift(at("2026-01-15T08:00"), cairo)))
    }

    // ---- DST (Egypt) and timezone changes ----

    @Test
    fun dstSpringForward_theMissingSlotIsSkipped() {
        // 2026-04-24 00:00 → 01:00 in Egypt: there is no 00:00 slot that night.
        assertEquals("24 02:00", next("2026-04-23T22:30"))
        assertEquals("24 01:00", next("2026-04-23T23:30", 60))
    }

    @Test
    fun dstFallBack_theRepeatedHourRingsOnce() {
        // 2026-10-29 (last Thursday of October) 24:00 → 23:00: the 23:00 hour happens twice.
        val first = TurnGrid.nextSlotAfter(at("2026-10-29T22:30"), 60, cairo)
        val second = TurnGrid.nextSlotAfter(first, 60, cairo)
        assertEquals("29 23:00", show(first))
        assertEquals("30 00:00", show(second))
    }

    @Test
    fun timezoneChange_theGridFollowsThePhonesLocalClock() {
        val dubai = ZoneId.of("Asia/Dubai")
        val t = at("2026-01-15T09:10") // = 11:10 in Dubai
        assertEquals("15 12:00", show(TurnGrid.nextSlotAfter(t, 120, dubai), dubai))
        assertEquals("15 10:00", show(TurnGrid.nextSlotAfter(t, 120, cairo), cairo))
    }

    // ---- Positions ----

    @Test
    fun rotationCyclesAndNeverSuggestsAnExcludedPosition() {
        val all = listOf(LEFT_30, BACK, RIGHT_30)
        assertEquals(RIGHT_30, TurnGrid.nextPosition(BACK, all))
        assertEquals(LEFT_30, TurnGrid.nextPosition(RIGHT_30, all))
        val noLeft = listOf(BACK, RIGHT_30) // "not on left" (line on that side)
        assertEquals(BACK, TurnGrid.nextPosition(RIGHT_30, noLeft))
        assertEquals(RIGHT_30, TurnGrid.nextPosition(BACK, noLeft))
        assertEquals(BACK, TurnGrid.nextPosition(LEFT_30, noLeft)) // lying on an excluded side → first allowed
    }

    @Test
    fun longestGapIsComputedFromActualTurnTimes() {
        val gap = TurnGrid.longestGapMs(
            turnTimes = listOf(at("2026-01-15T08:06"), at("2026-01-15T10:25")),
            planStart = at("2026-01-15T07:30"),
            now = at("2026-01-15T11:05"),
        )
        assertEquals("2 h 19 min", TurnStatus.gap(gap))
    }
}
