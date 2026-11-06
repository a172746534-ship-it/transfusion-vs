package app.transfusionvs.schedule

import app.transfusionvs.data.CustomSlot
import app.transfusionvs.data.CustomSlotStatus
import app.transfusionvs.data.CustomTimer
import app.transfusionvs.data.Settings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property-based checks of the custom timer's series, ≥ 10,000 random cases per rule from a fixed
 * seed so a failure always reproduces. Start times are biased towards Egypt's DST nights.
 * Intervals are the buttons and "Other" (15 min – 24 h in 5-min steps).
 */
class CustomSchedulePropertyTest {

    private val cases = 12_000
    private val rnd = Random(20261106)
    private val zones = listOf("Africa/Cairo", "Asia/Kathmandu", "America/New_York", "Asia/Riyadh", "UTC").map(ZoneId::of)

    private fun lastWeekday(year: Int, month: Int, dow: DayOfWeek): LocalDate {
        var d = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
        while (d.dayOfWeek != dow) d = d.minusDays(1)
        return d
    }

    /** A start time on the whole minute; half of them within a day of an Egyptian DST change. */
    private fun start(): Long {
        val zone = zones[rnd.nextInt(zones.size)]
        val year = rnd.nextInt(2024, 2031)
        val t = if (rnd.nextBoolean()) {
            val date = if (rnd.nextBoolean()) lastWeekday(year, 4, DayOfWeek.FRIDAY) else lastWeekday(year, 10, DayOfWeek.THURSDAY)
            date.atStartOfDay(zone).toInstant().toEpochMilli() + rnd.nextLong(-86_400_000L, 86_400_000L)
        } else {
            LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli() + rnd.nextLong(0, 365L * 86_400_000L)
        }
        return ScheduleCalculator.truncateToMinute(t)
    }

    private fun interval(): Int =
        if (rnd.nextInt(3) == 0) CustomRules.INTERVALS[rnd.nextInt(CustomRules.INTERVALS.size)]
        else rnd.nextInt(CustomRules.MIN_INTERVAL / CustomRules.OTHER_STEP, CustomRules.MAX_INTERVAL / CustomRules.OTHER_STEP + 1) * CustomRules.OTHER_STEP

    private fun timer(start: Long, interval: Int) =
        CustomTimer(id = 1, bedLabel = "7654812", name = "Check drain", intervalMin = interval, startAt = start, anchorAt = start, fromAt = start)

    private fun onSeries(t: CustomTimer, at: Long) = Math.floorMod(at - t.anchorAt, t.intervalMin * MINUTE_MS) == 0L && at > t.fromAt

    @Test fun theFirstAlarmIsOneIntervalAfterTheStart() = repeat(cases) {
        val start = start()
        val t = timer(start, interval())
        assertEquals(start + t.intervalMin * MINUTE_MS, CustomRules.next(t, start))
        assertEquals(listOf(start + t.intervalMin * MINUTE_MS), CustomRules.materialize(t, emptyList(), start).map { it.slotAt })
    }

    /** Elapsed time: across DST nights and time zones, alarms are exactly one interval apart. None skipped, none doubled. */
    @Test fun dstNeverShiftsSkipsOrDoublesAnAlarm() = repeat(cases) {
        val t = timer(start(), interval())
        var prev = t.startAt
        repeat(rnd.nextInt(2, 12)) {
            val n = CustomRules.next(t, prev)
            assertEquals("${t.intervalMin} min after $prev", prev + t.intervalMin * MINUTE_MS, n)
            prev = n
        }
        // Rebuilt after a long gap (phone off over the DST night): the same points, never one extra.
        val now = prev + rnd.nextLong(0, 3 * t.intervalMin * MINUTE_MS)
        val rebuilt = CustomRules.materialize(t, emptyList(), now).map { it.slotAt }
        assertEquals(rebuilt.distinct(), rebuilt)
        assertTrue(rebuilt.all { onSeries(t, it) })
        rebuilt.zipWithNext().forEach { (a, b) -> assertEquals(t.intervalMin * MINUTE_MS, b - a) }
    }

    /**
     * A shift with random gaps (phone off up to a day and more), random late Done taps and snoozes.
     * After every refresh: every alarm is on the series, none doubled, exactly one next alarm armed,
     * anything whose next alarm fired is closed, and a Done never changes again or moves anything.
     */
    @Test fun aShiftWithGapsLateDonesAndSnoozes() = repeat(cases) {
        val t = timer(start(), interval())
        val timeout = rnd.nextInt(1, 61)
        val snoozeMin = rnd.nextInt(1, 11)
        val stepMs = t.intervalMin * MINUTE_MS
        var nextId = 1L
        var slots = listOf<CustomSlot>()
        val done = mutableMapOf<Long, CustomSlot>()
        var now = t.startAt
        repeat(12) {
            now += if (rnd.nextInt(7) == 0) rnd.nextLong(0, 30 * 3_600_000L) else rnd.nextLong(0, stepMs + stepMs / 2)
            // The engine's refresh: create, then step.
            val created = CustomRules.materialize(t, slots, now).map { it.copy(id = nextId++) }
            slots = slots + created
            val changed = CustomRules.step(t, slots, now, timeout).associateBy { it.id }
            slots = slots.map { changed[it.id] ?: it }

            assertTrue("on the series: $slots", slots.all { onSeries(t, it.slotAt) })
            assertEquals("no doubled alarm", slots.size, slots.map { it.slotAt }.distinct().size)
            val future = slots.filter { it.status == CustomSlotStatus.PENDING && it.slotAt > now }
            assertEquals("exactly one next alarm at $now: $slots", 1, future.size)
            // An alarm due within EARLY_MS already rings; the next one is the one after it.
            assertEquals(CustomRules.next(t, now + CustomRules.EARLY_MS), future.single().slotAt)
            val plan = CustomRules.arm(t, slots, now, timeout, preAlertMin = 0)
            assertEquals(listOf(future.single().slotAt), plan.requests.filter { it.kind == AlarmKind.CHECKPOINT }.map { it.at })
            slots.filter { CustomRules.next(t, it.slotAt) <= now }.forEach {
                assertTrue("closed once the next alarm fired: $it at $now", it.status == CustomSlotStatus.ACKED || it.status == CustomSlotStatus.MISSED)
            }
            done.values.forEach { d -> assertEquals(d, slots.single { it.id == d.id }) }

            // The nurse: a (late) Done on what is open, or a snooze where it is offered.
            val open = slots.filter { it.status == CustomSlotStatus.RINGING }
            when (rnd.nextInt(3)) {
                0 -> slots = slots.map { s ->
                    if (s in open) s.copy(status = CustomSlotStatus.ACKED, ackAt = now, snoozeUntil = null).also { done[it.id] = it } else s
                }
                1 -> slots = slots.map { s ->
                    if (s in open && CustomRules.isAudible(s, now) && CustomRules.canSnooze(t, s, now, snoozeMin)) {
                        s.copy(snoozeCount = s.snoozeCount + 1, snoozeUntil = now + snoozeMin * MINUTE_MS)
                    } else s
                }
                else -> Unit
            }
        }
    }

    /** Open alarms close as MISSED exactly when the next alarm fires, also when the interval is shorter than the timeout. */
    @Test fun missedExactlyWhenTheNextAlarmFires() = repeat(cases) {
        val t = timer(start(), interval())
        val timeout = rnd.nextInt(1, 2 * t.intervalMin + 1)
        val slotAt = CustomRules.next(t, t.startAt + rnd.nextLong(0, 5 * t.intervalMin * MINUTE_MS))
        val s = CustomSlot(id = 1, timerId = 1, slotAt = slotAt, firedAt = slotAt, status = CustomSlotStatus.RINGING, silenced = rnd.nextBoolean())
        val now = slotAt + rnd.nextLong(0, 3 * t.intervalMin * MINUTE_MS)
        val after = CustomRules.step(t, listOf(s), now, timeout).singleOrNull() ?: s
        val nextFired = slotAt + t.intervalMin * MINUTE_MS <= now
        assertEquals("interval ${t.intervalMin}, timeout $timeout, ${now - slotAt} ms late", nextFired, after.status == CustomSlotStatus.MISSED)
        if (!nextFired) assertEquals(CustomSlotStatus.RINGING, after.status)
    }

    /** A snooze is offered exactly when it ends before the next alarm (and the limit isn't reached). */
    @Test fun snoozeNeverPassesTheNextAlarm() = repeat(cases) {
        val t = timer(start(), interval())
        val slotAt = CustomRules.next(t, t.startAt + rnd.nextLong(0, 5 * t.intervalMin * MINUTE_MS))
        val count = rnd.nextInt(0, Settings.MAX_SNOOZES + 1)
        val s = CustomSlot(id = 1, timerId = 1, slotAt = slotAt, firedAt = slotAt, status = CustomSlotStatus.RINGING, snoozeCount = count)
        val now = slotAt + rnd.nextLong(0, t.intervalMin * MINUTE_MS)
        val snoozeMin = rnd.nextInt(1, 31)
        val ok = CustomRules.canSnooze(t, s, now, snoozeMin)
        val nextAlarm = slotAt + t.intervalMin * MINUTE_MS
        assertEquals(count < Settings.MAX_SNOOZES && now + snoozeMin * MINUTE_MS < nextAlarm, ok)
    }

    /** Every interval up to 24 h, at any moment: a next alarm is armed, within one interval. */
    @Test fun aNextAlarmIsAlwaysArmed_forEveryInterval() {
        val intervals = (CustomRules.MIN_INTERVAL..CustomRules.MAX_INTERVAL step CustomRules.OTHER_STEP).toList()
        var n = 0
        for (interval in intervals) repeat(42) {
            val t = timer(start(), interval)
            val now = t.startAt + rnd.nextLong(0, 3L * interval * MINUTE_MS)
            val slots = CustomRules.materialize(t, emptyList(), now).mapIndexed { i, s -> s.copy(id = i + 1L) }
            val stepped = CustomRules.step(t, slots, now, 10).associateBy { it.id }
            val after = slots.map { stepped[it.id] ?: it }
            val armed = CustomRules.arm(t, after, now, 10, 0).requests.filter { it.kind == AlarmKind.CHECKPOINT }.map { it.at }
            assertEquals("interval $interval", 1, armed.size)
            assertTrue(armed.single() > now + CustomRules.EARLY_MS && armed.single() <= now + CustomRules.EARLY_MS + interval * MINUTE_MS)
            n++
        }
        assertTrue(n >= 10_000)
    }

    /**
     * Editing the interval: the new series never rings in the past and has its next alarm within one
     * new interval. A rename or another MRN with the same interval changes nothing about the schedule.
     */
    @Test fun editingTheIntervalNeverRingsInThePast_andARenameNeverReschedules() = repeat(cases) {
        val t = timer(start(), interval())
        val now = t.startAt + rnd.nextLong(0, 6 * t.intervalMin * MINUTE_MS)
        var id = 1L
        val history = CustomRules.materialize(t, emptyList(), now).map { s ->
            val closed = s.slotAt <= now
            when {
                !closed -> s
                rnd.nextBoolean() -> s.copy(status = CustomSlotStatus.ACKED, firedAt = s.slotAt, ackAt = s.slotAt + rnd.nextLong(0, t.intervalMin * MINUTE_MS))
                else -> s.copy(status = CustomSlotStatus.MISSED)
            }.copy(id = id++)
        }.filter { it.status != CustomSlotStatus.ACKED || it.ackAt!! <= now }

        val renamed = t.copy(name = "Other task", bedLabel = "7654899")
        assertSame(renamed, CustomRules.edited(renamed, history, t.intervalMin, now))

        val newInterval = generateSequence { interval() }.first { it != t.intervalMin }
        val e = CustomRules.edited(t, history, newInterval, now)
        // What the engine keeps: pending alarms of the old series are cancelled.
        val kept = history.filter { it.status != CustomSlotStatus.PENDING }
        val made = CustomRules.materialize(e, kept, now)
        assertTrue("never in the past: $made", made.isNotEmpty() && made.all { it.slotAt > now })
        assertEquals("one new next alarm", 1, made.count { it.slotAt > now + CustomRules.EARLY_MS })
        assertTrue(made.first().slotAt <= now + newInterval * MINUTE_MS)
        assertTrue(made.all { onSeries(e, it.slotAt) })
        val lastDone = history.filter { it.status == CustomSlotStatus.ACKED }.mapNotNull { it.ackAt }.maxOrNull()
        assertEquals(ScheduleCalculator.truncateToMinute(lastDone ?: t.startAt), e.anchorAt)
        assertFalse(e.fromAt < now)
    }
}
