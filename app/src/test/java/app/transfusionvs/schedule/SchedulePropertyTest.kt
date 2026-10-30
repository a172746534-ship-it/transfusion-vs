package app.transfusionvs.schedule

import app.transfusionvs.data.CheckpointType
import app.transfusionvs.data.Position
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property-based checks: at least 10,000 random start times, grids, intervals and DST dates per
 * rule, from a fixed seed so a failure always reproduces. Dates are biased towards Egypt's DST
 * nights (last Friday of April, last Thursday of October).
 */
class SchedulePropertyTest {

    private val cases = 12_000
    private val zones = listOf("Africa/Cairo", "Asia/Kathmandu", "America/New_York", "Asia/Riyadh", "UTC").map(ZoneId::of)
    private val rnd = Random(20261031)

    private fun lastWeekday(year: Int, month: Int, dow: java.time.DayOfWeek): LocalDate {
        var d = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
        while (d.dayOfWeek != dow) d = d.minusDays(1)
        return d
    }

    /** A random instant; half of them within a day of an Egyptian DST change. */
    private fun instant(zone: ZoneId): Long {
        val year = rnd.nextInt(2024, 2031)
        val base = if (rnd.nextBoolean()) {
            val date = if (rnd.nextBoolean()) lastWeekday(year, 4, java.time.DayOfWeek.FRIDAY) else lastWeekday(year, 10, java.time.DayOfWeek.THURSDAY)
            date.atStartOfDay(zone).toInstant().toEpochMilli() + rnd.nextLong(-86_400_000L, 2 * 86_400_000L)
        } else {
            LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli() + rnd.nextLong(0, 365L * 86_400_000L)
        }
        return base
    }

    private fun interval() = TurnGrid.INTERVALS[rnd.nextInt(TurnGrid.INTERVALS.size)]
    private fun zone() = zones[rnd.nextInt(zones.size)]

    @Test fun transfusionPlanFollowsTheCard() = repeat(cases) {
        val duration = rnd.nextInt(10, 601)
        val limit = rnd.nextInt(60, 481)
        val config = ScheduleConfig(duration, limit, rnd.nextInt(5, minOf(61, limit)))
        val until = rnd.nextInt(0, 900)
        val plan = ScheduleCalculator.plan(config, until)
        assertEquals(CheckpointType.START, plan.first().type)
        assertEquals(0, plan.first().offsetMin)
        val offsets = plan.map { it.offsetMin }
        assertEquals("sorted: $plan", offsets.sorted(), offsets)
        // The time-limit warning may share a minute with a check; the card's own checks never do.
        val own = plan.filter { it.type != CheckpointType.LIMIT_WARNING }.map { it.offsetMin }
        assertEquals("no two checks share a minute: $plan", own.distinct().size, own.size)
        plan.filter { it.type == CheckpointType.HOURLY }.forEach { assertEquals(0, it.offsetMin % 60) }
        if (until >= 15 && duration != 15) assertTrue("15-min check: $plan", plan.any { it.type == CheckpointType.M15 && it.offsetMin == 15 })
        if (until >= 30 && duration != 30) assertTrue("30-min check: $plan", plan.any { it.type == CheckpointType.M30 && it.offsetMin == 30 })
        if (until >= duration) assertTrue("End check: $plan", plan.any { it.type == CheckpointType.PLANNED_END && it.offsetMin == duration })
    }

    @Test fun t0IsAWholeMinute() = repeat(cases) {
        val t = rnd.nextLong(1_600_000_000_000, 1_900_000_000_000)
        val m = ScheduleCalculator.truncateToMinute(t)
        assertEquals(0, m % MINUTE_MS)
        assertTrue(m <= t && t - m < MINUTE_MS)
        assertEquals(m, ScheduleCalculator.truncateToMinute(m))
    }

    @Test fun slotsSitOnTheWardClockAndAreAlwaysAhead() = repeat(cases) {
        val zone = zone()
        val after = instant(zone)
        val interval = interval()
        val slot = TurnGrid.nextSlotAfter(after, interval, zone)
        val local = Instant.ofEpochMilli(slot).atZone(zone).toLocalDateTime()
        assertTrue(slot > after)
        assertEquals(0, (local.hour * 60 + local.minute) % interval)
        assertEquals(0, local.second)
        // One interval away, unless a DST change sits in between: a slot that does not exist that
        // night is skipped (SPEC), so the wait is then at most two intervals.
        val sameOffset = zone.rules.getOffset(Instant.ofEpochMilli(after)) == zone.rules.getOffset(Instant.ofEpochMilli(slot))
        val limit = if (sameOffset) interval else 2 * interval
        assertTrue("$after $slot $zone", slot - after <= limit * MINUTE_MS)
    }

    @Test fun aLateTurnNeverMovesTheGrid() = repeat(cases) {
        val zone = zone()
        val interval = interval()
        val slot = TurnGrid.nextSlotAfter(instant(zone), interval, zone)
        val next = TurnGrid.nextSlotAfter(slot, interval, zone)
        val ack = slot + rnd.nextLong(0, next - slot)
        assertEquals(next, TurnGrid.nextSlotAfter(ack, interval, zone))
        assertEquals(next, TurnGrid.missAt(slot, interval, zone))
    }

    @Test fun firstSlotKeepsThe30MinuteRule() = repeat(cases) {
        val zone = zone()
        val start = instant(zone)
        val interval = interval()
        val first = TurnGrid.firstSlot(start, interval, zone)
        val choices = TurnGrid.firstSlotChoices(start, interval, zone)
        assertTrue(first in choices)
        assertTrue(first - start >= TurnGrid.MIN_LEAD_MIN * MINUTE_MS)
        if (choices[0] - start >= TurnGrid.MIN_LEAD_MIN * MINUTE_MS) assertEquals(choices[0], first)
    }

    @Test fun endOfShiftIsTheNextShiftChange() = repeat(cases) {
        val zone = zone()
        val now = instant(zone)
        val end = TurnGrid.endOfShift(now, zone)
        val local = Instant.ofEpochMilli(end).atZone(zone).toLocalTime()
        assertTrue(end > now)
        assertTrue("$local", local.hour == 8 || local.hour == 20)
        assertEquals(0, local.minute)
        assertTrue(end - now <= 13 * 60 * MINUTE_MS)
    }

    @Test fun noAlarmAtOrAfterTheStopTime() = repeat(cases) {
        val zone = zone()
        val interval = interval()
        val first = TurnGrid.firstSlot(instant(zone), interval, zone)
        val stop = first + rnd.nextLong(0, 48 * 60 * MINUTE_MS)
        val slots = TurnGrid.slots(first, interval, stop, stop + 24 * 60 * MINUTE_MS, zone)
        assertTrue(slots.all { it < stop })
        assertEquals(slots.sorted().distinct(), slots)
        slots.zipWithNext().forEach { (a, b) -> assertEquals(TurnGrid.nextSlotAfter(a, interval, zone), b) }
    }

    @Test fun anExcludedPositionIsNeverSuggested() = repeat(cases) {
        val rotation = Position.entries.shuffled(rnd).take(rnd.nextInt(1, Position.entries.size + 1))
        val excluded = Position.entries - rotation.toSet()
        var current = Position.entries[rnd.nextInt(Position.entries.size)]
        repeat(rotation.size + 1) {
            current = TurnGrid.nextPosition(current, rotation)
            assertTrue(current in rotation)
            assertFalse(current in excluded)
        }
        // The rotation visits every allowed position.
        val seen = mutableSetOf<Position>()
        repeat(rotation.size) {
            current = TurnGrid.nextPosition(current, rotation)
            seen += current
        }
        assertEquals(rotation.toSet(), seen)
    }

    @Test fun longestGapIsBounded() = repeat(cases) {
        val start = rnd.nextLong(1_700_000_000_000, 1_800_000_000_000)
        val now = start + rnd.nextLong(0, 24 * 60 * MINUTE_MS)
        val turns = List(rnd.nextInt(0, 12)) { rnd.nextLong(start, now + 1) }.sorted()
        val gap = TurnGrid.longestGapMs(turns, start, now)
        assertTrue(gap in 0..(now - start))
        val all = (listOf(start) + turns + now).zipWithNext { a, b -> b - a }
        assertEquals(all.maxOrNull() ?: 0L, gap)
    }
}
