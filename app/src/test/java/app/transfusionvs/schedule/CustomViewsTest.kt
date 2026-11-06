package app.transfusionvs.schedule

import app.transfusionvs.alarm.CustomRingBuilder
import app.transfusionvs.alarm.RingSpec
import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.CustomTimerStatus
import app.transfusionvs.data.CustomTimerWithSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** A custom timer on Home's card, in the list's order, and on the alarm screen. */
class CustomViewsTest {

    private val cairo = ZoneId.of("Africa/Cairo")
    private fun at(hm: String): Long = LocalDateTime.parse("2026-01-15T$hm").atZone(cairo).toInstant().toEpochMilli()

    private fun item(vararg slots: CustomSlot, id: Long = 1, bed: String = "7654812", name: String = "Check drain", start: String = "08:00", stopped: Boolean = false) =
        CustomTimerWithSlots(
            CustomTimer(
                id = id, bedLabel = bed, name = name, intervalMin = 60, startAt = at(start), anchorAt = at(start), fromAt = at(start),
                status = if (stopped) CustomTimerStatus.STOPPED else CustomTimerStatus.RUNNING,
            ),
            slots.map { it.copy(timerId = id) },
        )

    private fun slot(id: Long, hm: String, status: CustomSlotStatus = CustomSlotStatus.PENDING, fired: String? = null) =
        CustomSlot(id = id, timerId = 1, slotAt = at(hm), firedAt = fired?.let { at(it) }, status = status)

    @Test
    fun upcoming_soon_due_overdue_andStopped_inTheSameWordsAsTheOtherCards() {
        val calm = BedViews.of(item(slot(1, "09:00")), at("08:20"), 10)
        assertEquals(BedType.CUSTOM, calm.type)
        assertEquals("7654812", calm.bed)
        assertEquals("Check drain", calm.what)
        assertEquals(Urgency.CALM, calm.urgency)
        assertEquals(at("09:00"), calm.time)
        assertTrue(calm.running)

        val soon = BedViews.of(item(slot(1, "09:00")), at("08:57"), 10)
        assertEquals(Urgency.SOON, soon.urgency)
        assertEquals("SOON · 3 min", soon.stateText)

        val due = BedViews.of(item(slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00"), slot(2, "10:00")), at("09:02"), 10)
        assertEquals(Urgency.NOW, due.urgency)
        assertEquals("DUE +2 min", due.stateText)
        assertEquals(listOf(1L), due.ackIds)
        assertEquals(Labels.DONE, due.ackLabel)

        val snoozed = BedViews.of(item(slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00").copy(snoozeUntil = at("09:04"))), at("09:02"), 10)
        assertEquals("SNOOZED · ${Labels.clock(at("09:04"))}", snoozed.stateText)

        val overdue = BedViews.of(item(slot(1, "09:00", CustomSlotStatus.RINGING, fired = "09:00")), at("09:12"), 10)
        assertEquals(Urgency.OVERDUE, overdue.urgency)
        assertEquals("OVERDUE +12 min", overdue.stateText)

        val stopped = BedViews.of(item(slot(1, "09:00", CustomSlotStatus.CANCELLED), stopped = true), at("09:12"), 10)
        assertTrue(stopped.idle)
        assertFalse(stopped.running)
    }

    @Test
    fun atAnEqualRankAndTime_customComesAfterTheOthers_whoseOrderDoesNotChange() {
        val now = at("08:20")
        val custom = BedViews.of(item(slot(1, "09:00"), bed = "7654801"), now, 10)
        val a = custom.copy(type = BedType.TRANSFUSION, id = 11, bed = "7654899", what = "1-h check", ackLabel = Labels.ACK)
        val b = custom.copy(type = BedType.TURN, id = 12, bed = "7654850", what = "Turn")
        val without = BedViews.sorted(listOf(a, b))
        val with = BedViews.sorted(listOf(custom, a, b))
        assertEquals(without.map { it.id }, with.filter { it.type != BedType.CUSTOM }.map { it.id })
        assertEquals(BedType.CUSTOM, with.last().type)
        // A more urgent custom timer still comes first.
        val urgent = BedViews.of(item(slot(1, "08:00", CustomSlotStatus.RINGING, fired = "08:00")), now, 10)
        assertEquals(BedType.CUSTOM, BedViews.sorted(listOf(a, b, urgent)).first().type)
    }

    @Test
    fun severalCustomAlarmsInTheSameMinute_earliestStartFirst() {
        val now = at("10:00")
        val later = item(slot(5, "10:00", CustomSlotStatus.RINGING, fired = "10:00"), id = 2, start = "09:00", name = "Later one")
        val earlier = item(slot(6, "10:00", CustomSlotStatus.RINGING, fired = "10:00"), id = 3, start = "08:00", name = "Earlier one")
        val rings = CustomRingBuilder.build(listOf(later, earlier), now, snoozeMin = 2)
        assertEquals(listOf("Earlier one", "Later one"), rings.map { it.name })
        assertTrue(rings.all { it.audible })
    }

    @Test
    fun alarmsOfDifferentMinutes_theMostOverdueFirst() {
        val now = at("11:05")
        val due = item(slot(5, "11:00", CustomSlotStatus.RINGING, fired = "11:00"), id = 2, start = "07:00", name = "Due one")
        val overdue = item(slot(6, "10:50", CustomSlotStatus.RINGING, fired = "10:50").copy(silenced = true), id = 3, start = "10:20", name = "Overdue one")
        assertEquals(listOf("Overdue one", "Due one"), CustomRingBuilder.build(listOf(due, overdue), now, 2).map { it.name })
    }

    @Test
    fun customOnly_titlesTheScreen_andMaskingCoversTheName() {
        val now = at("10:00")
        val rings = CustomRingBuilder.build(listOf(item(slot(5, "10:00", CustomSlotStatus.RINGING, fired = "10:00"), name = "Drain 7654812")), now, 2)
        val spec = RingSpec.build(emptyList(), canSnooze = true, snoozeMin = 2, soundOn = true, soundUri = null, missedTimeoutMin = 10, customs = rings)
        assertTrue(spec.customOnly)
        assertEquals("Drain 7654812 — 7654812", spec.title)
        val masked = spec.masked(BedMask.masker(listOf("7654812")))
        assertFalse(masked.customs.single().bedLabel.contains("7654812"))
        assertFalse(masked.customs.single().name.contains("7654812"))
    }

    @Test
    fun withNoCustomAlarm_theRingIsAsBefore() {
        val spec = RingSpec.build(emptyList(), canSnooze = true, snoozeMin = 2, soundOn = true, soundUri = null, missedTimeoutMin = 10)
        assertFalse(spec.customOnly)
        assertTrue(spec.customs.isEmpty())
    }
}
